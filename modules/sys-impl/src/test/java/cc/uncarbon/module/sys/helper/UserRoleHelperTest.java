package cc.uncarbon.module.sys.helper;

import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserRoleRelationMapper;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cc.uncarbon.module.sys.MybatisPlusTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

/**
 * {@link UserRoleHelper} 角色/管理员判定分支测试
 */
@ExtendWith(MockitoExtension.class)
class UserRoleHelperTest {

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysUserRoleRelationMapper sysUserRoleRelationMapper;

    @InjectMocks
    private UserRoleHelper helper;


    @Test
    void superAdminUserIdShortCircuitsToMockScope() {
        UserRoleScope scope = helper.getSpecifiedUserRole(SysConstant.SUPER_ADMIN_USER_ID);

        Assertions.assertTrue(scope.isSuperAdmin());
        Assertions.assertFalse(scope.isTenantAdmin());
        Assertions.assertFalse(scope.isNotAnyAdmin());
        Assertions.assertEquals(List.of(SysConstant.SUPER_ADMIN_ROLE_ID), scope.getRelatedRoleIds());
        Mockito.verifyNoInteractions(sysRoleMapper, sysUserRoleRelationMapper);
    }

    @Test
    void userWithoutRelationsYieldsEmptyScope() {
        Mockito.when(sysUserRoleRelationMapper.listRoleIdsByUser(7L)).thenReturn(List.of());

        UserRoleScope scope = helper.getSpecifiedUserRole(7L);

        Assertions.assertFalse(scope.isSuperAdmin());
        Assertions.assertFalse(scope.isTenantAdmin());
        Assertions.assertTrue(scope.isNotAnyAdmin());
        Assertions.assertEquals(List.of(), scope.getRelatedRoleIds());
    }

    @Test
    void onlyEnabledRolesCounted() {
        Mockito.when(sysUserRoleRelationMapper.listRoleIdsByUser(7L)).thenReturn(List.of(5L, 6L));
        // 启用状态过滤在 wrapper 中，mock 侧直接返回过滤后的启用角色
        Mockito.when(sysRoleMapper.selectList(Mockito.any())).thenReturn(List.of(role(5L, "R5")));

        UserRoleScope scope = helper.getSpecifiedUserRole(7L);

        Assertions.assertEquals(List.of(5L), scope.getRelatedRoleIds());
    }

    @Test
    void tenantAdminDetectedByRoleCode() {
        Mockito.when(sysUserRoleRelationMapper.listRoleIdsByUser(7L)).thenReturn(List.of(5L));
        Mockito.when(sysRoleMapper.selectList(Mockito.any()))
                .thenReturn(List.of(role(5L, SysConstant.TENANT_ADMIN_ROLE_CODE)));

        UserRoleScope scope = helper.getSpecifiedUserRole(7L);

        Assertions.assertTrue(scope.isTenantAdmin());
        Assertions.assertFalse(scope.isSuperAdmin());
    }


    @Test
    void superAdminHidesNoRoles() {
        withUser(0L, () -> {
            Assertions.assertEquals(Set.of(), helper.listHiddenRoleIds());
            Mockito.verifyNoInteractions(sysRoleMapper);
        });
    }

    @Test
    void tenantAdminHidesOnlySuperAdminRole() {
        withUser(7L, () -> {
            Mockito.when(sysUserRoleRelationMapper.listRoleIdsByUser(7L)).thenReturn(List.of(5L));
            Mockito.when(sysRoleMapper.selectList(Mockito.any()))
                    .thenReturn(List.of(role(5L, SysConstant.TENANT_ADMIN_ROLE_CODE)));

            Assertions.assertEquals(Set.of(SysConstant.SUPER_ADMIN_ROLE_ID), helper.listHiddenRoleIds());
        });
    }

    @Test
    void normalUserHidesSuperAdminAndTenantAdminRoles() {
        withUser(7L, () -> {
            Mockito.when(sysUserRoleRelationMapper.listRoleIdsByUser(7L)).thenReturn(List.of(5L));
            Mockito.when(sysRoleMapper.selectList(Mockito.any()))
                    .thenReturn(List.of(role(5L, "R5")))
                    .thenReturn(List.of(role(9L, SysConstant.TENANT_ADMIN_ROLE_CODE)));

            Assertions.assertEquals(
                    Set.of(SysConstant.SUPER_ADMIN_ROLE_ID, 9L), helper.listHiddenRoleIds());
        });
    }

    @Test
    void listHiddenUserIdsDelegates() {
        withUser(7L, () -> {
            Mockito.when(sysUserRoleRelationMapper.listRoleIdsByUser(7L)).thenReturn(List.of(5L));
            Mockito.when(sysRoleMapper.selectList(Mockito.any())).thenReturn(List.of(role(5L, "R5")));
            Mockito.when(sysUserRoleRelationMapper.listUserIdsByRoles(Mockito.anyCollection()))
                    .thenReturn(Set.of(1L, 2L));

            Assertions.assertEquals(Set.of(1L, 2L), helper.listHiddenUserIds());
        });
    }

    @Test
    void getCurrentUserRoleResolvesSelf() {
        withUser(7L, () -> {
            Mockito.when(sysUserRoleRelationMapper.listRoleIdsByUser(7L)).thenReturn(List.of());

            Assertions.assertEquals(List.of(), helper.getCurrentUserRole().getRelatedRoleIds());
        });
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private static void withUser(Long userId, Runnable op) {
        ScopedValue.where(UserContextHolder.scoped(), new SimpleUserContext().setUserId(userId)).run(op);
    }

    private SysRoleEntity role(Long id, String code) {
        return new SysRoleEntity().setId(id).setCode(code);
    }
}
