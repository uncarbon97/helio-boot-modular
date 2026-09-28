package cc.uncarbon.module.tenant.biz;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import cc.uncarbon.framework.helium.tenant.props.HeliumTenantProperties;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.tenant.constant.TenantConstant;
import cc.uncarbon.module.tenant.errorcode.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaDTO;
import cc.uncarbon.module.tenant.model.valueobj.TenantValidateResult;
import cc.uncarbon.module.tenant.service.TenantService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

/**
 * {@link TenantFacadeImpl} 租户门面分支测试（校验/切换断言/可切换列表）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantFacadeImplTest {

    @Mock
    private TenantService tenantService;
    @Mock
    private TenantUserRoleFacade tenantUserRoleFacade;
    @Mock
    private HeliumTenantProperties props;

    @InjectMocks
    private TenantFacadeImpl facade;


    /*
    ----------------------------------------------------------------
                        validateByCode
    ----------------------------------------------------------------
     */

    @Test
    void validateByCodePassesWhenTenantDisabled() {
        Mockito.when(props.doesTenantEnabled()).thenReturn(false);

        Assertions.assertTrue(facade.validateByCode(null).isValid());
        Mockito.verifyNoInteractions(tenantService);
    }

    @Test
    void validateByCodeBlankRejected() {
        Mockito.when(props.doesTenantEnabled()).thenReturn(true);

        var ret = facade.validateByCode("  ");
        Assertions.assertFalse(ret.isValid());
        Assertions.assertEquals(TenantErrorCodeEnum.A03011, ret.getErrorCode());
    }

    @Test
    void validateByCodeMissingRejected() {
        Mockito.when(props.doesTenantEnabled()).thenReturn(true);
        Mockito.when(tenantService.getByCode("nope", false)).thenReturn(null);

        var ret = facade.validateByCode("nope");
        Assertions.assertFalse(ret.isValid());
        Assertions.assertEquals(TenantErrorCodeEnum.A03001, ret.getErrorCode());
    }

    @Test
    void validateByCodeDisabledTenantRejected() {
        Mockito.when(props.doesTenantEnabled()).thenReturn(true);
        Mockito.when(tenantService.getByCode("t5", false)).thenReturn(
                meta(5L, EnabledStatusEnum.DISABLED));

        var ret = facade.validateByCode("t5");
        Assertions.assertFalse(ret.isValid());
        Assertions.assertEquals(TenantErrorCodeEnum.A03006, ret.getErrorCode());
    }

    @Test
    void validateByCodeHappyPath() {
        Mockito.when(props.doesTenantEnabled()).thenReturn(true);
        Mockito.when(tenantService.getByCode("t5", false)).thenReturn(
                meta(5L, EnabledStatusEnum.ENABLED));

        var ret = facade.validateByCode("t5");
        Assertions.assertTrue(ret.isValid());
        Assertions.assertEquals(5L, ret.getTenantId());
        Assertions.assertEquals("t5", ret.getTenantCode());
    }

    @Test
    void validateByCodeSwallowsDbFailure() {
        Mockito.when(props.doesTenantEnabled()).thenReturn(true);
        Mockito.when(tenantService.getByCode(Mockito.anyString(), Mockito.anyBoolean()))
                .thenThrow(new RuntimeException("db down"));

        var ret = facade.validateByCode("t5");
        Assertions.assertFalse(ret.isValid());
        Assertions.assertEquals(TenantErrorCodeEnum.B03001, ret.getErrorCode());
    }

    @Test
    void featureFlagDelegates() {
        Mockito.when(props.doesTenantEnabled()).thenReturn(true);
        Mockito.when(props.getLoginMode()).thenReturn(TenantLoginModeEnum.USER_FIRST);

        Assertions.assertTrue(facade.isTenantEnabled());
        Assertions.assertEquals(TenantLoginModeEnum.USER_FIRST, facade.getLoginMode());
    }


    /*
    ----------------------------------------------------------------
                        listSelectableTenants
    ----------------------------------------------------------------
     */

    @Test
    void listSelectableTenantsNullUserYieldsEmpty() {
        Assertions.assertEquals(List.of(), facade.listSelectableTenants(null));
    }

    @Test
    void listSelectableTenantsSuperAdminExcludesPlatform() {
        Mockito.when(tenantUserRoleFacade.isSuperAdmin(1L)).thenReturn(true);
        Mockito.when(tenantService.listEnabled()).thenReturn(List.of(
                meta(TenantConstant.PLATFORM_TENANT_ID, EnabledStatusEnum.ENABLED),
                meta(5L, EnabledStatusEnum.ENABLED)));

        var ret = facade.listSelectableTenants(1L);

        Assertions.assertEquals(1, ret.size());
        Assertions.assertEquals(5L, ret.get(0).getTenantId());
    }

    @Test
    void listSelectableTenantsUserFirstExpandsRelations() {
        Mockito.when(tenantUserRoleFacade.isSuperAdmin(2L)).thenReturn(false);
        Mockito.when(props.getLoginMode()).thenReturn(TenantLoginModeEnum.USER_FIRST);
        Mockito.when(tenantUserRoleFacade.listEnabledTenantIdsByUser(2L)).thenReturn(List.of(5L, 0L, 9L));
        Mockito.when(tenantService.getById(5L, false)).thenReturn(meta(5L, EnabledStatusEnum.ENABLED));
        Mockito.when(tenantService.getById(9L, false)).thenReturn(meta(9L, EnabledStatusEnum.ENABLED));

        var ret = facade.listSelectableTenants(2L);

        // 平台自营域被排除，保持加入先后顺序
        Assertions.assertEquals(2, ret.size());
        Assertions.assertEquals(5L, ret.get(0).getTenantId());
        Assertions.assertEquals(9L, ret.get(1).getTenantId());
    }

    @Test
    void listSelectableTenantsTenantFirstYieldsHomeOnly() {
        Mockito.when(tenantUserRoleFacade.isSuperAdmin(2L)).thenReturn(false);
        Mockito.when(props.getLoginMode()).thenReturn(TenantLoginModeEnum.TENANT_FIRST);
        Mockito.when(tenantUserRoleFacade.getUserHomeTenantId(2L)).thenReturn(5L);
        Mockito.when(tenantService.getById(5L, false)).thenReturn(meta(5L, EnabledStatusEnum.ENABLED));

        var ret = facade.listSelectableTenants(2L);

        Assertions.assertEquals(1, ret.size());
        Assertions.assertEquals(5L, ret.get(0).getTenantId());
    }

    @Test
    void listSelectableTenantsSwallowsFailure() {
        Mockito.when(tenantUserRoleFacade.isSuperAdmin(2L)).thenReturn(false);
        Mockito.when(props.getLoginMode()).thenReturn(TenantLoginModeEnum.TENANT_FIRST);
        Mockito.when(tenantUserRoleFacade.getUserHomeTenantId(2L)).thenThrow(new RuntimeException("x"));

        Assertions.assertEquals(List.of(), facade.listSelectableTenants(2L));
    }


    /*
    ----------------------------------------------------------------
                        切换目标/断言
    ----------------------------------------------------------------
     */

    @Test
    void resolveSwitchTargetBranches() {
        Mockito.when(tenantService.getByCode(Mockito.anyString(), Mockito.anyBoolean()))
                .thenThrow(new RuntimeException("db down"));
        var ex = Assertions.assertThrows(BusinessException.class, () -> facade.resolveSwitchTarget("t5"));
        Assertions.assertEquals(TenantErrorCodeEnum.B03001, ex.getErrorCode());

        Mockito.doReturn(null).when(tenantService).getByCode("t5", false);
        var ex2 = Assertions.assertThrows(BusinessException.class, () -> facade.resolveSwitchTarget("t5"));
        Assertions.assertEquals(TenantErrorCodeEnum.A03001, ex2.getErrorCode());

        Mockito.doReturn(meta(5L, EnabledStatusEnum.DISABLED)).when(tenantService).getByCode("t5", false);
        var ex3 = Assertions.assertThrows(BusinessException.class, () -> facade.resolveSwitchTarget("t5"));
        Assertions.assertEquals(TenantErrorCodeEnum.A03006, ex3.getErrorCode());

        Mockito.doReturn(meta(5L, EnabledStatusEnum.ENABLED)).when(tenantService).getByCode("t5", false);
        Assertions.assertEquals(5L, facade.resolveSwitchTarget("t5").getTenantId());
    }

    @Test
    void assertSwitchableBranches() {
        // 超管通行
        Mockito.when(tenantUserRoleFacade.isSuperAdmin(1L)).thenReturn(true);
        Assertions.assertDoesNotThrow(() -> facade.assertSwitchable(1L, 5L));

        // USER_FIRST 关联租户可切
        Mockito.when(tenantUserRoleFacade.isSuperAdmin(2L)).thenReturn(false);
        Mockito.when(props.getLoginMode()).thenReturn(TenantLoginModeEnum.USER_FIRST);
        Mockito.when(tenantUserRoleFacade.listEnabledTenantIdsByUser(2L)).thenReturn(List.of(5L));
        Assertions.assertDoesNotThrow(() -> facade.assertSwitchable(2L, 5L));

        // USER_FIRST 平台自营域不可切
        var ex = Assertions.assertThrows(BusinessException.class,
                () -> facade.assertSwitchable(2L, TenantConstant.PLATFORM_TENANT_ID));
        Assertions.assertEquals(TenantErrorCodeEnum.A03012, ex.getErrorCode());

        // USER_FIRST 未关联租户不可切
        var ex2 = Assertions.assertThrows(BusinessException.class, () -> facade.assertSwitchable(2L, 7L));
        Assertions.assertEquals(TenantErrorCodeEnum.A03012, ex2.getErrorCode());

        // TENANT_FIRST 普通用户一律不可切
        Mockito.when(props.getLoginMode()).thenReturn(TenantLoginModeEnum.TENANT_FIRST);
        var ex3 = Assertions.assertThrows(BusinessException.class, () -> facade.assertSwitchable(2L, 5L));
        Assertions.assertEquals(TenantErrorCodeEnum.A03012, ex3.getErrorCode());
    }

    @Test
    void resolveEnabledTenantBranches() {
        Assertions.assertNull(facade.resolveEnabledTenant(null));

        Mockito.when(tenantService.getById(5L, false)).thenReturn(null);
        Assertions.assertNull(facade.resolveEnabledTenant(5L));

        Mockito.when(tenantService.getById(5L, false)).thenReturn(meta(5L, EnabledStatusEnum.DISABLED));
        Assertions.assertNull(facade.resolveEnabledTenant(5L));

        Mockito.when(tenantService.getById(5L, false)).thenReturn(meta(5L, EnabledStatusEnum.ENABLED));
        TenantContext ctx = facade.resolveEnabledTenant(5L);
        Assertions.assertNotNull(ctx);
        Assertions.assertEquals(5L, ctx.getTenantId());
    }

    @Test
    void resolveDefaultTenantBranches() {
        Assertions.assertNull(facade.resolveDefaultTenant(null));

        Mockito.when(tenantUserRoleFacade.getUserHomeTenantId(2L)).thenReturn(null);
        Assertions.assertNull(facade.resolveDefaultTenant(2L));

        Mockito.when(tenantUserRoleFacade.getUserHomeTenantId(2L)).thenReturn(5L);
        Mockito.when(tenantService.getById(5L, false)).thenReturn(meta(5L, EnabledStatusEnum.DISABLED));
        Assertions.assertNull(facade.resolveDefaultTenant(2L));

        Mockito.when(tenantService.getById(5L, false)).thenReturn(meta(5L, EnabledStatusEnum.ENABLED));
        Assertions.assertEquals(5L, facade.resolveDefaultTenant(2L).getTenantId());
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private TenantMetaDTO meta(Long id, EnabledStatusEnum status) {
        return new TenantMetaDTO().setId(id).setCode("t" + id).setName("T" + id).setStatus(status);
    }
}
