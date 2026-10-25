package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.db.fieldcrypto.blindindex.BlindIndexCalculator;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.MybatisPlusTestSupport;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.constant.SysUserCipherCols;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserTenantRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserTenantRelationMapper;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.model.request.TenantUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.TenantUserExpelRequest;
import cc.uncarbon.module.sys.model.request.TenantUserJoinRequest;
import cc.uncarbon.module.sys.model.request.TenantUserLookupRequest;
import cc.uncarbon.module.sys.model.valueobj.SysUserDTO;
import cc.uncarbon.module.sys.model.valueobj.TenantUserLookupVO;
import cc.uncarbon.module.sys.service.SysUserDeptRelationService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cc.uncarbon.module.tenant.constant.TenantConstant;
import cc.uncarbon.module.tenant.errorcode.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.facade.TenantConfigFacade;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * {@link SysUserFirstTenantServiceImpl} USER_FIRST 租户成员资格分支测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysUserFirstTenantServiceImplTest {

    private static final long TENANT_ID = 10L;

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysUserTenantRelationMapper sysUserTenantRelationMapper;
    @Mock
    private SysUserRoleRelationService sysUserRoleRelationService;
    @Mock
    private SysUserDeptRelationService sysUserDeptRelationService;
    @Mock
    private TenantUserRoleFacade tenantUserRoleFacade;
    @Mock
    private TenantFacade tenantFacade;
    @Mock
    private TenantConfigFacade tenantConfigFacade;
    @Mock
    private BlindIndexCalculator blindIndexCalculator;

    @InjectMocks
    private SysUserFirstTenantServiceImpl service;

    @BeforeEach
    void stubUserFirstMode() {
        Mockito.when(tenantConfigFacade.getLoginMode()).thenReturn(TenantLoginModeEnum.USER_FIRST);
    }


    /*
    ----------------------------------------------------------------
                        lookup 全局找人
    ----------------------------------------------------------------
     */

    @Test
    void lookupByPinReturnsMaskedProfileAndMemberFlag() {
        Mockito.when(sysUserMapper.getByPin("u66")).thenReturn(enabledUser());
        Mockito.when(sysUserTenantRelationMapper.existsByTenantIdAndUserId(Mockito.anyLong(), Mockito.anyLong())).thenReturn(true);

        List<TenantUserLookupVO> ret = withTenantContext(() ->
                service.lookupUsers(new TenantUserLookupRequest().setPin("u66")));

        Assertions.assertEquals(1, ret.size());
        TenantUserLookupVO first = ret.get(0);
        Assertions.assertEquals(66L, first.getUserId());
        Assertions.assertEquals("u66", first.getPin());
        Assertions.assertEquals("138****0000", first.getPhoneNo());
        Assertions.assertTrue(first.isMemberFlag());
    }

    @Test
    void lookupByPhoneMatchesFullBlindIndex() {
        Mockito.when(blindIndexCalculator.compute(Mockito.eq(SysUserCipherCols.PHONE_FULL_PURPOSE),
                Mockito.eq("13800000000"))).thenReturn("IDX");
        Mockito.when(sysUserMapper.selectList(Mockito.any())).thenReturn(List.of(enabledUser()));
        Mockito.when(sysUserTenantRelationMapper.existsByTenantIdAndUserId(Mockito.anyLong(), Mockito.anyLong())).thenReturn(false);

        List<TenantUserLookupVO> ret = withTenantContext(() ->
                service.lookupUsers(new TenantUserLookupRequest().setPhoneNo("13800000000")));

        Assertions.assertEquals(1, ret.size());
        Assertions.assertEquals(66L, ret.get(0).getUserId());
        Assertions.assertFalse(ret.get(0).isMemberFlag());
        // 明文不落 SQL，仅以盲索引等值匹配
        Mockito.verify(sysUserMapper).selectList(Mockito.argThat(w -> w != null));
    }

    @Test
    void lookupPhoneMultiHitReturnsAllUsableCandidates() {
        Mockito.when(blindIndexCalculator.compute(Mockito.anyString(), Mockito.anyString())).thenReturn("IDX");
        Mockito.when(sysUserMapper.selectList(Mockito.any())).thenReturn(List.of(
                enabledUser(),
                enabledUser().setId(67L).setPin("u67").setNickname("n67"),
                enabledUser().setId(68L).setPin("u68").setStatus(SysUserStatusEnum.DISABLED)));
        Mockito.when(sysUserTenantRelationMapper.existsByTenantIdAndUserId(Mockito.anyLong(), Mockito.anyLong())).thenReturn(false);

        List<TenantUserLookupVO> ret = withTenantContext(() ->
                service.lookupUsers(new TenantUserLookupRequest().setPhoneNo("13800000000")));

        // 同一手机号允许多账号，仅返回可用候选人
        Assertions.assertEquals(List.of(66L, 67L),
                ret.stream().map(TenantUserLookupVO::getUserId).toList());
    }

    @Test
    void lookupWithoutConditionRejected() {
        var ex = Assertions.assertThrows(BusinessException.class, () -> withTenantContext(() ->
                service.lookupUsers(new TenantUserLookupRequest())));
        Assertions.assertEquals(SysErrorCodeEnum.A01051, ex.getErrorCode());
    }

    @Test
    void lookupMergesMissingDisabledAndSuperAdmin() {
        Mockito.when(sysUserMapper.getByPin("missing")).thenReturn(null);
        var ex = Assertions.assertThrows(BusinessException.class, () -> withTenantContext(() ->
                service.lookupUsers(new TenantUserLookupRequest().setPin("missing"))));
        Assertions.assertEquals(SysErrorCodeEnum.A01046, ex.getErrorCode());

        Mockito.when(sysUserMapper.getByPin("u66")).thenReturn(
                enabledUser().setStatus(SysUserStatusEnum.DISABLED));
        ex = Assertions.assertThrows(BusinessException.class, () -> withTenantContext(() ->
                service.lookupUsers(new TenantUserLookupRequest().setPin("u66"))));
        Assertions.assertEquals(SysErrorCodeEnum.A01046, ex.getErrorCode());

        Mockito.when(sysUserMapper.getByPin("admin")).thenReturn(
                enabledUser().setId(0L).setPin("admin"));
        ex = Assertions.assertThrows(BusinessException.class, () -> withTenantContext(() ->
                service.lookupUsers(new TenantUserLookupRequest().setPin("admin"))));
        Assertions.assertEquals(SysErrorCodeEnum.A01046, ex.getErrorCode());
    }

    @Test
    void lookupPlatformTenantRejected() {
        Mockito.when(sysUserMapper.getByPin("u66")).thenReturn(enabledUser());
        var ex = Assertions.assertThrows(BusinessException.class, () ->
                TenantContextHolder.runWithContext(
                        new SimpleTenantContext(0L, "平台自营域", "0000"),
                        () -> service.lookupUsers(new TenantUserLookupRequest().setPin("u66"))));
        Assertions.assertEquals(SysErrorCodeEnum.A01052, ex.getErrorCode());
    }


    /*
    ----------------------------------------------------------------
                        join 拉入本租户
    ----------------------------------------------------------------
     */

    @Test
    void joinTenantFirstModeRejected() {
        Mockito.when(tenantConfigFacade.getLoginMode()).thenReturn(TenantLoginModeEnum.TENANT_FIRST);

        var ex = Assertions.assertThrows(BusinessException.class, () -> withTenantContext(() ->
                service.joinTenant(new TenantUserJoinRequest().setPin("u66"))));
        Assertions.assertEquals(SysErrorCodeEnum.A01045, ex.getErrorCode());
    }

    @Test
    void joinDisabledTenantRejected() {
        mockUserFirstMode();
        Mockito.when(sysUserMapper.getByPin("u66")).thenReturn(enabledUser());

        var ex = Assertions.assertThrows(BusinessException.class, () -> withTenantContext(() ->
                service.joinTenant(new TenantUserJoinRequest().setPin("u66"))));
        Assertions.assertEquals(TenantErrorCodeEnum.A03006, ex.getErrorCode());
    }

    @Test
    void joinDuplicateMemberRejected() {
        mockUserFirstMode();
        Mockito.when(tenantFacade.resolveEnabledTenant(TENANT_ID)).thenReturn(tenantContext());
        Mockito.when(sysUserMapper.getByPin("u66")).thenReturn(enabledUser());
        Mockito.when(sysUserTenantRelationMapper.existsByTenantIdAndUserId(Mockito.anyLong(), Mockito.anyLong())).thenReturn(true);

        var ex = Assertions.assertThrows(BusinessException.class, () -> withTenantContext(() ->
                service.joinTenant(new TenantUserJoinRequest().setPin("u66"))));
        Assertions.assertEquals(SysErrorCodeEnum.A01048, ex.getErrorCode());
    }

    @Test
    void joinSuperAdminRejected() {
        mockUserFirstMode();
        Mockito.when(tenantFacade.resolveEnabledTenant(TENANT_ID)).thenReturn(tenantContext());
        Mockito.when(sysUserMapper.getByPin("admin")).thenReturn(
                enabledUser().setId(SysConstant.SUPER_ADMIN_USER_ID).setPin("admin"));

        var ex = Assertions.assertThrows(BusinessException.class, () -> withTenantContext(() ->
                service.joinTenant(new TenantUserJoinRequest().setPin("admin"))));
        Assertions.assertEquals(SysErrorCodeEnum.A01047, ex.getErrorCode());
    }

    @Test
    void joinHappyPathWritesRelationAndBindsRoles() {
        mockUserFirstMode();
        Mockito.when(tenantFacade.resolveEnabledTenant(TENANT_ID)).thenReturn(tenantContext());
        Mockito.when(sysUserMapper.getByPin("u66")).thenReturn(enabledUser());
        Mockito.when(sysUserTenantRelationMapper.existsByTenantIdAndUserId(Mockito.anyLong(), Mockito.anyLong())).thenReturn(false);

        withTenantContext(() -> service.joinTenant(
                new TenantUserJoinRequest().setPin("u66").setRoleIds(List.of(91L))));

        ArgumentCaptor<SysUserTenantRelationEntity> captor =
                ArgumentCaptor.forClass(SysUserTenantRelationEntity.class);
        Mockito.verify(sysUserTenantRelationMapper).insert(captor.capture());
        Assertions.assertEquals(TENANT_ID, captor.getValue().getTenantId());
        Assertions.assertEquals(66L, captor.getValue().getUserId());
        Mockito.verify(tenantUserRoleFacade).bindTenantUserRoleRelation(
                Mockito.any(TenantUserBindRoleRequest.class));
    }

    @Test
    void joinWithoutRoleIdsSkipsBinding() {
        mockUserFirstMode();
        Mockito.when(tenantFacade.resolveEnabledTenant(TENANT_ID)).thenReturn(tenantContext());
        Mockito.when(sysUserMapper.getByPin("u66")).thenReturn(enabledUser());
        Mockito.when(sysUserTenantRelationMapper.existsByTenantIdAndUserId(Mockito.anyLong(), Mockito.anyLong())).thenReturn(false);

        withTenantContext(() -> service.joinTenant(new TenantUserJoinRequest().setPin("u66")));

        Mockito.verify(sysUserTenantRelationMapper).insert(Mockito.any(SysUserTenantRelationEntity.class));
        Mockito.verify(tenantUserRoleFacade, Mockito.never())
                .bindTenantUserRoleRelation(Mockito.any(TenantUserBindRoleRequest.class));
    }


    /*
    ----------------------------------------------------------------
                        expel 移出租户
    ----------------------------------------------------------------
     */

    @Test
    void expelTenantAdminRejected() {
        mockUserFirstMode();
        Mockito.when(tenantFacade.getTenantAdminUserId(TENANT_ID)).thenReturn(66L);

        var ex = Assertions.assertThrows(BusinessException.class, () -> withTenantContext(() ->
                service.expelTenantMember(new TenantUserExpelRequest().setUserId(66L))));
        Assertions.assertEquals(SysErrorCodeEnum.A01049, ex.getErrorCode());
    }

    @Test
    void expelSuperAdminRejected() {
        mockUserFirstMode();

        var ex = Assertions.assertThrows(BusinessException.class, () -> withTenantContext(() ->
                service.expelTenantMember(
                        new TenantUserExpelRequest().setUserId(SysConstant.SUPER_ADMIN_USER_ID))));
        Assertions.assertEquals(SysErrorCodeEnum.A01047, ex.getErrorCode());
    }

    @Test
    void expelNonMemberIdempotent() {
        mockUserFirstMode();
        Mockito.when(sysUserMapper.selectById(66L)).thenReturn(enabledUser());
        Mockito.when(sysUserTenantRelationMapper.existsByTenantIdAndUserId(Mockito.anyLong(), Mockito.anyLong())).thenReturn(false);

        SysUserDTO ret = withTenantContext(() ->
                service.expelTenantMember(new TenantUserExpelRequest().setUserId(66L)));

        // 仍返回被操作用户，供操作日志展示 pin
        Assertions.assertEquals("u66", ret.getPin());
        Mockito.verify(sysUserTenantRelationMapper, Mockito.never())
                .deleteByTenantIdAndUserId(Mockito.anyLong(), Mockito.anyLong());
        Mockito.verify(sysUserRoleRelationService, Mockito.never())
                .cleanAndBindByUser(Mockito.any(), Mockito.any());
    }

    @Test
    void expelRepairsProjectionWhenPointingHere() {
        mockUserFirstMode();
        Mockito.when(sysUserMapper.selectById(66L)).thenReturn(enabledUser());
        Mockito.when(sysUserTenantRelationMapper.existsByTenantIdAndUserId(Mockito.anyLong(), Mockito.anyLong())).thenReturn(true);
        Mockito.when(tenantUserRoleFacade.getUserHomeTenantId(66L)).thenReturn(TENANT_ID);
        Mockito.when(tenantUserRoleFacade.listEnabledTenantIdsByUser(66L)).thenReturn(List.of(7L, 8L));

        SysUserDTO ret = withTenantContext(() ->
                service.expelTenantMember(new TenantUserExpelRequest().setUserId(66L)));

        Assertions.assertEquals("u66", ret.getPin());
        Mockito.verify(sysUserTenantRelationMapper).deleteByTenantIdAndUserId(TENANT_ID, 66L);
        // 清理本租户内角色/部门关系
        Mockito.verify(sysUserRoleRelationService).cleanAndBindByUser(66L, null);
        Mockito.verify(sysUserDeptRelationService).cleanAndBind(66L, null);
        // 投影列改指首个剩余启用租户
        Mockito.verify(sysUserMapper).updateTenantId(66L, 7L);
    }

    @Test
    void expelKeepsProjectionWhenPointingElsewhere() {
        mockUserFirstMode();
        Mockito.when(sysUserMapper.selectById(66L)).thenReturn(enabledUser());
        Mockito.when(sysUserTenantRelationMapper.existsByTenantIdAndUserId(Mockito.anyLong(), Mockito.anyLong())).thenReturn(true);
        Mockito.when(tenantUserRoleFacade.getUserHomeTenantId(66L)).thenReturn(99L);

        withTenantContext(() -> service.expelTenantMember(new TenantUserExpelRequest().setUserId(66L)));

        Mockito.verify(sysUserTenantRelationMapper).deleteByTenantIdAndUserId(TENANT_ID, 66L);
        Mockito.verify(sysUserMapper, Mockito.never())
                .updateTenantId(Mockito.anyLong(), Mockito.any());
    }

    @Test
    void expelFallsBackToPlatformWhenNoRemaining() {
        mockUserFirstMode();
        Mockito.when(sysUserMapper.selectById(66L)).thenReturn(enabledUser());
        Mockito.when(sysUserTenantRelationMapper.existsByTenantIdAndUserId(Mockito.anyLong(), Mockito.anyLong())).thenReturn(true);
        Mockito.when(tenantUserRoleFacade.getUserHomeTenantId(66L)).thenReturn(TENANT_ID);
        Mockito.when(tenantUserRoleFacade.listEnabledTenantIdsByUser(66L)).thenReturn(List.of());

        withTenantContext(() -> service.expelTenantMember(new TenantUserExpelRequest().setUserId(66L)));

        Mockito.verify(sysUserMapper).updateTenantId(66L, TenantConstant.PLATFORM_TENANT_ID);
    }

    @Test
    void expelMissingUserRejected() {
        mockUserFirstMode();
        Mockito.when(sysUserMapper.selectById(66L)).thenReturn(null);

        Assertions.assertThrows(NoRecordException.class, () -> withTenantContext(() ->
                service.expelTenantMember(new TenantUserExpelRequest().setUserId(66L))));
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    private void mockUserFirstMode() {
        Mockito.when(tenantConfigFacade.getLoginMode()).thenReturn(TenantLoginModeEnum.USER_FIRST);
    }

    private SimpleTenantContext tenantContext() {
        return new SimpleTenantContext(TENANT_ID, "T10", "t10");
    }

    private SysUserEntity enabledUser() {
        return new SysUserEntity().setId(66L).setPin("u66").setNickname("n66")
                .setPhoneNoMasked("138****0000").setEmailMasked("a***@b.c")
                .setStatus(SysUserStatusEnum.ENABLED);
    }

    /**
     * ScopedValue 需在绑定作用域内执行业务调用
     */
    @SneakyThrows
    private <T> T withTenantContext(java.util.function.Supplier<T> op) {
        return TenantContextHolder.callWithContext(tenantContext(), op::get);
    }

    private void withTenantContext(Runnable op) {
        TenantContextHolder.runWithContext(tenantContext(), op);
    }
}
