package cc.uncarbon.test.tenant;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.tenant.errorcode.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cc.uncarbon.module.tenant.model.request.AdminTenantCreateRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantValidateResult;
import cc.uncarbon.module.tenant.service.TenantService;
import cc.uncarbon.test.base.BaseIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import cc.uncarbon.module.tenant.dal.mapper.TenantMetaMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserRoleRelationMapper;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMenuRelationMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserTenantRelationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserTenantRelationEntity;
import cc.uncarbon.module.tenant.dal.entity.TenantMetaEntity;

/**
 * 租户门面集成测试：编码校验、切换目标解析、可切换列表、切换断言
 */
@Tag("tenant")
class TenantFacadeIT extends BaseIntegrationTest {

    @Resource
    private TenantFacade tenantFacade;
    @Resource
    private TenantService tenantService;

    @Resource
    private TenantMetaMapper tenantMetaMapper;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysRoleMapper sysRoleMapper;
    @Resource
    private SysUserRoleRelationMapper userRoleRelationMapper;
    @Resource
    private SysRoleMenuRelationMapper roleMenuRelationMapper;
    @Resource
    private SysUserTenantRelationMapper userTenantRelationMapper;


    @Test
    void facadeBranches() {
        Assumptions.assumeTrue(tenantFacade.isTenantEnabled(), "多租户未启用时跳过");

        long nano = System.nanoTime() % 1_000_000_000L;
        String code = "itf" + nano;
        Long tenantId = tenantService.adminCreate(new AdminTenantCreateRequest()
                .setCode(code).setName("IT门面租户")
                .setTenantAdminPin("itfa" + nano).setTenantAdminPwd("it-pass-123456")
                .setTenantAdminEmail("it@a.b.c").setTenantAdminPhoneNo("13800001111"));

        try {
            // 空编码明确报错，不静默回退
            TenantValidateResult blank = tenantFacade.validateByCode("  ");
            Assertions.assertFalse(blank.isValid());
            Assertions.assertEquals(TenantErrorCodeEnum.A03011, blank.getErrorCode());

            // 不存在
            TenantValidateResult missing = tenantFacade.validateByCode("it-no-such" + nano);
            Assertions.assertFalse(missing.isValid());
            Assertions.assertEquals(TenantErrorCodeEnum.A03001, missing.getErrorCode());

            // 命中
            TenantValidateResult hit = tenantFacade.validateByCode(code);
            Assertions.assertTrue(hit.isValid());
            Assertions.assertEquals(tenantId, hit.getTenantId());

            // 解析启用租户
            Assertions.assertNotNull(tenantFacade.resolveEnabledTenant(tenantId));
            Assertions.assertNull(tenantFacade.resolveEnabledTenant(null));
            Assertions.assertNull(tenantFacade.resolveEnabledTenant(99_999_999L));

            // 切换目标
            Assertions.assertEquals(tenantId, tenantFacade.resolveSwitchTarget(code).getTenantId());

            // 超管可切任意租户；超管可切换列表包含新建租户
            Assertions.assertDoesNotThrow(() -> tenantFacade.assertSwitchable(0L, tenantId));
            Assertions.assertTrue(tenantFacade.listSelectableTenants(0L).stream()
                    .anyMatch(ctx -> tenantId.equals(ctx.getTenantId())));
        } finally {
            cleanupTenant(tenantId);
        }
    }

    @Test
    void validatePassesWhenTenantDisabled() {
        Assumptions.assumeFalse(tenantFacade.isTenantEnabled(), "多租户启用时跳过本用例");

        Assertions.assertTrue(tenantFacade.validateByCode(null).isValid());
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private void cleanupTenant(Long tenantId) {
        TenantContextHolder.runIgnored(() -> {
            var roleIds = sysRoleMapper.selectList(
                            new LambdaQueryWrapper<SysRoleEntity>()
                                    .eq(SysRoleEntity::getTenantId, tenantId))
                    .stream().map(SysRoleEntity::getId).toList();
            if (!roleIds.isEmpty()) {
                roleMenuRelationMapper.deleteByRoleIds(roleIds);
                userRoleRelationMapper.deleteByRoleIds(roleIds);
                sysRoleMapper.deleteByIds(roleIds);
            }
            var userIds = userTenantRelationMapper.selectList(
                            new LambdaQueryWrapper<SysUserTenantRelationEntity>()
                                    .eq(SysUserTenantRelationEntity::getTenantId, tenantId))
                    .stream().map(SysUserTenantRelationEntity::getUserId).toList();
            if (!userIds.isEmpty()) {
                userTenantRelationMapper.delete(
                        new LambdaQueryWrapper<SysUserTenantRelationEntity>()
                                .eq(SysUserTenantRelationEntity::getTenantId, tenantId));
                sysUserMapper.deleteByIds(userIds);
            }
            tenantMetaMapper.deleteById(tenantId);
        });
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.runIgnored(() ->
                tenantMetaMapper.selectList(new LambdaQueryWrapper<TenantMetaEntity>()
                                .likeRight(TenantMetaEntity::getCode, "itf"))
                        .forEach(t -> {
                            cleanupTenant(t.getId());
                        }));
    }
}
