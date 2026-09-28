package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.module.sys.dal.entity.SysRoleMenuRelationEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserRoleRelationEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserDeptRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMenuRelationMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserRoleRelationMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserDeptRelationMapper;
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

import java.util.List;
import java.util.Set;

/**
 * 用户/角色/部门 关联关系服务 增量绑定逻辑分支测试
 */
@ExtendWith(MockitoExtension.class)
class SysRelationServicesTest {

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @Mock
    private SysUserRoleRelationMapper userRoleMapper;
    @Mock
    private SysRoleMenuRelationMapper roleMenuMapper;
    @Mock
    private SysUserDeptRelationMapper userDeptMapper;

    @InjectMocks
    private SysUserRoleRelationServiceImpl userRoleService;
    @InjectMocks
    private SysRoleMenuRelationServiceImpl roleMenuService;
    @InjectMocks
    private SysUserDeptRelationServiceImpl userDeptService;


    @Test
    void userRoleCleanAndBindEmptyRolesDeletesAll() {
        userRoleService.cleanAndBindByUser(1L, null);

        Mockito.verify(userRoleMapper).delete(Mockito.any());
        Mockito.verify(userRoleMapper, Mockito.never()).insert(Mockito.anyList());
    }

    @Test
    void userRoleCleanAndBindAppendsOnlyMissing() {
        Mockito.when(userRoleMapper.selectList(Mockito.any()))
                .thenReturn(List.of(SysUserRoleRelationEntity.of(1L, 5L)))
                .thenReturn(List.of());

        userRoleService.cleanAndBindByUser(1L, List.of(5L, 6L, 6L));

        Mockito.verify(userRoleMapper).delete(Mockito.any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SysUserRoleRelationEntity>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(userRoleMapper).insert(captor.capture());
        // 输入去重 + 已存在排除，仅增量插入 6
        Assertions.assertEquals(1, captor.getValue().size());
        Assertions.assertEquals(6L, captor.getValue().get(0).getRoleId());
    }

    @Test
    void userRoleListDelegates() {
        Mockito.when(userRoleMapper.listRoleIdsByUser(9L)).thenReturn(List.of(1L, 2L));
        Mockito.when(userRoleMapper.listUserIdsByRoles(Mockito.anyCollection())).thenReturn(Set.of(9L));

        Assertions.assertEquals(List.of(1L, 2L), userRoleService.listRoleIdsByUser(9L));
        Assertions.assertEquals(Set.of(9L), userRoleService.listUserIdsByRole(2L));
        // roleId 为 null 时委托给空集合
        Assertions.assertEquals(Set.of(), userRoleService.listUserIdsByRole(null));
    }

    @Test
    void userRoleDeleteByRoleIdsSkipsEmpty() {
        userRoleService.deleteByRoleIds(List.of());

        Mockito.verifyNoInteractions(userRoleMapper);
    }


    @Test
    void roleMenuListMenuIdsAggregates() {
        Mockito.when(roleMenuMapper.selectList(Mockito.any()))
                .thenReturn(List.of(SysRoleMenuRelationEntity.of(1L, 10L), SysRoleMenuRelationEntity.of(1L, 11L)))
                .thenReturn(List.of(SysRoleMenuRelationEntity.of(2L, 20L)));

        Assertions.assertEquals(Set.of(10L, 11L, 20L),
                roleMenuService.listMenuIdsByRoles(List.of(1L, 2L)));
        Assertions.assertEquals(Set.of(), roleMenuService.listMenuIdsByRoles(null));
    }

    @Test
    void roleMenuListRoleIdsByMenus() {
        Mockito.when(roleMenuMapper.selectList(Mockito.any()))
                .thenReturn(List.of(SysRoleMenuRelationEntity.of(1L, 10L)));

        Assertions.assertEquals(Set.of(1L), roleMenuService.listRoleIdsByMenus(List.of(10L)));
        Assertions.assertEquals(Set.of(), roleMenuService.listRoleIdsByMenus(List.of()));
    }

    @Test
    void roleMenuCleanAndBindEmptyDeletes() {
        roleMenuService.cleanAndBind(1L, null);

        Mockito.verify(roleMenuMapper).delete(Mockito.any());
        Mockito.verify(roleMenuMapper, Mockito.never()).insert(Mockito.anyList());
    }

    @Test
    void roleMenuDeleteByMenuIdsAndRoleIdsGuardEmpty() {
        roleMenuService.deleteByMenuIds(null);
        roleMenuService.deleteByRoleIds(List.of());

        Mockito.verifyNoInteractions(roleMenuMapper);
    }


    @Test
    void userDeptListDeptIdsByUser() {
        Mockito.when(userDeptMapper.selectOne(Mockito.any()))
                .thenReturn(SysUserDeptRelationEntity.of(3L, 77L));

        Assertions.assertEquals(List.of(77L), userDeptService.listDeptIdsByUser(3L));
    }

    @Test
    void userDeptListDeptIdsByUserMissing() {
        Mockito.when(userDeptMapper.selectOne(Mockito.any())).thenReturn(null);

        Assertions.assertEquals(List.of(), userDeptService.listDeptIdsByUser(3L));
    }

    @Test
    void userDeptListUserIdsByDeptsGuardEmpty() {
        Assertions.assertEquals(Set.of(), userDeptService.listUserIdsByDepts(List.of()));
        Mockito.verifyNoInteractions(userDeptMapper);
    }

    @Test
    void userDeptCleanAndBindNullDeptOnlyUnbinds() {
        userDeptService.cleanAndBind(3L, null);

        Mockito.verify(userDeptMapper).delete(Mockito.any());
        Mockito.verify(userDeptMapper, Mockito.never()).insert(Mockito.any(SysUserDeptRelationEntity.class));
    }

    @Test
    void userDeptCleanAndBindRebinds() {
        userDeptService.cleanAndBind(3L, 88L);

        Mockito.verify(userDeptMapper).delete(Mockito.any());
        ArgumentCaptor<SysUserDeptRelationEntity> captor = ArgumentCaptor.forClass(SysUserDeptRelationEntity.class);
        Mockito.verify(userDeptMapper).insert(captor.capture());
        Assertions.assertEquals(3L, captor.getValue().getUserId());
        Assertions.assertEquals(88L, captor.getValue().getDeptId());
    }

    @Test
    void userDeptCleanAllBindings() {
        userDeptService.cleanAllBindings(88L);

        Mockito.verify(userDeptMapper).delete(Mockito.any());
    }
}
