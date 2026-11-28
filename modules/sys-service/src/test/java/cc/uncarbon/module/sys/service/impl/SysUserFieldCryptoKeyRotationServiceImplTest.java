package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.db.fieldcrypto.blindindex.BlindIndexCalculator;
import cc.uncarbon.framework.helium.db.fieldcrypto.field.FieldCipher;
import cc.uncarbon.framework.helium.db.fieldcrypto.field.FieldCipherContext;
import cc.uncarbon.module.sys.MybatisPlusTestSupport;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.fieldcrypto.SysUserCipherCols;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.model.internal.OldKeyRemainingReport;
import cc.uncarbon.module.sys.model.internal.RootKeyRotationOptions;
import cc.uncarbon.module.sys.model.internal.RotationOptions;
import cc.uncarbon.module.sys.model.internal.RotationResult;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * {@link SysUserFieldCryptoKeyRotationServiceImpl} 密钥轮换分支测试
 *
 * <p>真实 FieldCipher（当前代 k2 + 上一代 k1 并存）与真实 BlindIndexCalculator（确定性 HMAC），
 * mapper 走 Mockito；租户忽略作用域走 ScopedValue 真实链路
 */
class SysUserFieldCryptoKeyRotationServiceImplTest {

    private static final String ACTIVE_KEY = "k2";
    private static final String OLD_KEY = "k1";
    private static final String PHONE = "13800001111";

    private SysUserMapper sysUserMapper;
    private BlindIndexCalculator blindIndexCalculator;
    private SysUserFieldCryptoKeyRotationServiceImpl service;


    @BeforeAll
    static void initTableInfo() {
        MybatisPlusTestSupport.initTableInfo();
        FieldCipherContext.register(new FieldCipher(
                "C".repeat(32).getBytes(StandardCharsets.UTF_8), List.of(ACTIVE_KEY), ACTIVE_KEY,
                "P".repeat(32).getBytes(StandardCharsets.UTF_8), List.of(OLD_KEY)));
    }

    @AfterAll
    static void tearDown() {
        FieldCipherContext.clear();
    }

    @BeforeEach
    void setUp() {
        sysUserMapper = Mockito.mock(SysUserMapper.class);
        blindIndexCalculator = new BlindIndexCalculator(new byte[32]);
        service = new SysUserFieldCryptoKeyRotationServiceImpl(sysUserMapper, blindIndexCalculator);
    }


