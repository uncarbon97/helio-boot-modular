package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysDeptEntity;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.mapper.SysDeptMapper;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.helper.UserRoleHelper;
import cc.uncarbon.module.sys.model.internal.UserDeptScope;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cc.uncarbon.module.sys.model.request.AdminSysDeptUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysDeptDTO;
import cc.uncarbon.module.sys.service.SysUserDeptRelationService;
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
import java.util.stream.Collectors;

/**
 * {@link SysDeptServiceImpl} 部门树校验分支测试（成环防护、级联停启用、删除约束）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysDeptServiceImplTest {

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @Mock
    private SysDeptMapper sysDeptMapper;
    @Mock
    private SysUserDeptRelationService sysUserDeptRelationService;
    @Mock
    private UserRoleHelper userRoleHelper;

    @InjectMocks
    private SysDeptServiceImpl service;


    /*
    ----------------------------------------------------------------
                        新增/修改
    ----------------------------------------------------------------
     */

    @Test
    void adminCreateDuplicateNameUnderSameParentRejected() {
        Mockito.when(sysDeptMapper.exists(Mockito.any())).thenReturn(true);

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminCreate(upsert(null, "研发部", null)));
        Assertions.assertEquals(SysErrorCodeEnum.A01040, ex.getErrorCode());
    }

    @Test
    void adminCreateMissingParentRejected() {
        Mockito.when(sysDeptMapper.exists(Mockito.any())).thenReturn(false);
        Mockito.when(sysDeptMapper.selectById(9L)).thenReturn(null);

        Assertions.assertThrows(NoRecordException.class,
                () -> service.adminCreate(upsert(null, "研发部", 9L)));
    }

    @Test
    void adminCreateDisabledParentRejected() {
        Mockito.when(sysDeptMapper.exists(Mockito.any())).thenReturn(false);
        Mockito.when(sysDeptMapper.selectById(9L)).thenReturn(
                new SysDeptEntity().setId(9L).setStatus(EnabledStatusEnum.DISABLED));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminCreate(upsert(null, "研发部", 9L)));
        Assertions.assertEquals(SysErrorCodeEnum.A01037, ex.getErrorCode());
    }

    @Test
    void adminCreateDefaultsParentIdAndStatus() {
        Mockito.when(sysDeptMapper.exists(Mockito.any())).thenReturn(false);
        Mockito.when(sysDeptMapper.insert(Mockito.any(SysDeptEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysDeptEntity.class).setId(5L);
            return 1;
        });

        Long id = service.adminCreate(upsert(null, "研发部", null));

        Assertions.assertEquals(5L, id);
        ArgumentCaptor<SysDeptEntity> captor = ArgumentCaptor.forClass(SysDeptEntity.class);
        Mockito.verify(sysDeptMapper).insert(captor.capture());
        Assertions.assertEquals(SysConstant.ROOT_PARENT_ID, captor.getValue().getParentId());
        Assertions.assertEquals(EnabledStatusEnum.ENABLED, captor.getValue().getStatus());
    }

    @Test
    void adminUpdateMissingRejected() {
        Mockito.when(sysDeptMapper.exists(Mockito.any())).thenReturn(false);

        Assertions.assertThrows(NoRecordException.class,
                () -> service.adminUpdate(upsert(1L, "研发部", null)));
    }

    @Test
    void adminUpdateParentCannotBeSelf() {
        Mockito.when(sysDeptMapper.exists(Mockito.any())).thenReturn(false);

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminUpdate(upsert(1L, "研发部", 1L)));
        Assertions.assertEquals(SysErrorCodeEnum.A01039, ex.getErrorCode());
    }

    @Test
    void adminUpdateParentCannotBeInferior() {
        // 1 的上级指向 3，3 的上级回到 1 → 成环
        Mockito.when(sysDeptMapper.exists(Mockito.any())).thenReturn(false);
        Mockito.when(sysDeptMapper.selectById(3L)).thenReturn(dept(3L, 1L));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminUpdate(upsert(1L, "研发部", 3L)));
        Assertions.assertEquals(SysErrorCodeEnum.A01039, ex.getErrorCode());
    }

    @Test
    void adminUpdateCycleGuardRejectsDirtyData() {
        // 构造超长父链（200→199→…→1→0）触发 guard > 100 保护：被改节点不在链上
        Mockito.when(sysDeptMapper.exists(Mockito.any())).thenReturn(false);
        Mockito.when(sysDeptMapper.selectById(Mockito.anyLong())).thenAnswer(inv -> {
            long n = inv.getArgument(0);
            return dept(n, n <= 1 ? 0L : n - 1);
        });

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminUpdate(upsert(999L, "研发部", 200L)));
        Assertions.assertEquals(SysErrorCodeEnum.A01039, ex.getErrorCode());
    }

    @Test
    void adminUpdateRootParentSkipsChainWalk() {
        Mockito.when(sysDeptMapper.exists(Mockito.any())).thenReturn(false);
        Mockito.when(sysDeptMapper.updateById(Mockito.any(SysDeptEntity.class))).thenReturn(1);

        service.adminUpdate(upsert(1L, "研发部", 0L));

        Mockito.verify(sysDeptMapper).updateById(Mockito.any(SysDeptEntity.class));
        Mockito.verify(sysDeptMapper, Mockito.never()).selectById(Mockito.anyLong());
    }


    /*
    ----------------------------------------------------------------
                        删除/状态
    ----------------------------------------------------------------
     */

    @Test
    void adminDeleteWithChildrenRejected() {
        Mockito.when(sysDeptMapper.exists(Mockito.any())).thenReturn(true);

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminDelete(List.of(5L)));
        Assertions.assertEquals(SysErrorCodeEnum.A01038, ex.getErrorCode());
    }

    @Test
    void adminDeleteWithBoundUsersRejected() {
        Mockito.when(sysDeptMapper.exists(Mockito.any())).thenReturn(false);
        Mockito.when(sysUserDeptRelationService.listUserIdsByDepts(List.of(5L))).thenReturn(Set.of(3L));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminDelete(List.of(5L)));
        Assertions.assertEquals(SysErrorCodeEnum.A01038, ex.getErrorCode());
    }

    @Test
    void adminDeleteUnbindsThenDeletes() {
        Mockito.when(sysDeptMapper.exists(Mockito.any())).thenReturn(false);
        Mockito.when(sysUserDeptRelationService.listUserIdsByDepts(List.of(5L))).thenReturn(Set.of());

        service.adminDelete(List.of(5L));

        Mockito.verify(sysUserDeptRelationService).cleanAllBindings(5L);
        Mockito.verify(sysDeptMapper).deleteByIds(List.of(5L));
    }

    @Test
    void adminSetStatusIdempotentWhenUnchanged() {
        Mockito.when(sysDeptMapper.selectById(5L))
                .thenReturn(dept(5L, 0L, EnabledStatusEnum.ENABLED));

        service.adminSetStatus(status(5L, EnabledStatusEnum.ENABLED));

        Mockito.verify(sysDeptMapper, Mockito.never()).updateById(Mockito.any(SysDeptEntity.class));
    }

    @Test
    void adminSetStatusDisableWithEnabledChildrenRejected() {
        Mockito.when(sysDeptMapper.selectById(5L))
                .thenReturn(dept(5L, 0L, EnabledStatusEnum.ENABLED));
        Mockito.when(sysDeptMapper.exists(Mockito.any())).thenReturn(true);

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminSetStatus(status(5L, EnabledStatusEnum.DISABLED)));
        Assertions.assertEquals(SysErrorCodeEnum.A01036, ex.getErrorCode());
    }

    @Test
    void adminSetStatusEnableWithDisabledParentRejected() {
        Mockito.when(sysDeptMapper.selectById(6L))
                .thenReturn(dept(6L, 5L, EnabledStatusEnum.DISABLED));
        Mockito.when(sysDeptMapper.selectById(5L))
                .thenReturn(dept(5L, 0L, EnabledStatusEnum.DISABLED));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminSetStatus(status(6L, EnabledStatusEnum.ENABLED)));
        Assertions.assertEquals(SysErrorCodeEnum.A01037, ex.getErrorCode());
    }

    @Test
    void adminSetStatusEnableWithMissingParentRejected() {
        Mockito.when(sysDeptMapper.selectById(6L))
                .thenReturn(dept(6L, 5L, EnabledStatusEnum.DISABLED));
        Mockito.when(sysDeptMapper.selectById(5L)).thenReturn(null);

        Assertions.assertThrows(BusinessException.class,
                () -> service.adminSetStatus(status(6L, EnabledStatusEnum.ENABLED)));
    }

    @Test
    void adminSetStatusDisableLeafProceeds() {
        Mockito.when(sysDeptMapper.selectById(6L))
                .thenReturn(dept(6L, 5L, EnabledStatusEnum.ENABLED));
        Mockito.when(sysDeptMapper.exists(Mockito.any())).thenReturn(false);

        service.adminSetStatus(status(6L, EnabledStatusEnum.DISABLED));

        ArgumentCaptor<SysDeptEntity> captor = ArgumentCaptor.forClass(SysDeptEntity.class);
        Mockito.verify(sysDeptMapper).updateById(captor.capture());
        Assertions.assertEquals(EnabledStatusEnum.DISABLED, captor.getValue().getStatus());
    }


    /*
    ----------------------------------------------------------------
                        可见范围/转换
    ----------------------------------------------------------------
     */

    @Test
    void adminListSelectOptionNonAdminSeesSubtreeOnly() {
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(normalScope(List.of(5L)));
        Mockito.when(sysUserDeptRelationService.listDeptIdsByUser(7L)).thenReturn(List.of(5L));
        Mockito.when(sysDeptMapper.selectByIds(List.of(5L))).thenReturn(List.of(dept(5L, 0L)));
        Mockito.when(sysDeptMapper.selectSortedList()).thenReturn(List.of(
                dept(5L, 0L), dept(6L, 5L), dept(7L, 0L)));

        withUser(7L, () -> {
            var ret = service.adminListSelectOption(true);

            Assertions.assertEquals(Set.of(5L, 6L),
                    ret.stream().map(SysDeptDTO::getId)
                            .collect(Collectors.toSet()));
        });
    }

    @Test
    void adminListSelectOptionAdminSeesAllEnabled() {
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(UserRoleScope.mockSuperAdmin());
        Mockito.when(sysDeptMapper.selectEnabledSortedList()).thenReturn(List.of(
                dept(5L, 0L), dept(6L, 5L)));

        withUser(0L, () -> {
            var ret = service.adminListSelectOption(true);

            Assertions.assertEquals(2, ret.size());
            // 根部门 parentId 0 → null
            Assertions.assertNull(ret.get(0).getParentId());
            Assertions.assertEquals(5L, ret.get(1).getParentId());
        });
    }

    @Test
    void getSpecifiedUserDeptSurvivesParentCycle() {
        Mockito.when(sysUserDeptRelationService.listDeptIdsByUser(7L)).thenReturn(List.of(5L));
        Mockito.when(sysDeptMapper.selectByIds(List.of(5L)))
                .thenReturn(List.of(dept(5L, 6L), dept(6L, 5L)));
        Mockito.when(sysDeptMapper.selectSortedList()).thenReturn(List.of(dept(5L, 6L), dept(6L, 5L)));

        UserDeptScope scope = service.getSpecifiedUserDept(7L, true);

        // 成环防护：visited 集合保证终止
        Assertions.assertTrue(scope.getVisibleDeptIds().contains(5L));
        Assertions.assertTrue(scope.getVisibleDeptIds().contains(6L));
    }

    @Test
    void convertBranches() {
        Assertions.assertNull(service.getById(null));

        Mockito.when(sysDeptMapper.selectById(5L)).thenReturn(dept(5L, 0L));
        Assertions.assertNull(service.getById(5L).getParentId());

        Mockito.when(sysDeptMapper.selectSortedList()).thenReturn(List.of(dept(5L, 0L)));
        Assertions.assertEquals(1, service.adminList().size());
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private static void withUser(Long userId, Runnable op) {
        ScopedValue.where(UserContextHolder.scoped(), new SimpleUserContext().setUserId(userId)).run(op);
    }

    private AdminSysDeptUpsertRequest upsert(Long id, String name, Long parentId) {
        return new AdminSysDeptUpsertRequest().setId(id).setName(name).setParentId(parentId).setSort(1);
    }

    private AdminSetStatusRequest<Long, EnabledStatusEnum> status(Long id, EnabledStatusEnum newStatus) {
        return new AdminSetStatusRequest<Long, EnabledStatusEnum>().setId(id).setNewStatus(newStatus);
    }

    private SysDeptEntity dept(Long id, Long parentId) {
        return dept(id, parentId, EnabledStatusEnum.ENABLED);
    }

    private SysDeptEntity dept(Long id, Long parentId, EnabledStatusEnum status) {
        return new SysDeptEntity().setId(id).setParentId(parentId).setStatus(status);
    }

    private UserRoleScope normalScope(List<Long> roleIds) {
        return new UserRoleScope(roleIds, roleIds.stream()
                .map(id -> new SysRoleEntity().setId(id).setCode("R" + id)).toList());
    }
}
