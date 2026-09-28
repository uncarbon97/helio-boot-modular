package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.db.entity.AbstractTenantGenericEntity;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import cc.uncarbon.framework.helium.web.context.SimpleVisitorContext;
import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.helper.UserRoleHelper;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cc.uncarbon.module.sys.model.request.AdminAuthPasswordLoginRequest;
import cc.uncarbon.module.sys.model.request.SysLoginLogCreateRequest;
import cc.uncarbon.module.sys.model.response.SysUserLoginResult;
import cc.uncarbon.module.sys.service.SysLoginLogService;
import cc.uncarbon.module.sys.service.SysMenuService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cc.uncarbon.module.sys.util.PwdUtil;
import cc.uncarbon.module.tenant.errorcode.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaDTO;
import cc.uncarbon.module.tenant.model.valueobj.TenantValidateResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Set;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import cc.uncarbon.framework.helium.base.errorcode.StructuredErrorCode;

/**
 * {@link AdminLoginServiceImpl} 双登录模式 + 会话重建分支测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminLoginServiceImplTest {

    private static final String RAW_PWD = "pass12345678";
    private static final Long TENANT_ID = 5L;
    private static final String TENANT_CODE = "t5";
    private static final Long USER_ID = 2L;

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysMenuService sysMenuService;
    @Mock
    private SysLoginLogService sysLoginLogService;
    @Mock
    private UserRoleHelper userRoleHelper;
    @Mock
    private TenantFacade tenantFacade;
    @Mock
    private TenantUserRoleFacade tenantUserRoleFacade;
    @Mock
    private SysUserRoleRelationService sysUserRoleRelationService;

    @InjectMocks
    private AdminLoginServiceImpl service;


    /*
    ----------------------------------------------------------------
                        TENANT_FIRST
    ----------------------------------------------------------------
     */

    @Test
    void tenantFirstHappyPath() {
        stubTenantFirstValid();
        Mockito.when(sysUserMapper.getByPin("u2")).thenReturn(user());
        Mockito.when(userRoleHelper.getSpecifiedUserRole(USER_ID)).thenReturn(roleScope());
        Mockito.when(sysMenuService.getPermissionsByRole(Mockito.anyCollection()))
                .thenReturn(Map.of(5L, Set.of("p1", "p2")));

        SysUserLoginResult ret = service.passwordLogin(request(RAW_PWD), visitor());

        Assertions.assertEquals(USER_ID, ret.getId());
        Assertions.assertEquals(TENANT_ID, ret.getTenantContext().getTenantId());
        Assertions.assertEquals(List.of(5L), ret.getRoleIds());
        Assertions.assertEquals(List.of("R5"), ret.getRoleCodes());
        Assertions.assertEquals(Set.of("p1", "p2"), ret.getPermissions());
        Mockito.verify(sysUserMapper).updateLastLoginAt(Mockito.eq(USER_ID), Mockito.any());

        ArgumentCaptor<SysLoginLogCreateRequest> logCaptor = logCaptor();
        Mockito.verify(sysLoginLogService).create(logCaptor.capture());
        SysLoginLogCreateRequest saved = logCaptor.getValue();
        Assertions.assertEquals(USER_ID, saved.getUserId());
        Assertions.assertEquals(LogResultStatusEnum.SUCCESS, saved.getResultStatus());
        Assertions.assertEquals(TENANT_ID, saved.getTenantId());
        Assertions.assertNull(saved.getFailedMsg());
    }

    @Test
    void tenantFirstUnknownPinRejected() {
        stubTenantFirstValid();
        Mockito.when(sysUserMapper.getByPin("u2")).thenReturn(null);

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.passwordLogin(request(RAW_PWD), visitor()));
        Assertions.assertEquals(SysErrorCodeEnum.A01001, ex.getErrorCode());
        assertFailedLog(null, null);
    }

    @Test
    void tenantFirstWrongPwdRejected() {
        stubTenantFirstValid();
        Mockito.when(sysUserMapper.getByPin("u2")).thenReturn(user());

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.passwordLogin(request("wrong-pass-1"), visitor()));
        Assertions.assertEquals(SysErrorCodeEnum.A01001, ex.getErrorCode());
        assertFailedLog(USER_ID, SysErrorCodeEnum.A01001);
    }

    @Test
    void tenantFirstDisabledUserRejected() {
        stubTenantFirstValid();
        Mockito.when(sysUserMapper.getByPin("u2"))
                .thenReturn(user().setStatus(SysUserStatusEnum.DISABLED));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.passwordLogin(request(RAW_PWD), visitor()));
        Assertions.assertEquals(SysErrorCodeEnum.A01002, ex.getErrorCode());
    }

    @Test
    void tenantFirstAllRolesDisabledRejected() {
        stubTenantFirstValid();
        Mockito.when(sysUserMapper.getByPin("u2")).thenReturn(user());
        Mockito.when(userRoleHelper.getSpecifiedUserRole(USER_ID))
                .thenReturn(new UserRoleScope(List.of(), List.of()));
        Mockito.when(sysUserRoleRelationService.listRoleIdsByUser(USER_ID)).thenReturn(List.of(5L));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.passwordLogin(request(RAW_PWD), visitor()));
        Assertions.assertEquals(SysErrorCodeEnum.A01005, ex.getErrorCode());
    }

    @Test
    void tenantFirstInvalidTenantRejectedBeforeUserLookup() {
        Mockito.when(tenantFacade.getLoginMode()).thenReturn(TenantLoginModeEnum.TENANT_FIRST);
        Mockito.when(tenantFacade.validateByCode(TENANT_CODE))
                .thenReturn(TenantValidateResult.fail(TenantErrorCodeEnum.A03001));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.passwordLogin(request(RAW_PWD), visitor()));
        Assertions.assertEquals(TenantErrorCodeEnum.A03001, ex.getErrorCode());
        Mockito.verify(sysUserMapper, Mockito.never()).getByPin(Mockito.anyString());
        // 租户校验失败发生在 try/finally 之前，不落登录日志
        Mockito.verify(sysLoginLogService, Mockito.never()).create(Mockito.any());
    }


    /*
    ----------------------------------------------------------------
                        USER_FIRST
    ----------------------------------------------------------------
     */

    @Test
    void userFirstHappyPathRemembersTenantFirst() {
        Mockito.when(tenantFacade.getLoginMode()).thenReturn(TenantLoginModeEnum.USER_FIRST);
        // 记忆租户 8 + 关联 7：候选顺序应为 [8, 7]；8 不可用则激活 7
        SysUserEntity u2 = user();
        u2.setTenantId(8L);
        Mockito.when(sysUserMapper.getByPin("u2")).thenReturn(u2);
        Mockito.when(tenantUserRoleFacade.listEnabledTenantIdsByUser(USER_ID)).thenReturn(List.of(7L, 8L));
        Mockito.when(tenantFacade.resolveEnabledTenant(8L)).thenReturn(null);
        Mockito.when(tenantFacade.resolveEnabledTenant(7L)).thenReturn(ctx(7L));
        Mockito.when(userRoleHelper.getSpecifiedUserRole(USER_ID)).thenReturn(roleScope());
        Mockito.when(sysMenuService.getPermissionsByRole(Mockito.anyCollection()))
                .thenReturn(Map.of(5L, Set.of("p1")));
        Mockito.when(tenantFacade.listSelectableTenants(USER_ID)).thenReturn(List.of(ctx(7L)));

        SysUserLoginResult ret = service.passwordLogin(request(RAW_PWD), visitor());

        Assertions.assertEquals(7L, ret.getTenantContext().getTenantId());
        Assertions.assertNotNull(ret.getSelectableTenants());
        Mockito.verify(tenantUserRoleFacade).rememberActiveTenant(USER_ID, 7L);
        Mockito.verify(sysUserMapper).updateLastLoginAt(Mockito.eq(USER_ID), Mockito.any());
    }

    @Test
    void userFirstNoTenantRelationRejected() {
        Mockito.when(tenantFacade.getLoginMode()).thenReturn(TenantLoginModeEnum.USER_FIRST);
        SysUserEntity u2 = user();
        u2.setTenantId(null);
        Mockito.when(sysUserMapper.getByPin("u2")).thenReturn(u2);
        Mockito.when(tenantUserRoleFacade.listEnabledTenantIdsByUser(USER_ID)).thenReturn(List.of());

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.passwordLogin(request(RAW_PWD), visitor()));
        Assertions.assertEquals(SysErrorCodeEnum.A01008, ex.getErrorCode());
    }

    @Test
    void userFirstAllTenantsUnavailableRejected() {
        Mockito.when(tenantFacade.getLoginMode()).thenReturn(TenantLoginModeEnum.USER_FIRST);
        SysUserEntity u2 = user();
        u2.setTenantId(null);
        Mockito.when(sysUserMapper.getByPin("u2")).thenReturn(u2);
        Mockito.when(tenantUserRoleFacade.listEnabledTenantIdsByUser(USER_ID)).thenReturn(List.of(7L));
        Mockito.when(tenantFacade.resolveEnabledTenant(7L)).thenReturn(null);

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.passwordLogin(request(RAW_PWD), visitor()));
        Assertions.assertEquals(TenantErrorCodeEnum.A03006, ex.getErrorCode());
    }

    @Test
    void userFirstWrongPwdRejected() {
        Mockito.when(tenantFacade.getLoginMode()).thenReturn(TenantLoginModeEnum.USER_FIRST);
        Mockito.when(sysUserMapper.getByPin("u2")).thenReturn(user());

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.passwordLogin(request("wrong-pass-1"), visitor()));
        Assertions.assertEquals(SysErrorCodeEnum.A01001, ex.getErrorCode());
    }


    /*
    ----------------------------------------------------------------
                        会话上下文重建
    ----------------------------------------------------------------
     */

    @Test
    void buildSessionUserContextNullWhenUserMissingOrDisabled() {
        Mockito.when(sysUserMapper.selectById(9L)).thenReturn(null);
        Assertions.assertNull(service.buildSessionUserContext(9L));

        Mockito.when(sysUserMapper.selectById(9L))
                .thenReturn(user().setId(9L).setStatus(SysUserStatusEnum.DISABLED));
        Assertions.assertNull(service.buildSessionUserContext(9L));
    }

    @Test
    void buildSessionUserContextSwallowsDbFailure() {
        Mockito.when(sysUserMapper.selectById(9L)).thenThrow(new RuntimeException("db down"));

        Assertions.assertNull(service.buildSessionUserContext(9L));
    }

    @Test
    void buildSessionUserContextAssemblesByHomeTenant() {
        SysUserEntity u9 = user().setId(9L);
        u9.setTenantId(TENANT_ID);
        Mockito.when(sysUserMapper.selectById(9L)).thenReturn(u9);
        Mockito.when(userRoleHelper.getSpecifiedUserRole(9L)).thenReturn(roleScope());

        UserContext ctx = service.buildSessionUserContext(9L);

        Assertions.assertEquals(9L, ctx.getUserId());
        Assertions.assertEquals("u2", ctx.getUserPin());
        Assertions.assertEquals("ADMIN_USER", ctx.getUserTypeCode());
        Assertions.assertEquals(List.of(5L), ctx.getRoleIds());
        Assertions.assertEquals(List.of("R5"), ctx.getRoleCodes());
    }

    @Test
    void buildSessionUserContextWithTenantContext() {
        // null 入参 → 走归属租户版
        Assertions.assertNull(service.buildSessionUserContext(null, ctx(7L)));
        SysUserEntity u9 = user().setId(9L);
        u9.setTenantId(null);
        Mockito.when(sysUserMapper.selectById(9L)).thenReturn(u9);
        Mockito.when(userRoleHelper.getSpecifiedUserRole(9L)).thenReturn(roleScope());
        Assertions.assertNotNull(service.buildSessionUserContext(9L, null));

        // 普通用户 + 指定租户 → 在指定租户作用域内组装
        Mockito.when(userRoleHelper.getSpecifiedUserRole(9L)).thenReturn(roleScope());
        Assertions.assertNotNull(service.buildSessionUserContext(9L, ctx(7L)));
    }

    @Test
    void getTenantUIConfigBranches() {
        Mockito.when(tenantFacade.isTenantEnabled()).thenReturn(false);
        Mockito.when(tenantFacade.getLoginMode()).thenReturn(TenantLoginModeEnum.TENANT_FIRST);
        Assertions.assertFalse(service.getTenantUIConfig().isShowTenantCodeInputFlag());

        Mockito.when(tenantFacade.isTenantEnabled()).thenReturn(true);
        Assertions.assertTrue(service.getTenantUIConfig().isShowTenantCodeInputFlag());

        Mockito.when(tenantFacade.getLoginMode()).thenReturn(TenantLoginModeEnum.USER_FIRST);
        Assertions.assertFalse(service.getTenantUIConfig().isShowTenantCodeInputFlag());
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private void stubTenantFirstValid() {
        Mockito.when(tenantFacade.getLoginMode()).thenReturn(TenantLoginModeEnum.TENANT_FIRST);
        TenantMetaDTO meta = new TenantMetaDTO().setId(TENANT_ID).setCode(TENANT_CODE).setName("租户五");
        Mockito.when(tenantFacade.validateByCode(TENANT_CODE)).thenReturn(TenantValidateResult.pass(meta));
    }

    private AdminAuthPasswordLoginRequest request(String pwd) {
        return new AdminAuthPasswordLoginRequest().setPin("u2").setPwd(pwd).setTenantCode(TENANT_CODE);
    }

    private VisitorContext visitor() {
        return new SimpleVisitorContext().setClientIp("127.0.0.1").setUserAgent("UA");
    }

    private SysUserEntity user() {
        return new SysUserEntity().setId(USER_ID).setPin("u2").setNickname("n2")
                .setPwd(PwdUtil.hash(RAW_PWD)).setStatus(SysUserStatusEnum.ENABLED);
    }

    private UserRoleScope roleScope() {
        SysRoleEntity role = new SysRoleEntity().setId(5L).setCode("R5");
        return new UserRoleScope(List.of(5L), List.of(role));
    }

    private TenantContext ctx(Long tenantId) {
        return new SimpleTenantContext(tenantId, "T" + tenantId, "t" + tenantId);
    }

    private ArgumentCaptor<SysLoginLogCreateRequest> logCaptor() {
        return ArgumentCaptor.forClass(SysLoginLogCreateRequest.class);
    }

    private void assertFailedLog(Long expectedUserId, StructuredErrorCode expectedError) {
        assertFailedLog(expectedUserId, expectedError, TENANT_ID);
    }

    private void assertFailedLog(Long expectedUserId,
                                 StructuredErrorCode expectedError,
                                 Long expectedTenantId) {
        ArgumentCaptor<SysLoginLogCreateRequest> captor = logCaptor();
        Mockito.verify(sysLoginLogService).create(captor.capture());
        SysLoginLogCreateRequest saved = captor.getValue();
        Assertions.assertEquals(LogResultStatusEnum.FAILED, saved.getResultStatus());
        Assertions.assertEquals(expectedUserId, saved.getUserId());
        Assertions.assertEquals(expectedTenantId, saved.getTenantId());
        if (expectedError != null) {
            Assertions.assertNotNull(saved.getFailedMsg());
        }
    }
}
