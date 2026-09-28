package cc.uncarbon.test.tenant;

import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.test.base.BaseIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 行级多租户隔离测试（M2 验收：同 pin 双租户互不串数据）
 *
 * <p>前置：已执行 attachments/db/upgrade/4.0.0-tenant-login-mode.sql，
 * 且 helium.tenant.strategy=LINE</p>
 */
@Tag("tenant")
class TenantLineIsolationIT extends BaseIntegrationTest {

    private static final Long TENANT_A = 960001L;
    private static final Long TENANT_B = 960002L;

    @Resource
    private SysUserMapper sysUserMapper;


    @Test
    void samePinInTwoTenantsIsolated() throws Exception {
        String pin = "it" + (System.nanoTime() % 1_000_000_000L);

        Long idA = insertUserInTenant(TENANT_A, pin, "A-租户视角");
        Long idB = insertUserInTenant(TENANT_B, pin, "B-租户视角");

        // 各租户视角内按 pin 查询，只能看到本租户的同名账号
        SysUserEntity inA = TenantContextHolder.callWithContext(
                new SimpleTenantContext(TENANT_A, null, null),
                () -> sysUserMapper.getByPin(pin));
        SysUserEntity inB = TenantContextHolder.callWithContext(
                new SimpleTenantContext(TENANT_B, null, null),
                () -> sysUserMapper.getByPin(pin));

        Assertions.assertNotNull(inA);
        Assertions.assertNotNull(inB);
        Assertions.assertEquals(idA, inA.getId());
        Assertions.assertEquals(idB, inB.getId());
        Assertions.assertEquals("A-租户视角", inA.getNickname());
        Assertions.assertEquals("B-租户视角", inB.getNickname());
        Assertions.assertEquals(TENANT_A, inA.getTenantId());
        Assertions.assertEquals(TENANT_B, inB.getTenantId());

        // 忽略租户态下可见两行（证明行级过滤确实生效，而非只有一行）
        Long total = TenantContextHolder.callIgnored(() ->
                sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                        .eq(SysUserEntity::getPin, pin)));
        Assertions.assertEquals(2L, total.longValue());
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private Long insertUserInTenant(Long tenantId, String pin, String nickname) throws Exception {
        return TenantContextHolder.callWithContext(
                new SimpleTenantContext(tenantId, null, null),
                () -> {
                    var entity = new SysUserEntity()
                            .setPin(pin)
                            .setPwd("it-placeholder")
                            .setNickname(nickname)
                            .setStatus(SysUserStatusEnum.ENABLED);
                    sysUserMapper.insert(entity);
                    return entity.getId();
                });
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.runIgnored(() -> {
            sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                            .likeRight(SysUserEntity::getPin, "it"))
                    .forEach(user -> sysUserMapper.deleteById(user.getId()));
        });
    }
}
