package cc.uncarbon.test.sys;

import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.db.fieldcrypto.field.FieldCipher;
import cc.uncarbon.framework.helium.db.fieldcrypto.field.FieldCipherContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.model.internal.RootKeyRotationOptions;
import cc.uncarbon.module.sys.model.internal.RotationOptions;
import cc.uncarbon.module.sys.model.internal.RotationResult;
import cc.uncarbon.module.sys.model.query.AdminSysUserListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysUserCreateRequest;
import cc.uncarbon.module.sys.service.SysUserFieldCryptoKeyRotationService;
import cc.uncarbon.module.sys.service.SysUserService;
import cc.uncarbon.test.base.BaseIntegrationTest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * sys_user 敏感字段加密密钥轮换集成测试：常规 DEK 轮换、数据根泄露应急轮换、参数闸门
 *
 * <p>轮换需按运维手册分代注入密钥配置后重启节点；本 IT 用 {@link FieldCipherContext}
 * 在测试内分代注册 {@link FieldCipher}，模拟"换配置重启"的节点视角。
 * 用 {@code itk*} 专用 keyId 建行，前缀扫描只命中本测试数据，不波及测试库其它密文。
 *
 * <p>索引根密钥轮换（rotateIndexRootKey）需更换 BlindIndexCalculator Bean（须重建上下文），不在本 IT 覆盖范围
 */
