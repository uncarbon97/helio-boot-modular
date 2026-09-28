package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysDeptEntity;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserTenantRelationMapper;
import cc.uncarbon.module.sys.enums.SysRoleFlagEnum;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.helper.UserRoleHelper;
import cc.uncarbon.module.sys.model.internal.UserDeptScope;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cc.uncarbon.module.sys.model.query.AdminSysUserListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysUserBindDeptRequest;
import cc.uncarbon.module.sys.model.request.AdminSysUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.AdminSysUserCreateRequest;
import cc.uncarbon.module.sys.model.request.AdminSysUserResetPasswordRequest;
import cc.uncarbon.module.sys.model.request.AdminSysUserUpdateRequest;
import cc.uncarbon.module.sys.model.request.TenantUserCreateRequest;
import cc.uncarbon.module.sys.model.response.TenantUserCreateResult;
import cc.uncarbon.module.sys.model.valueobj.SysDeptDTO;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cc.uncarbon.module.sys.service.SysUserDeptRelationService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cc.uncarbon.module.sys.MybatisPlusTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.Set;
import cc.uncarbon.module.sys.model.query.AdminSysRoleListRelatedUserQuery;
import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;

/**
 * {@link SysUserServiceImpl} 用户管理越权/校验分支测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysUserServiceImplTest {

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysDeptServiceImpl sysDeptService;
    @Mock
    private SysUserDeptRelationService sysUserDeptRelationService;
    @Mock
    private SysUserRoleRelationService sysUserRoleRelationService;
    @Mock
    private SysRoleMenuRelationService sysRoleMenuRelationService;
    @Mock
    private UserRoleHelper userRoleHelper;
    @Mock
    private SysUserTenantRelationMapper sysUserTenantRelationMapper;

    @InjectMocks
    private SysUserServiceImpl service;


    /*
    ----------------------------------------------------------------
                        查询 list
    ----------------------------------------------------------------
     */

    @Test
    void adminListSelectedDeptWithoutUsersReturnsEmpty() {
        AdminSysUserListQuery query = new AdminSysUserListQuery()
                .setPageParam(new PageParam(1, 10))
                .setSelectedDeptId(88L);
        Mockito.when(sysUserDeptRelationService.listUserIdsByDepts(Set.of(88L))).thenReturn(Set.of());

        var ret = service.adminList(query);

        Assertions.assertTrue(ret.getRecords().isEmpty());
        Mockito.verify(sysUserMapper, Mockito.never()).selectPage(Mockito.any(), Mockito.any());
    }

    @Test
    void adminListFillsPrimaryDept() {
        AdminSysUserListQuery query = new AdminSysUserListQuery().setPageParam(new PageParam(1, 10));
        Mockito.when(userRoleHelper.listHiddenUserIds()).thenReturn(Set.of());
        Mockito.when(sysUserMapper.selectPage(Mockito.any(), Mockito.any())).thenAnswer(inv -> {
            Page<SysUserEntity> page = inv.getArgument(0);
            page.setRecords(List.of(new SysUserEntity().setId(3L).setPin("u3").setNickname("n3")));
            page.setTotal(1);
            return page;
        });
        SysDeptEntity dept = new SysDeptEntity().setId(5L).setName("研发部");
        Mockito.when(sysDeptService.getSpecifiedUserDept(3L, false))
                .thenReturn(new UserDeptScope(List.of(5L), List.of(dept)));

        var ret = service.adminList(query);

        Assertions.assertEquals(1, ret.getRecords().size());
        Assertions.assertEquals(5L, ret.getRecords().get(0).getDeptId());
        Assertions.assertEquals("研发部", ret.getRecords().get(0).getDeptName());
    }

    @Test
    void adminListRoleRelatedUsersWithoutRelationsReturnsEmpty() {
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(UserRoleScope.mockSuperAdmin());
        Mockito.when(sysUserRoleRelationService.listUserIdsByRole(5L)).thenReturn(Set.of());

        var ret = service.adminListRoleRelatedUsers(
                new AdminSysRoleListRelatedUserQuery()
                        .setPageParam(new PageParam(1, 10)).setRoleId(5L));

        Assertions.assertTrue(ret.getRecords().isEmpty());
        Mockito.verify(sysUserMapper, Mockito.never()).selectPage(Mockito.any(), Mockito.any());
    }


    /*
    ----------------------------------------------------------------
                        新增/修改/状态/删除
    ----------------------------------------------------------------
     */

    @Test
    void adminCreateDuplicatePinRejected() {
        Mockito.when(sysUserMapper.selectOne(Mockito.any())).thenReturn(new SysUserEntity().setId(1L));

        var ex = Assertions.assertThrows(HasRepeatRecordException.class,
                () -> withUser(0L, () -> service.adminCreate(createRequest(null))));
        Assertions.assertEquals(SysErrorCodeEnum.A01030, ex.getErrorCode());
    }

    @Test
    void adminCreateDisabledDeptRejected() {
        Mockito.when(sysUserMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(UserRoleScope.mockSuperAdmin());
        Mockito.when(sysDeptService.getNonnullById(5L)).thenReturn(new SysDeptDTO().setId(5L).setStatus(EnabledStatusEnum.DISABLED));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(0L, () -> service.adminCreate(createRequest(5L))));
        Assertions.assertEquals(SysErrorCodeEnum.A01035, ex.getErrorCode());
    }

    @Test
    void adminCreateHashesPwdAndDisablesByDefault() {
        Mockito.when(sysUserMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(UserRoleScope.mockSuperAdmin());
        Mockito.when(sysUserMapper.insert(Mockito.any(SysUserEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysUserEntity.class).setId(123L);
            return 1;
        });

        Long id = service.adminCreate(createRequest(null));

        Assertions.assertEquals(123L, id);
        ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
        Mockito.verify(sysUserMapper).insert(captor.capture());
        SysUserEntity saved = captor.getValue();
        Assertions.assertEquals(SysUserStatusEnum.DISABLED, saved.getStatus());
        Assertions.assertTrue(saved.getPwd().startsWith("$argon2id$"));
        Assertions.assertNotEquals("init123456", saved.getPwd());
        Mockito.verify(sysUserDeptRelationService).cleanAndBind(123L, null);
    }

    @Test
    void adminUpdateMissingUserRejected() {
        Mockito.when(sysUserMapper.exists(Mockito.any())).thenReturn(false);

        Assertions.assertThrows(NoRecordException.class,
                () -> withUser(0L, () -> service.adminUpdate(updateRequest(9L))));
    }

    @Test
    void adminUpdateSelfRejectedForNonSuperAdmin() {
        Mockito.when(sysUserMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysUserMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(normalScope(7L, List.of(5L)));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(7L, () -> service.adminUpdate(updateRequest(7L))));
        Assertions.assertEquals(SysErrorCodeEnum.A01020, ex.getErrorCode());
    }

    @Test
    void adminUpdateTenantAdminTargetRejected() {
        Mockito.when(sysUserMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysUserMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(normalScope(7L, List.of(5L)));
        Mockito.when(userRoleHelper.getSpecifiedUserRole(8L)).thenReturn(
                new UserRoleScope(List.of(6L), List.of(new SysRoleEntity().setId(6L).setCode(SysConstant.TENANT_ADMIN_ROLE_CODE))));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(7L, () -> service.adminUpdate(updateRequest(8L))));
        Assertions.assertEquals(SysErrorCodeEnum.A01021, ex.getErrorCode());
    }

    @Test
    void adminSetStatusSuperAdminCannotDisableSelf() {
        Mockito.when(sysUserMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(UserRoleScope.mockSuperAdmin());

        var request = new AdminSetStatusRequest<Long, SysUserStatusEnum>()
                .setId(0L).setNewStatus(SysUserStatusEnum.DISABLED);
        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(0L, () -> service.adminSetStatus(request)));
        Assertions.assertEquals(SysErrorCodeEnum.A01020, ex.getErrorCode());
    }

    @Test
    void adminSetStatusHappyPath() {
        Mockito.when(sysUserMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(UserRoleScope.mockSuperAdmin());
        Mockito.when(userRoleHelper.listHiddenUserIds()).thenReturn(Set.of());

        var request = new AdminSetStatusRequest<Long, SysUserStatusEnum>()
                .setId(3L).setNewStatus(SysUserStatusEnum.ENABLED);
        withUser(0L, () -> service.adminSetStatus(request));

        Mockito.verify(sysUserMapper).updateStatusBatch(List.of(3L), SysUserStatusEnum.ENABLED);
    }

    @Test
    void adminDeleteSelfRejected() {
        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(7L, () -> service.adminDelete(List.of(7L))));
        Assertions.assertEquals(SysErrorCodeEnum.A01020, ex.getErrorCode());
        Mockito.verify(sysUserMapper, Mockito.never()).deleteByIds(Mockito.anyCollection());
    }

    @Test
    void adminDeleteBuiltinRoleUserRejected() {
        Mockito.when(userRoleHelper.getSpecifiedUserRole(8L)).thenReturn(new UserRoleScope(
                List.of(6L), List.of(new SysRoleEntity().setId(6L).assignFlags(List.of(SysRoleFlagEnum.BUILTIN)))));
        Mockito.when(userRoleHelper.listHiddenUserIds()).thenReturn(Set.of());

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(7L, () -> service.adminDelete(List.of(8L))));
        Assertions.assertEquals(SysErrorCodeEnum.A01023, ex.getErrorCode());
    }

    @Test
    void adminDeleteUnbindsRelationsThenDeletes() {
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(UserRoleScope.mockSuperAdmin());
        Mockito.when(userRoleHelper.getSpecifiedUserRole(8L)).thenReturn(normalScope(8L, List.of(5L)));
        Mockito.when(userRoleHelper.listHiddenUserIds()).thenReturn(Set.of());

        withUser(0L, () -> service.adminDelete(List.of(8L)));

        Mockito.verify(sysUserDeptRelationService).cleanAndBind(8L, null);
        Mockito.verify(sysUserRoleRelationService).cleanAndBindByUser(8L, null);
        Mockito.verify(sysUserTenantRelationMapper).delete(Mockito.any());
        Mockito.verify(sysUserMapper).deleteByIds(List.of(8L));
    }


    /*
    ----------------------------------------------------------------
                        绑定角色/部门
    ----------------------------------------------------------------
     */

    @Test
    void adminBindRoleSuperAdminCannotStripOwnSuperRole() {
        // 超管自身当前无内置角色关联（内置不可变校验通过），再把超管角色去掉 → 拒绝
        Mockito.when(sysUserRoleRelationService.listRoleIdsByUser(0L)).thenReturn(List.of());
        Mockito.when(sysRoleMapper.selectByIds(Mockito.anyCollection())).thenReturn(List.of());
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(UserRoleScope.mockSuperAdmin());

        var request = new AdminSysUserBindRoleRequest().setUserId(0L).setRoleIds(Set.of(5L));
        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(0L, () -> service.adminBindRole(request)));
        Assertions.assertEquals(SysErrorCodeEnum.A01020, ex.getErrorCode());
    }

    @Test
    void adminBindRoleBuiltinRoleCannotBeManuallyAssigned() {
        // 目标用户未关联内置角色，请求分配内置角色 0
        Mockito.when(sysUserRoleRelationService.listRoleIdsByUser(2L)).thenReturn(List.of());
        Mockito.when(sysRoleMapper.selectByIds(Mockito.anyCollection()))
                .thenReturn(List.of(new SysRoleEntity().setId(0L).assignFlags(List.of(SysRoleFlagEnum.BUILTIN))));

        var request = new AdminSysUserBindRoleRequest().setUserId(2L).setRoleIds(Set.of(0L));
        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(0L, () -> service.adminBindRole(request)));
        Assertions.assertEquals(SysErrorCodeEnum.A01024, ex.getErrorCode());
    }

    @Test
    void adminBindRoleBuiltinRoleCannotBeDetached() {
        // 目标用户已关联内置角色 6，请求列表不含 6 → 解除被拒
        Mockito.when(sysUserRoleRelationService.listRoleIdsByUser(2L)).thenReturn(List.of(6L));
        Mockito.when(sysRoleMapper.selectByIds(Mockito.anyCollection()))
                .thenReturn(List.of(new SysRoleEntity().setId(6L).assignFlags(List.of(SysRoleFlagEnum.BUILTIN))));

        var request = new AdminSysUserBindRoleRequest().setUserId(2L).setRoleIds(Set.of(5L));
        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(0L, () -> service.adminBindRole(request)));
        Assertions.assertEquals(SysErrorCodeEnum.A01025, ex.getErrorCode());
    }

    @Test
    void adminBindRoleNonSuperAdminCannotTouchSelf() {
        Mockito.when(sysUserRoleRelationService.listRoleIdsByUser(7L)).thenReturn(List.of());
        Mockito.when(sysRoleMapper.selectByIds(Mockito.anyCollection())).thenReturn(List.of());
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(normalScope(7L, List.of(5L)));

        var request = new AdminSysUserBindRoleRequest().setUserId(7L).setRoleIds(Set.of(5L));
        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(7L, () -> service.adminBindRole(request)));
        Assertions.assertEquals(SysErrorCodeEnum.A01020, ex.getErrorCode());
    }

    @Test
    void adminBindRoleAdminTargetRejected() {
        Mockito.when(sysUserRoleRelationService.listRoleIdsByUser(8L)).thenReturn(List.of());
        Mockito.when(sysRoleMapper.selectByIds(Mockito.anyCollection())).thenReturn(List.of());
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(normalScope(7L, List.of(5L)));
        Mockito.when(userRoleHelper.getSpecifiedUserRole(8L)).thenReturn(
                new UserRoleScope(List.of(6L), List.of(new SysRoleEntity().setId(6L).setCode(SysConstant.TENANT_ADMIN_ROLE_CODE))));

        var request = new AdminSysUserBindRoleRequest().setUserId(8L).setRoleIds(Set.of(5L));
        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(7L, () -> service.adminBindRole(request)));
        Assertions.assertEquals(SysErrorCodeEnum.A01021, ex.getErrorCode());
    }

    @Test
    void adminBindRoleOverMenuGrantRejectedForNormalUser() {
        Mockito.when(sysUserRoleRelationService.listRoleIdsByUser(9L)).thenReturn(List.of());
        Mockito.when(sysRoleMapper.selectByIds(Mockito.anyCollection())).thenReturn(List.of());
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(normalScope(7L, List.of(5L)));
        Mockito.when(userRoleHelper.getSpecifiedUserRole(9L)).thenReturn(normalScope(9L, List.of(5L)));
        Mockito.when(sysRoleMenuRelationService.listMenuIdsByRoles(List.of(5L))).thenReturn(Set.of(1L));
        Mockito.when(sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(6L))).thenReturn(Set.of(1L, 2L));
        Mockito.when(userRoleHelper.listHiddenUserIds()).thenReturn(Set.of());

        var request = new AdminSysUserBindRoleRequest().setUserId(9L).setRoleIds(Set.of(6L));
        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(7L, () -> service.adminBindRole(request)));
        Assertions.assertEquals(SysErrorCodeEnum.A01022, ex.getErrorCode());
    }

    @Test
    void adminBindRoleTenantAdminExistingRoleProceeds() {
        Mockito.when(sysUserRoleRelationService.listRoleIdsByUser(9L)).thenReturn(List.of());
        Mockito.when(sysRoleMapper.selectByIds(Mockito.anyCollection()))
                .thenReturn(List.of(new SysRoleEntity().setId(6L).setCode("R6")))
                .thenReturn(List.of(new SysRoleEntity().setId(6L).setCode("R6")));
        Mockito.when(userRoleHelper.getCurrentUserRole())
                .thenReturn(new UserRoleScope(List.of(5L), List.of(new SysRoleEntity().setId(5L).setCode(SysConstant.TENANT_ADMIN_ROLE_CODE))));
        Mockito.when(userRoleHelper.listHiddenRoleIds()).thenReturn(Set.of(SysConstant.SUPER_ADMIN_ROLE_ID));
        Mockito.when(userRoleHelper.getSpecifiedUserRole(9L)).thenReturn(new UserRoleScope(List.of(6L),
                List.of(new SysRoleEntity().setId(6L).setCode("R6").setName("R6"))));
        Mockito.when(userRoleHelper.listHiddenUserIds()).thenReturn(Set.of());
        Mockito.when(sysUserMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysUserMapper.selectById(9L)).thenReturn(new SysUserEntity().setId(9L).setPin("u9"));
        Mockito.when(sysDeptService.getSpecifiedUserDept(9L, false))
                .thenReturn(new UserDeptScope(List.of(), List.of()));

        var request = new AdminSysUserBindRoleRequest().setUserId(9L).setRoleIds(Set.of(6L));
        var ret = service.adminBindRole(request);

        Mockito.verify(sysUserRoleRelationService).cleanAndBindByUser(9L, Set.of(6L));
        Assertions.assertEquals(List.of("R6"), ret.getRoleNames());
    }

    @Test
    void adminBindDeptDisabledDeptRejected() {
        Mockito.when(sysUserMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(UserRoleScope.mockSuperAdmin());
        Mockito.when(userRoleHelper.listHiddenUserIds()).thenReturn(Set.of());
        Mockito.when(sysDeptService.getNonnullById(5L)).thenReturn(
                new SysDeptDTO().setId(5L).setStatus(EnabledStatusEnum.DISABLED));

        var request = new AdminSysUserBindDeptRequest().setUserId(9L).setDeptId(5L);
        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(0L, () -> service.adminBindDept(request)));
        Assertions.assertEquals(SysErrorCodeEnum.A01035, ex.getErrorCode());
    }

    @Test
    void adminBindDeptHappyPath() {
        Mockito.when(sysUserMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(UserRoleScope.mockSuperAdmin());
        Mockito.when(userRoleHelper.listHiddenUserIds()).thenReturn(Set.of());
        Mockito.when(sysDeptService.getNonnullById(5L)).thenReturn(
                new SysDeptDTO().setId(5L).setStatus(EnabledStatusEnum.ENABLED));

        var request = new AdminSysUserBindDeptRequest().setUserId(9L).setDeptId(5L);
        withUser(0L, () -> service.adminBindDept(request));

        Mockito.verify(sysUserDeptRelationService).cleanAndBind(9L, 5L);
    }


    /*
    ----------------------------------------------------------------
                        其他分支
    ----------------------------------------------------------------
     */

    @Test
    void checkRoleQueryAccessHiddenRoleRejected() {
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(normalScope(7L, List.of(5L)));
        Mockito.when(userRoleHelper.listHiddenRoleIds()).thenReturn(Set.of(0L));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(7L, () -> service.checkRoleQueryAccess(0L)));
        Assertions.assertEquals(SysErrorCodeEnum.A01021, ex.getErrorCode());
    }

    @Test
    void checkRoleQueryAccessMissingRoleRejected() {
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(normalScope(7L, List.of(5L)));
        Mockito.when(userRoleHelper.listHiddenRoleIds()).thenReturn(Set.of());
        Mockito.when(sysRoleMapper.exists(Mockito.any())).thenReturn(false);

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(7L, () -> service.checkRoleQueryAccess(6L)));
        Assertions.assertEquals(SysErrorCodeEnum.A01021, ex.getErrorCode());
    }

    @Test
    void checkRoleQueryAccessSuperAdminPasses() {
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(UserRoleScope.mockSuperAdmin());

        Assertions.assertDoesNotThrow(() -> withUser(0L, () -> service.checkRoleQueryAccess(6L)));
        Mockito.verify(sysRoleMapper, Mockito.never()).exists(Mockito.any());
    }

    @Test
    void createTenantUserOverridesFieldsForTenantAdmin() {
        Mockito.when(sysUserMapper.insert(Mockito.any(SysUserEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysUserEntity.class).setId(66L);
            return 1;
        });
        var request = new TenantUserCreateRequest()
                .setTenantId(10L).setTenantCode("t10").setTenantName("租户十")
                .setPin("tadmin").setPwdPlain("plain123").setEmail("a@b.c").setPhoneNo("13800000000")
                .setTenantAdmin(true);

        TenantUserCreateResult ret = service.createTenantUser(request);

        Assertions.assertEquals(66L, ret.getNewUserId());
        Assertions.assertTrue(ret.isTenantAdmin());
        ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
        Mockito.verify(sysUserMapper).insert(captor.capture());
        Assertions.assertEquals("租户十主管理员", captor.getValue().getNickname());
        Assertions.assertEquals(SysUserStatusEnum.ENABLED, captor.getValue().getStatus());
    }

    @Test
    void createTenantUserPlainMember() {
        Mockito.when(sysUserMapper.insert(Mockito.any(SysUserEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysUserEntity.class).setId(67L);
            return 1;
        });
        var request = new TenantUserCreateRequest()
                .setTenantId(10L).setTenantCode("t10").setTenantName("租户十")
                .setPin("tuser").setPwdPlain("plain123").setEmail("a@b.c").setPhoneNo("13800000000")
                .setTenantAdmin(false);

        TenantUserCreateResult ret = service.createTenantUser(request);

        Assertions.assertFalse(ret.isTenantAdmin());
        ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
        Mockito.verify(sysUserMapper).insert(captor.capture());
        Assertions.assertNull(captor.getValue().getNickname());
        Assertions.assertTrue(captor.getValue().getPwd().startsWith("$argon2id$"));
    }

    @Test
    void adminResetPasswordHashesRandomPassword() {
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(UserRoleScope.mockSuperAdmin());
        Mockito.when(sysUserMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysUserMapper.selectById(9L)).thenReturn(new SysUserEntity().setId(9L));

        withUser(0L, () -> service.adminResetPassword(new AdminSysUserResetPasswordRequest()
                .setUserId(9L).setRandomPassword("random-password-16").setMustChangePassword(YesOrNoEnum.YES)));

        ArgumentCaptor<String> pwdCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(sysUserMapper).updateEncryptedPwd(Mockito.eq(9L), pwdCaptor.capture(),
                Mockito.eq(YesOrNoEnum.YES));
        Assertions.assertTrue(pwdCaptor.getValue().startsWith("$argon2id$"));
    }

    @Test
    void getOperableByIdNullIdReturnsNull() {
        Assertions.assertNull(service.getOperableById(null));
    }

    @Test
    void getOperableByIdHiddenUserRejected() {
        Mockito.when(userRoleHelper.listHiddenUserIds()).thenReturn(Set.of(8L));

        var ex = Assertions.assertThrows(BusinessException.class, () -> service.getOperableById(8L));
        Assertions.assertEquals(SysErrorCodeEnum.A01021, ex.getErrorCode());
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private static void withUser(Long userId, Runnable op) {
        ScopedValue.where(UserContextHolder.scoped(), new SimpleUserContext().setUserId(userId)).run(op);
    }

    private AdminSysUserCreateRequest createRequest(Long deptId) {
        // 注意：父类链式 setter 返回父类型，子类字段单独赋值
        AdminSysUserCreateRequest ret = new AdminSysUserCreateRequest();
        ret.setPin("tester01");
        ret.setNickname("测试");
        ret.setEmail("a@b.c");
        ret.setPhoneNo("13800000000");
        ret.setInitPwd("init123456");
        ret.setDeptId(deptId);
        return ret;
    }

    private AdminSysUserUpdateRequest updateRequest(Long id) {
        return new AdminSysUserUpdateRequest()
                .setId(id).setPin("tester01").setNickname("测试").setEmail("a@b.c").setPhoneNo("13800000000");
    }

    private UserRoleScope normalScope(Long userId, List<Long> roleIds) {
        return new UserRoleScope(roleIds, roleIds.stream()
                .map(id -> new SysRoleEntity().setId(id).setCode("R" + id)).toList());
    }
}