    @Test
    void illegalOldKeyIdRejected() {
        // null、非法字符（_ / %）、超长（>16 位）均拒绝
        for (String bad : new String[]{null, "bad_id", "a%b", "0123456789abcdef7"}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> service.countOldKeyRemaining(bad),
                    "keyId: " + bad);
        }
    }

    @Test
    void countOldKeyRemainingAggregatesThreeCounts() {
        Mockito.when(sysUserMapper.selectCount(Mockito.any())).thenReturn(1L, 2L, 3L);

        OldKeyRemainingReport report = service.countOldKeyRemaining(OLD_KEY);

        Assertions.assertEquals(OLD_KEY, report.oldKeyId());
        Assertions.assertEquals(1L, report.phoneColumnRows());
        Assertions.assertEquals(2L, report.emailColumnRows());
        Assertions.assertEquals(3L, report.rowsTotal());
        Assertions.assertFalse(report.allClear());
    }

    @Test
    void safeToDropWhenNothingRemains() {
        Mockito.when(sysUserMapper.selectCount(Mockito.any())).thenReturn(0L);

        Assertions.assertTrue(service.isSafeToDropOldKey(OLD_KEY));
    }

    @Test
    void rotateDataKeyEqualToActiveRejected() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.rotateDataKey(ACTIVE_KEY, null));
    }

    @Test
    void rotateDataKeyHappyPathWithPause() {
        Mockito.when(sysUserMapper.selectList(Mockito.any())).thenReturn(
                List.of(new SysUserEntity().setId(1L)), List.of());
        Mockito.when(sysUserMapper.updateById(Mockito.any(SysUserEntity.class))).thenReturn(1);
        Mockito.when(sysUserMapper.selectCount(Mockito.any())).thenReturn(0L);

        RotationResult result = service.rotateDataKey(OLD_KEY, new RotationOptions(50, 1));

        Assertions.assertTrue(result.completed());
        Assertions.assertEquals(1, result.processedRows());
        Assertions.assertEquals(1, result.processedBatches());
        Assertions.assertNull(result.errorMessage());
        Mockito.verify(sysUserMapper).updateById(Mockito.any(SysUserEntity.class));
    }

    @Test
    void rotateDataKeyRemainingCiphertextFailsCompletion() {
        Mockito.when(sysUserMapper.selectList(Mockito.any())).thenReturn(List.of());
        Mockito.when(sysUserMapper.selectCount(Mockito.any())).thenReturn(7L);

        RotationResult result = service.rotateDataKey(OLD_KEY, new RotationOptions(50, 0));

        Assertions.assertFalse(result.completed());
        Assertions.assertEquals(0, result.processedBatches());
        Assertions.assertTrue(result.errorMessage().contains("残留"));
    }

    @Test
    void rotateDataKeySwallowsBatchFailure() {
        Mockito.when(sysUserMapper.selectList(Mockito.any()))
                .thenReturn(List.of(new SysUserEntity().setId(1L)));
        Mockito.when(sysUserMapper.updateById(Mockito.any(SysUserEntity.class)))
                .thenThrow(new RuntimeException("boom"));

        RotationResult result = service.rotateDataKey(OLD_KEY, new RotationOptions(50, 0));

        Assertions.assertFalse(result.completed());
        Assertions.assertEquals(0, result.processedRows());
        Assertions.assertTrue(result.errorMessage().contains("boom"));
    }

    @Test
    void rotateDataRootKeyValidatesArgs() {
        // oldKeyIds 空、含非法 keyId、等于当前活跃 keyId、本节点无兜底解密能力，逐类拒绝
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.rotateDataRootKey(new RootKeyRotationOptions(null, null)));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.rotateDataRootKey(new RootKeyRotationOptions(List.of(), null)));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.rotateDataRootKey(new RootKeyRotationOptions(List.of("bad_id"), null)));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.rotateDataRootKey(new RootKeyRotationOptions(List.of(ACTIVE_KEY), null)));
        Assertions.assertThrows(IllegalStateException.class,
                () -> service.rotateDataRootKey(new RootKeyRotationOptions(List.of("k9"), null)));
    }

    @Test
    void rotateDataRootKeyHappyPathWithDuplicateKeyIds() {
        Mockito.when(sysUserMapper.selectList(Mockito.any())).thenReturn(
                List.of(new SysUserEntity().setId(1L)), List.of());
        Mockito.when(sysUserMapper.selectById(1L)).thenReturn(new SysUserEntity().setId(1L));
        Mockito.when(sysUserMapper.updateById(Mockito.any(SysUserEntity.class))).thenReturn(1);
        Mockito.when(sysUserMapper.selectCount(Mockito.any())).thenReturn(0L);

        // 重复 oldKeyId 去重后仅扫描一个前缀；无 PII 行抽样校验通过
        RotationResult result = service.rotateDataRootKey(
                new RootKeyRotationOptions(List.of(OLD_KEY, OLD_KEY), new RotationOptions(50, 0)));

        Assertions.assertTrue(result.completed());
        Assertions.assertEquals(1, result.processedRows());
        Mockito.verify(sysUserMapper, Mockito.times(3)).selectCount(Mockito.any());
    }

    @Test
    void rotateIndexRootKeyDefaultsOptionsWhenNull() {
        Mockito.when(sysUserMapper.selectList(Mockito.any())).thenReturn(List.of());

        RotationResult result = service.rotateIndexRootKey(null);

        Assertions.assertTrue(result.completed());
        Assertions.assertEquals(0, result.processedRows());
    }

    @Test
    void rotateIndexRootKeyHappyPathWithConsistentBlindIndexes() {
        SysUserEntity row = consistentRow();
        Mockito.when(sysUserMapper.selectList(Mockito.any())).thenReturn(List.of(row), List.of());
        Mockito.when(sysUserMapper.selectById(1L)).thenReturn(row);
        Mockito.when(sysUserMapper.updateById(Mockito.any(SysUserEntity.class))).thenReturn(1);

        RotationResult result = service.rotateIndexRootKey(new RotationOptions(50, 0));

        Assertions.assertTrue(result.completed());
        Assertions.assertEquals(1, result.processedRows());
        Assertions.assertNull(result.errorMessage());
    }

    @Test
    void rotateIndexRootKeyPhoneIndexMismatchFails() {
        SysUserEntity row = new SysUserEntity().setId(1L)
                .setPhoneNo(PHONE)
                .setPhoneNoFullBlindIndex(blindIndexCalculator.compute(
                        SysUserCipherCols.PHONE_FULL_PURPOSE, PHONE))
                .setPhoneNoTail4BlindIndex("bad");
        Mockito.when(sysUserMapper.selectList(Mockito.any())).thenReturn(List.of(row), List.of());
        Mockito.when(sysUserMapper.selectById(1L)).thenReturn(row);

        RotationResult result = service.rotateIndexRootKey(new RotationOptions(50, 0));

        Assertions.assertFalse(result.completed());
        Assertions.assertTrue(result.errorMessage().contains(SysUserCipherCols.PHONE_TAIL4_PURPOSE));
    }

    @Test
    void rotateIndexRootKeyEmailIndexMismatchFails() {
        SysUserEntity row = new SysUserEntity().setId(1L)
                .setEmail("a@b.com")
                .setEmailFullBlindIndex("bad");
        Mockito.when(sysUserMapper.selectList(Mockito.any())).thenReturn(List.of(row), List.of());
        Mockito.when(sysUserMapper.selectById(1L)).thenReturn(row);

        RotationResult result = service.rotateIndexRootKey(new RotationOptions(50, 0));

        Assertions.assertFalse(result.completed());
        Assertions.assertTrue(result.errorMessage().contains(SysUserCipherCols.EMAIL_FULL_PURPOSE));
    }

    @Test
    void rotateIndexRootKeySampleRowVanishedFails() {
        SysUserEntity row = new SysUserEntity().setId(1L);
        Mockito.when(sysUserMapper.selectList(Mockito.any())).thenReturn(List.of(row), List.of());
        Mockito.when(sysUserMapper.selectById(1L)).thenReturn(null);

        RotationResult result = service.rotateIndexRootKey(new RotationOptions(50, 0));

        Assertions.assertFalse(result.completed());
        Assertions.assertTrue(result.errorMessage().contains("复查行不存在"));
    }

    @Test
    void rotationOptionsRejectsInvalidValues() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RotationOptions(0, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RotationOptions(1001, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RotationOptions(10, -1));
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    /**
     * 手机/邮箱盲索引与真实计算器一致的行（抽样校验通过）
     */
    private SysUserEntity consistentRow() {
        return new SysUserEntity().setId(1L)
                .setPhoneNo(PHONE)
                .setPhoneNoFullBlindIndex(blindIndexCalculator.compute(
                        SysUserCipherCols.PHONE_FULL_PURPOSE, PHONE))
                .setPhoneNoTail4BlindIndex(blindIndexCalculator.compute(
                        SysUserCipherCols.PHONE_TAIL4_PURPOSE, new SysUserEntity.Tail4Digits().extract(PHONE)))
                .setEmail("a@b.com")
                .setEmailFullBlindIndex(blindIndexCalculator.compute(
                        SysUserCipherCols.EMAIL_FULL_PURPOSE, "a@b.com"));
    }
}