@Tag("sys")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SysUserFieldCryptoKeyRotationIT extends BaseIntegrationTest {

    private static final String PIN_PREFIX = "itrot";

    /*
     * IT 专用 keyId（[A-Za-z0-9-]{1,16}），与业务侧 k1/k2 区分，前缀扫描天然隔离
     */
    private static final String OLD_KEY_1 = "itk1";
    private static final String OLD_KEY_2 = "itk2";
    private static final String NEW_KEY = "itk3";

    /*
     * 两代数据根密钥（32 字节，固定测试值；仅作用于 itk* keyId 的 HKDF 派生）
     */
    private static final byte[] OLD_ROOT = "A".repeat(32).getBytes(StandardCharsets.UTF_8);
    private static final byte[] NEW_ROOT = "B".repeat(32).getBytes(StandardCharsets.UTF_8);

    private static final RotationOptions FAST_OPTIONS = new RotationOptions(100, 0);

    @Resource
    private SysUserFieldCryptoKeyRotationService rotationService;
    @Resource
    private SysUserService sysUserService;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * profile 配置的 FieldCipher，测试中按代际换装，测完恢复
     */
    private FieldCipher profileCipher;


    @Test
    void rotationLifecycle() {
        // 初代节点：OLD_ROOT + itk1
        FieldCipherContext.register(new FieldCipher(OLD_ROOT, List.of(OLD_KEY_1), OLD_KEY_1));

        long stamp = System.nanoTime() % 1_000_000_000L;
        String phone1 = "139" + String.format("%08d", stamp);
        String phone2 = "138" + String.format("%08d", stamp);
        String email1 = "itrot." + stamp + "a@it.a.b.c";
        String email2 = "itrot." + stamp + "b@it.a.b.c";
        Long id1 = createUser(PIN_PREFIX + "a" + stamp, phone1, email1);
        Long id2 = createUser(PIN_PREFIX + "b" + stamp, phone2, email2);

        // 初代密文带 itk1 前缀，残留计数 > 0，不可下线旧 keyId
        Assertions.assertTrue(rotationService.countOldKeyRemaining(OLD_KEY_1).rowsTotal() >= 2);
        Assertions.assertFalse(rotationService.isSafeToDropOldKey(OLD_KEY_1));
        Assertions.assertTrue(rawPhoneNo(id1).startsWith(OLD_KEY_1 + "$"));

        // 常规 DEK 轮换 itk1 -> itk2：换装"新旧并存、active=itk2"节点
        FieldCipherContext.register(new FieldCipher(OLD_ROOT, List.of(OLD_KEY_1, OLD_KEY_2), OLD_KEY_2));
        RotationResult dekResult = rotationService.rotateDataKey(OLD_KEY_1, FAST_OPTIONS);
        Assertions.assertTrue(dekResult.completed(), () -> "DEK 轮换未完成: " + dekResult);
        Assertions.assertEquals(2, dekResult.processedRows());
        Assertions.assertTrue(rotationService.isSafeToDropOldKey(OLD_KEY_1));
        Assertions.assertTrue(rawPhoneNo(id1).startsWith(OLD_KEY_2 + "$"));

        // 数据根泄露应急：换装"新根 + previous-root 兜底解密 itk1/itk2"节点，oldKeyIds 与人工演练一致
        FieldCipherContext.register(new FieldCipher(NEW_ROOT, List.of(NEW_KEY), NEW_KEY,
                OLD_ROOT, List.of(OLD_KEY_1, OLD_KEY_2)));
        RotationResult rootResult = rotationService.rotateDataRootKey(
                new RootKeyRotationOptions(List.of(OLD_KEY_1, OLD_KEY_2), FAST_OPTIONS));
        Assertions.assertTrue(rootResult.completed(), () -> "数据根轮换未完成: " + rootResult);
        Assertions.assertEquals(2, rootResult.processedRows());
        Assertions.assertTrue(rotationService.isSafeToDropOldKey(OLD_KEY_1));
        Assertions.assertTrue(rotationService.isSafeToDropOldKey(OLD_KEY_2));
        Assertions.assertTrue(rawPhoneNo(id2).startsWith(NEW_KEY + "$"));

        // 收尾态：全节点仅配置新根（无 previous-*），明文回读不丢，盲索引仍可检索
        FieldCipherContext.register(new FieldCipher(NEW_ROOT, List.of(NEW_KEY), NEW_KEY));
        assertPlaintextRoundTrip(id1, phone1, email1);
        assertPlaintextRoundTrip(id2, phone2, email2);
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () -> {
            var page = sysUserService.adminList(new AdminSysUserListQuery()
                    .setPageParam(new PageParam(1, 10)).setPhoneNoTail4(phone1.substring(7)));
            Assertions.assertTrue(page.getRecords().stream().anyMatch(u -> id1.equals(u.getId())));
        });
    }

    @Test
    void argumentGuards() {
        FieldCipherContext.register(new FieldCipher(OLD_ROOT, List.of(OLD_KEY_1), OLD_KEY_1));

        // 旧 keyId = 当前活跃 keyId
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> rotationService.rotateDataKey(OLD_KEY_1, null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> rotationService.rotateDataRootKey(new RootKeyRotationOptions(List.of(OLD_KEY_1), FAST_OPTIONS)));
        // 旧 keyId 含 LIKE 通配符
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> rotationService.rotateDataKey("it_1%", null));
        // oldKeyIds 为空
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> rotationService.rotateDataRootKey(new RootKeyRotationOptions(List.of(), FAST_OPTIONS)));
        // 本节点无法解密旧代密文（未配置 previous-*）
        Assertions.assertThrows(IllegalStateException.class,
                () -> rotationService.rotateDataRootKey(new RootKeyRotationOptions(List.of("itk9"), FAST_OPTIONS)));
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    private Long createUser(String pin, String phoneNo, String email) {
        var ref = new Object() {
            Long id;
        };
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () -> {
            var request = new AdminSysUserCreateRequest();
            request.setPin(pin);
            request.setNickname("轮换IT用户");
            request.setEmail(email);
            request.setPhoneNo(phoneNo);
            request.setInitPwd("it-pass-123");
            request.setDeptId(null);
            ref.id = sysUserService.adminCreate(request);
        });
        return ref.id;
    }

    /**
     * @return 绕过 TypeHandler 的密文原值，校验 keyId 前缀代际
     */
    private String rawPhoneNo(Long id) {
        return jdbcTemplate.queryForObject("SELECT phone_no FROM sys_user WHERE id = ?", String.class, id);
    }

    private void assertPlaintextRoundTrip(Long id, String expectedPhoneNo, String expectedEmail) {
        var ref = new Object() {
            SysUserEntity row;
        };
        TenantContextHolder.runIgnored(() -> ref.row = sysUserMapper.selectById(id));
        Assertions.assertNotNull(ref.row);
        Assertions.assertEquals(expectedPhoneNo, ref.row.getPhoneNo());
        Assertions.assertEquals(expectedEmail, ref.row.getEmail());
    }

    @BeforeAll
    void backupProfileCipher() {
        profileCipher = FieldCipherContext.getRequired();
        // 清理历史中断运行可能残留的同前缀测试行，保证 processedRows 断言确定
        TenantContextHolder.runIgnored(() -> sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                        .likeRight(SysUserEntity::getPin, PIN_PREFIX))
                .forEach(u -> sysUserMapper.deleteById(u.getId())));
    }

    @AfterEach
    void restoreProfileCipher() {
        FieldCipherContext.register(profileCipher);
    }

    @AfterAll
    void cleanup() {
        FieldCipherContext.register(profileCipher);
        TenantContextHolder.runIgnored(() -> sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                        .likeRight(SysUserEntity::getPin, PIN_PREFIX))
                .forEach(u -> sysUserMapper.deleteById(u.getId())));
    }
}
