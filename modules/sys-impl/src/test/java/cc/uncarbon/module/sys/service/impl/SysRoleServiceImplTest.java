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
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.enums.SysRoleFlagEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.helper.UserRoleHelper;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cc.uncarbon.module.sys.model.query.AdminSysRoleListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysRoleBindMenuRequest;
import cc.uncarbon.module.sys.model.request.AdminSysRoleUpsertRequest;
import cc.uncarbon.module.sys.model.request.TenantRoleCreateRequest;
import cc.uncarbon.module.sys.model.response.TenantRoleCreateResult;
import cc.uncarbon.module.sys.service.SysMenuService;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
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
import java.util.Map;
import java.util.Set;

/**
 * {@link SysRoleServiceImpl} 角色管理校验分支测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysRoleServiceImplTest {

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysUserRoleRelationService sysUserRoleRelationService;
    @Mock
    private SysRoleMenuRelationService sysRoleMenuRelationService;
    @Mock
    private SysMenuService sysMenuService;
    @Mock
    private UserRoleHelper userRoleHelper;

    @InjectMocks
    private SysRoleServiceImpl service;


    @Test
    void adminListFillsMenuIds() {
        Mockito.when(userRoleHelper.listHiddenRoleIds()).thenReturn(Set.of());
        Mockito.when(sysRoleMapper.selectPage(Mockito.any(), Mockito.any())).thenAnswer(inv -> {
            Page<SysRoleEntity> page = inv.getArgument(0);
            page.setRecords(List.of(new SysRoleEntity().setId(5L).setCode("R5").setName("角色五")));
            page.setTotal(1);
            return page;
        });
        Mockito.when(sysRoleMenuRelationService.listMenuIdsByRoles(Mockito.anyCollection()))
                .thenReturn(Set.of(1L, 2L));

        var ret = service.adminList(new AdminSysRoleListQuery().setPageParam(new PageParam(1, 10)));

        Assertions.assertEquals(1, ret.getRecords().size());
        Assertions.assertEquals(Set.of(1L, 2L), ret.getRecords().get(0).getMenuIds());
    }

    @Test
    void adminCreateDuplicateCodeRejected() {
        Mockito.when(sysRoleMapper.selectOne(Mockito.any())).thenReturn(new SysRoleEntity().setId(1L));

        var ex = Assertions.assertThrows(HasRepeatRecordException.class,
                () -> service.adminCreate(upsert(null, "R5")));
        Assertions.assertEquals(SysErrorCodeEnum.A01031, ex.getErrorCode());
    }

    @Test
    void adminCreateUnacceptableCodesRejected() {
        Mockito.when(sysRoleMapper.selectOne(Mockito.any())).thenReturn(null);

        var ex1 = Assertions.assertThrows(BusinessException.class,
                () -> service.adminCreate(upsert(null, SysConstant.SUPER_ADMIN_ROLE_CODE)));
        Assertions.assertEquals(SysErrorCodeEnum.A01010, ex1.getErrorCode());

        var ex2 = Assertions.assertThrows(BusinessException.class,
                () -> service.adminCreate(upsert(null, SysConstant.TENANT_ADMIN_ROLE_CODE)));
        Assertions.assertEquals(SysErrorCodeEnum.A01010, ex2.getErrorCode());
    }

    @Test
    void adminCreateHappyPath() {
        Mockito.when(sysRoleMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(sysRoleMapper.insert(Mockito.any(SysRoleEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysRoleEntity.class).setId(5L);
            return 1;
        });

        Assertions.assertEquals(5L, service.adminCreate(upsert(null, "R5")));
    }

    @Test
    void adminUpdateMissingRoleRejected() {
        Mockito.when(sysRoleMapper.exists(Mockito.any())).thenReturn(false);

        Assertions.assertThrows(NoRecordException.class, () -> service.adminUpdate(upsert(9L, "R9")));
    }

    @Test
    void adminUpdateBuiltinRoleRejected() {
        Mockito.when(sysRoleMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysRoleMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(sysRoleMapper.selectById(9L)).thenReturn(builtinRole(9L));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminUpdate(upsert(9L, "R9")));
        Assertions.assertEquals(SysErrorCodeEnum.A01013, ex.getErrorCode());
    }

    @Test
    void adminUpdateUnacceptableCodeRejected() {
        Mockito.when(sysRoleMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysRoleMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(sysRoleMapper.selectById(9L)).thenReturn(null);

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminUpdate(upsert(9L, SysConstant.SUPER_ADMIN_ROLE_CODE)));
        Assertions.assertEquals(SysErrorCodeEnum.A01010, ex.getErrorCode());
    }

    @Test
    void adminDeleteBuiltinRoleRejected() {
        Mockito.when(sysRoleMapper.selectByIds(Mockito.anyCollection()))
                .thenReturn(List.of(builtinRole(9L)));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminDelete(List.of(9L)));
        Assertions.assertEquals(SysErrorCodeEnum.A01011, ex.getErrorCode());
    }

    @Test
    void adminDeleteOwnRoleRejected() {
        Mockito.when(sysRoleMapper.selectByIds(Mockito.anyCollection())).thenReturn(List.of());
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(normalScope(7L, List.of(5L)));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(7L, () -> service.adminDelete(List.of(5L))));
        Assertions.assertEquals(SysErrorCodeEnum.A01012, ex.getErrorCode());
    }

    @Test
    void adminDeleteCleansRelations() {
        Mockito.when(sysRoleMapper.selectByIds(Mockito.anyCollection())).thenReturn(List.of());
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(UserRoleScope.mockSuperAdmin());

        withUser(0L, () -> service.adminDelete(List.of(6L)));

        Mockito.verify(sysUserRoleRelationService).deleteByRoleIds(List.of(6L));
        Mockito.verify(sysRoleMenuRelationService).deleteByRoleIds(List.of(6L));
        Mockito.verify(sysRoleMapper).deleteByIds(List.of(6L));
    }

    @Test
    void adminBindMenuMissingRoleRejected() {
        Mockito.when(sysRoleMapper.exists(Mockito.any())).thenReturn(false);

        Assertions.assertThrows(NoRecordException.class,
                () -> service.adminBindMenu(bindMenu(9L, Set.of(1L))));
    }

    @Test
    void adminBindMenuBuiltinRoleRejected() {
        Mockito.when(sysRoleMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysRoleMapper.selectById(9L)).thenReturn(builtinRole(9L));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminBindMenu(bindMenu(9L, Set.of(1L))));
        Assertions.assertEquals(SysErrorCodeEnum.A01013, ex.getErrorCode());
    }

    @Test
    void adminBindMenuOwnRoleRejected() {
        Mockito.when(sysRoleMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysRoleMapper.selectById(5L)).thenReturn(new SysRoleEntity().setId(5L).setCode("R5"));
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(normalScope(7L, List.of(5L)));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(7L, () -> service.adminBindMenu(bindMenu(5L, Set.of(1L)))));
        Assertions.assertEquals(SysErrorCodeEnum.A01014, ex.getErrorCode());
    }

    @Test
    void adminBindMenuSuperAdminOnlyMenuRejected() {
        Mockito.when(sysRoleMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysRoleMapper.selectById(6L)).thenReturn(new SysRoleEntity().setId(6L).setCode("R6"));
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(normalScope(7L, List.of(5L)));
        Mockito.when(sysMenuService.listSuperAdminOnlySubtreeMenuIds()).thenReturn(Set.of(42L));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(7L, () -> service.adminBindMenu(bindMenu(6L, Set.of(42L)))));
        Assertions.assertEquals(SysErrorCodeEnum.A01016, ex.getErrorCode());
    }

    @Test
    void adminBindMenuOverMenuGrantRejected() {
        Mockito.when(sysRoleMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysRoleMapper.selectById(6L)).thenReturn(new SysRoleEntity().setId(6L).setCode("R6"));
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(normalScope(7L, List.of(5L)));
        Mockito.when(sysMenuService.listSuperAdminOnlySubtreeMenuIds()).thenReturn(Set.of());
        Mockito.when(sysRoleMenuRelationService.listMenuIdsByRoles(List.of(5L))).thenReturn(Set.of(1L));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> withUser(7L, () -> service.adminBindMenu(bindMenu(6L, Set.of(1L, 2L)))));
        Assertions.assertEquals(SysErrorCodeEnum.A01015, ex.getErrorCode());
    }

    @Test
    void adminBindMenuWithinScopeProceeds() {
        Mockito.when(sysRoleMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysRoleMapper.selectById(6L)).thenReturn(new SysRoleEntity().setId(6L).setCode("R6"));
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(normalScope(7L, List.of(5L)));
        Mockito.when(sysMenuService.listSuperAdminOnlySubtreeMenuIds()).thenReturn(Set.of());
        Mockito.when(sysRoleMenuRelationService.listMenuIdsByRoles(List.of(5L))).thenReturn(Set.of(1L, 2L));
        Mockito.when(sysMenuService.getPermissionsByRole(Mockito.anyCollection()))
                .thenReturn(Map.of(6L, Set.of("p1")));

        withUser(7L, () -> service.adminBindMenu(bindMenu(6L, Set.of(2L))));

        Mockito.verify(sysRoleMenuRelationService).cleanAndBind(6L, Set.of(2L));
    }

    @Test
    void adminBindMenuSuperAdminSkipsScopeCheck() {
        Mockito.when(sysRoleMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysRoleMapper.selectById(6L)).thenReturn(new SysRoleEntity().setId(6L).setCode("R6"));
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(UserRoleScope.mockSuperAdmin());
        Mockito.when(sysMenuService.listSuperAdminOnlySubtreeMenuIds()).thenReturn(Set.of());
        Mockito.when(sysMenuService.getPermissionsByRole(Mockito.anyCollection())).thenReturn(Map.of());

        withUser(0L, () -> service.adminBindMenu(bindMenu(6L, Set.of(99L))));

        Mockito.verify(sysRoleMenuRelationService, Mockito.never())
                .listMenuIdsByRoles(Mockito.anyCollection());
        Mockito.verify(sysRoleMenuRelationService).cleanAndBind(6L, Set.of(99L));
    }

    @Test
    void adminSetStatusBranches() {
        // 缺失
        Mockito.when(sysRoleMapper.selectById(9L)).thenReturn(null);
        Assertions.assertThrows(NoRecordException.class,
                () -> service.adminSetStatus(status(9L, EnabledStatusEnum.ENABLED)));

        // 内置角色
        Mockito.when(sysRoleMapper.selectById(9L)).thenReturn(builtinRole(9L));
        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminSetStatus(status(9L, EnabledStatusEnum.DISABLED)));
        Assertions.assertEquals(SysErrorCodeEnum.A01013, ex.getErrorCode());

        // 正常
        Mockito.when(sysRoleMapper.selectById(9L)).thenReturn(new SysRoleEntity().setId(9L).setCode("R9"));
        service.adminSetStatus(status(9L, EnabledStatusEnum.DISABLED));
        ArgumentCaptor<SysRoleEntity> captor = ArgumentCaptor.forClass(SysRoleEntity.class);
        Mockito.verify(sysRoleMapper).updateById(captor.capture());
        Assertions.assertEquals(EnabledStatusEnum.DISABLED, captor.getValue().getStatus());
    }

    @Test
    void adminListSelectOptionNoMenuFill() {
        Mockito.when(userRoleHelper.listHiddenRoleIds()).thenReturn(Set.of());
        Mockito.when(sysRoleMapper.selectList(Mockito.any()))
                .thenReturn(List.of(new SysRoleEntity().setId(5L).setName("角色五")));

        var ret = service.adminListSelectOption();

        Assertions.assertEquals(1, ret.size());
        Assertions.assertNull(ret.get(0).getMenuIds());
        Mockito.verify(sysRoleMenuRelationService, Mockito.never())
                .listMenuIdsByRoles(Mockito.anyCollection());
    }

    @Test
    void createTenantRoleOverridesFieldsForTenantAdmin() {
        Mockito.when(sysRoleMapper.insert(Mockito.any(SysRoleEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysRoleEntity.class).setId(91L);
            return 1;
        });
        var request = new TenantRoleCreateRequest()
                .setTenantId(10L).setTenantCode("t10").setTenantName("租户十")
                .setCode("whatever").setName("随便").setStatus(EnabledStatusEnum.ENABLED)
                .setTenantAdmin(true);

        TenantRoleCreateResult ret = service.createTenantRole(request);

        Assertions.assertEquals(91L, ret.getNewRoleId());
        Assertions.assertTrue(ret.isTenantAdmin());
        ArgumentCaptor<SysRoleEntity> captor = ArgumentCaptor.forClass(SysRoleEntity.class);
        Mockito.verify(sysRoleMapper).insert(captor.capture());
        SysRoleEntity saved = captor.getValue();
        Assertions.assertEquals(SysConstant.TENANT_ADMIN_ROLE_CODE, saved.getCode());
        Assertions.assertEquals("租户十主管理员", saved.getName());
        Assertions.assertEquals(List.of(SysRoleFlagEnum.BUILTIN), saved.resolveFlags());
    }

    @Test
    void createTenantRolePlainRoleKeepsFields() {
        Mockito.when(sysRoleMapper.insert(Mockito.any(SysRoleEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysRoleEntity.class).setId(92L);
            return 1;
        });
        var request = new TenantRoleCreateRequest()
                .setTenantId(10L).setTenantCode("t10").setTenantName("租户十")
                .setCode("R9").setName("角色九").setStatus(EnabledStatusEnum.ENABLED)
                .setTenantAdmin(false);

        TenantRoleCreateResult ret = service.createTenantRole(request);

        Assertions.assertFalse(ret.isTenantAdmin());
        ArgumentCaptor<SysRoleEntity> captor = ArgumentCaptor.forClass(SysRoleEntity.class);
        Mockito.verify(sysRoleMapper).insert(captor.capture());
        Assertions.assertEquals("R9", captor.getValue().getCode());
        Assertions.assertEquals(List.of(), captor.getValue().resolveFlags());
    }

    @Test
    void getByIdNullReturnsNull() {
        Assertions.assertNull(service.getById(null));
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private static void withUser(Long userId, Runnable op) {
        ScopedValue.where(UserContextHolder.scoped(), new SimpleUserContext().setUserId(userId)).run(op);
    }

    private AdminSysRoleUpsertRequest upsert(Long id, String code) {
        return new AdminSysRoleUpsertRequest().setId(id).setCode(code).setName("角色").setDescription("d");
    }

    private AdminSysRoleBindMenuRequest bindMenu(Long roleId, Set<Long> menuIds) {
        return new AdminSysRoleBindMenuRequest().setRoleId(roleId).setMenuIds(menuIds);
    }

    private AdminSetStatusRequest<Long, EnabledStatusEnum> status(Long id, EnabledStatusEnum newStatus) {
        return new AdminSetStatusRequest<Long, EnabledStatusEnum>().setId(id).setNewStatus(newStatus);
    }

    private SysRoleEntity builtinRole(Long id) {
        return new SysRoleEntity().setId(id).setCode("B" + id).assignFlags(List.of(SysRoleFlagEnum.BUILTIN));
    }

    private UserRoleScope normalScope(Long userId, List<Long> roleIds) {
        return new UserRoleScope(roleIds, roleIds.stream()
                .map(id -> new SysRoleEntity().setId(id).setCode("R" + id)).toList());
    }
}
