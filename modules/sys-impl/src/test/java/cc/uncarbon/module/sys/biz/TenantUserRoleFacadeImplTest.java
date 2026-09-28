package cc.uncarbon.module.sys.biz;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserTenantRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserTenantRelationMapper;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.helper.UserRoleHelper;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cc.uncarbon.module.sys.model.request.TenantRoleCreateRequest;
import cc.uncarbon.module.sys.model.request.TenantRoleBindMenuRequest;
import cc.uncarbon.module.sys.model.request.TenantUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.TenantUserCreateRequest;
import cc.uncarbon.module.sys.model.response.TenantRoleCreateResult;
import cc.uncarbon.module.sys.model.response.TenantUserCreateResult;
import cc.uncarbon.module.sys.model.valueobj.SysUserBasicProfileDTO;
import cc.uncarbon.module.sys.model.valueobj.TenantUserBasicProfileDTO;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cc.uncarbon.module.sys.service.SysRoleService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cc.uncarbon.module.sys.service.SysUserService;
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

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * {@link TenantUserRoleFacadeImpl} 租户用户/角色门面分支测试（跨租户作用域包装）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantUserRoleFacadeImplTest {

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @Mock
    private SysRoleService sysRoleService;
    @Mock
    private SysUserService sysUserService;
    @Mock
    private SysUserRoleRelationService sysUserRoleRelationService;
    @Mock
    private SysRoleMenuRelationService sysRoleMenuRelationService;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysUserTenantRelationMapper sysUserTenantRelationMapper;
    @Mock
    private UserRoleHelper userRoleHelper;

    @InjectMocks
    private TenantUserRoleFacadeImpl facade;


    @Test
    void createTenantUserDoubleWritesRelationTable() {
        Mockito.when(sysUserService.createTenantUser(Mockito.any(TenantUserCreateRequest.class)))
                .thenReturn(new TenantUserCreateResult(66L, true));

        TenantUserCreateResult ret = facade.createTenantUser(new TenantUserCreateRequest()
                .setTenantId(10L).setTenantCode("t10").setTenantName("T")
                .setPin("tu").setPwdPlain("plain123").setEmail("a@b.c").setPhoneNo("13800000000")
                .setTenantAdmin(true));

        Assertions.assertEquals(66L, ret.getNewUserId());
        ArgumentCaptor<SysUserTenantRelationEntity> captor =
                ArgumentCaptor.forClass(SysUserTenantRelationEntity.class);
        Mockito.verify(sysUserTenantRelationMapper).insert(captor.capture());
        Assertions.assertEquals(10L, captor.getValue().getTenantId());
        Assertions.assertEquals(66L, captor.getValue().getUserId());
    }

    @Test
    void createTenantRoleDelegates() {
        Mockito.when(sysRoleService.createTenantRole(Mockito.any(TenantRoleCreateRequest.class)))
                .thenReturn(new TenantRoleCreateResult(91L, true));

        TenantRoleCreateResult ret = facade.createTenantRole(new TenantRoleCreateRequest()
                .setTenantId(10L).setTenantCode("t10").setTenantName("T")
                .setStatus(EnabledStatusEnum.ENABLED).setTenantAdmin(true));

        Assertions.assertEquals(91L, ret.getNewRoleId());
    }

    @Test
    void bindDelegates() {
        facade.bindTenantUserRoleRelation(new TenantUserBindRoleRequest()
                .setTenantId(10L).setTenantCode("t10").setUserId(66L).setRoleIds(List.of(91L)));
        Mockito.verify(sysUserRoleRelationService).cleanAndBindByUser(66L, List.of(91L));

        facade.bindTenantRoleMenuRelation(new TenantRoleBindMenuRequest()
                .setTenantId(10L).setTenantCode("t10").setRoleId(91L).setMenuIds(List.of(1L, 2L)));
        Mockito.verify(sysRoleMenuRelationService).cleanAndBind(91L, List.of(1L, 2L));
    }

    @Test
    void syncTenantRoleMenusTrimsToPackage() {
        Mockito.when(sysRoleMapper.selectList(Mockito.any())).thenReturn(List.of(
                new SysRoleEntity().setId(91L).setCode("OrgAdmin"),
                new SysRoleEntity().setId(92L).setCode("R92")));
        // 租户管理员直接对齐套餐；普通角色按交集裁剪
        Mockito.when(sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(92L)))
                .thenReturn(Set.of(1L, 3L));

        Set<Long> ret = facade.syncTenantRoleMenus(10L, Set.of(1L, 2L));

        Assertions.assertEquals(Set.of(91L, 92L), ret);
        Mockito.verify(sysRoleMenuRelationService).cleanAndBind(91L, Set.of(1L, 2L));
        Mockito.verify(sysRoleMenuRelationService).cleanAndBind(92L, Set.of(1L));
    }

    @Test
    void syncTenantRoleMenusEmptyPackageClearsAll() {
        Mockito.when(sysRoleMapper.selectList(Mockito.any())).thenReturn(List.of(
                new SysRoleEntity().setId(91L).setCode("OrgAdmin")));

        facade.syncTenantRoleMenus(10L, Set.of());

        Mockito.verify(sysRoleMenuRelationService).cleanAndBind(91L, Set.of());
    }

    @Test
    void getTenantUserBasicProfileDelegates() {
        Mockito.when(sysUserService.getNonnullBasicProfileById(66L)).thenReturn(
                new SysUserBasicProfileDTO().setPin("tu").setNickname("n"));

        TenantUserBasicProfileDTO ret = facade.getTenantUserBasicProfile(10L, 66L);

        Assertions.assertEquals("tu", ret.getPin());
    }

    @Test
    void listUserIdsByTenantExcludesSuperAdmin() {
        Mockito.when(sysUserMapper.selectList(Mockito.any())).thenReturn(List.of(
                new SysUserEntity().setId(1L),
                new SysUserEntity().setId(0L),   // 超管排除
                new SysUserEntity().setId(2L)));

        Assertions.assertEquals(List.of(1L, 2L), facade.listUserIdsByTenant(10L, null));
    }

    @Test
    void listEnabledTenantIdsByUserBranches() {
        Assertions.assertEquals(List.of(), facade.listEnabledTenantIdsByUser(null));

        Mockito.when(sysUserTenantRelationMapper.selectList(Mockito.any())).thenReturn(List.of(
                SysUserTenantRelationEntity.of(7L, 66L),
                SysUserTenantRelationEntity.of(8L, 66L)));

        Assertions.assertEquals(List.of(7L, 8L), facade.listEnabledTenantIdsByUser(66L));
    }

    @Test
    void rememberActiveTenantGuardsAndWrites() {
        Mockito.when(userRoleHelper.getSpecifiedUserRole(66L)).thenReturn(new UserRoleScope(List.of(), List.of()));
        facade.rememberActiveTenant(null, 5L);
        facade.rememberActiveTenant(66L, null);
        facade.rememberActiveTenant(0L, 5L);   // 超管归属平台自营域，不落投影列
        Mockito.verify(sysUserMapper, Mockito.never()).update(Mockito.any(), Mockito.any());

        facade.rememberActiveTenant(66L, 5L);
        Mockito.verify(sysUserMapper).update(Mockito.any(), Mockito.any());
    }

    @Test
    void isSuperAdminBranches() {
        Assertions.assertFalse(facade.isSuperAdmin(null));
        Assertions.assertTrue(facade.isSuperAdmin(0L));

        // 无归属租户 → 忽略租户态解析
        Mockito.when(sysUserMapper.selectById(66L)).thenReturn(new SysUserEntity().setId(66L));
        Mockito.when(userRoleHelper.getSpecifiedUserRole(66L)).thenReturn(normalScope(List.of(5L)));
        Assertions.assertFalse(facade.isSuperAdmin(66L));

        Mockito.when(userRoleHelper.getSpecifiedUserRole(66L)).thenReturn(UserRoleScope.mockSuperAdmin());
        Assertions.assertTrue(facade.isSuperAdmin(66L));

        // 有归属租户 → 在归属租户作用域内解析
        Mockito.when(sysUserMapper.selectById(77L)).thenReturn(tenantUser(77L, 10L));
        Mockito.when(userRoleHelper.getSpecifiedUserRole(77L)).thenReturn(UserRoleScope.mockSuperAdmin());
        Assertions.assertTrue(facade.isSuperAdmin(77L));
    }

    @Test
    void getUserHomeTenantIdBranches() {
        Assertions.assertNull(facade.getUserHomeTenantId(null));

        Mockito.when(sysUserMapper.selectById(66L)).thenReturn(null);
        Assertions.assertNull(facade.getUserHomeTenantId(66L));

        Mockito.when(sysUserMapper.selectById(66L)).thenReturn(tenantUser(66L, 9L));
        Assertions.assertEquals(9L, facade.getUserHomeTenantId(66L));
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private SysUserEntity tenantUser(Long id, Long tenantId) {
        var ret = new SysUserEntity().setId(id);
        ret.setTenantId(tenantId);
        return ret;
    }

    private UserRoleScope normalScope(List<Long> roleIds) {
        return new UserRoleScope(roleIds, roleIds.stream()
                .map(id -> new SysRoleEntity().setId(id).setCode("R" + id)).toList());
    }
}
