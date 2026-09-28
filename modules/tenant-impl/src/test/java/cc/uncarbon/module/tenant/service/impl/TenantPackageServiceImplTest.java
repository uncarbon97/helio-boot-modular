package cc.uncarbon.module.tenant.service.impl;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.sys.facade.SysMenuFacade;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.tenant.dal.entity.TenantMetaEntity;
import cc.uncarbon.module.tenant.dal.entity.TenantPackageEntity;
import cc.uncarbon.module.tenant.dal.mapper.TenantMetaMapper;
import cc.uncarbon.module.tenant.dal.mapper.TenantPackageMapper;
import cc.uncarbon.module.tenant.errorcode.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.model.request.AdminTenantPackageBindMenuRequest;
import cc.uncarbon.module.tenant.model.request.AdminTenantPackageUpsertRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantPackageBindMenuResult;
import cc.uncarbon.module.tenant.service.TenantPackageMenuRelationService;
import cc.uncarbon.module.tenant.MybatisPlusTestSupport;
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

/**
 * {@link TenantPackageServiceImpl} 套餐管理 + 套餐变更级联裁剪分支测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantPackageServiceImplTest {

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @Mock
    private TenantPackageMapper tenantPackageMapper;
    @Mock
    private TenantPackageMenuRelationService tenantPackageMenuRelationService;
    @Mock
    private TenantMetaMapper tenantMetaMapper;
    @Mock
    private TenantUserRoleFacade tenantUserRoleFacade;
    @Mock
    private SysMenuFacade sysMenuFacade;

    @InjectMocks
    private TenantPackageServiceImpl service;


    @Test
    void adminCreateDuplicateCodeRejected() {
        Mockito.when(tenantPackageMapper.selectOne(Mockito.any()))
                .thenReturn(new TenantPackageEntity().setId(1L));

        var ex = Assertions.assertThrows(HasRepeatRecordException.class,
                () -> service.adminCreate(upsert(null, "p1")));
        Assertions.assertEquals(TenantErrorCodeEnum.A03004, ex.getErrorCode());
    }

    @Test
    void adminCreateDefaultsDisabled() {
        Mockito.when(tenantPackageMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(tenantPackageMapper.insert(Mockito.any(TenantPackageEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, TenantPackageEntity.class).setId(3L);
            return 1;
        });

        Assertions.assertEquals(3L, service.adminCreate(upsert(null, "p1")));

        ArgumentCaptor<TenantPackageEntity> captor = ArgumentCaptor.forClass(TenantPackageEntity.class);
        Mockito.verify(tenantPackageMapper).insert(captor.capture());
        Assertions.assertEquals(EnabledStatusEnum.DISABLED, captor.getValue().getStatus());
    }

    @Test
    void adminUpdateMissingRejected() {
        Mockito.when(tenantPackageMapper.exists(Mockito.any())).thenReturn(false);
        Mockito.when(tenantPackageMapper.selectOne(Mockito.any())).thenReturn(null);

        Assertions.assertThrows(NoRecordException.class, () -> service.adminUpdate(upsert(9L, "p1")));
    }

    @Test
    void adminDeleteInUseRejected() {
        Mockito.when(tenantMetaMapper.exists(Mockito.any())).thenReturn(true);

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminDelete(List.of(3L)));
        Assertions.assertEquals(TenantErrorCodeEnum.A03008, ex.getErrorCode());
    }

    @Test
    void adminDeleteHappyPath() {
        Mockito.when(tenantMetaMapper.exists(Mockito.any())).thenReturn(false);

        service.adminDelete(List.of(3L));

        Mockito.verify(tenantPackageMapper).deleteByIds(List.of(3L));
    }

    @Test
    void adminSetStatusDisableInUseRejected() {
        Mockito.when(tenantPackageMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(tenantMetaMapper.exists(Mockito.any())).thenReturn(true);

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminSetStatus(status(3L, EnabledStatusEnum.DISABLED)));
        Assertions.assertEquals(TenantErrorCodeEnum.A03003, ex.getErrorCode());
    }

    @Test
    void adminSetStatusHappyPath() {
        Mockito.when(tenantPackageMapper.exists(Mockito.any())).thenReturn(true);

        service.adminSetStatus(status(3L, EnabledStatusEnum.ENABLED));

        ArgumentCaptor<TenantPackageEntity> captor = ArgumentCaptor.forClass(TenantPackageEntity.class);
        Mockito.verify(tenantPackageMapper).updateById(captor.capture());
        Assertions.assertEquals(EnabledStatusEnum.ENABLED, captor.getValue().getStatus());
    }

    @Test
    void adminBindMenuSuperAdminOnlyRejected() {
        Mockito.when(sysMenuFacade.listSuperAdminOnlySubtreeMenuIds()).thenReturn(Set.of(42L));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminBindMenu(bindMenu(3L, Set.of(42L))));
        Assertions.assertEquals(TenantErrorCodeEnum.A03009, ex.getErrorCode());
    }

    @Test
    void adminBindMenuSyncsAffectedTenants() {
        Mockito.when(sysMenuFacade.listSuperAdminOnlySubtreeMenuIds()).thenReturn(Set.of());
        Mockito.when(tenantMetaMapper.selectList(Mockito.any()))
                .thenReturn(List.of(new TenantMetaEntity().setId(55L).setPackageId(3L)));
        Mockito.when(tenantUserRoleFacade.syncTenantRoleMenus(55L, Set.of(1L, 2L))).thenReturn(Set.of(91L));

        TenantPackageBindMenuResult ret = service.adminBindMenu(bindMenu(3L, Set.of(1L, 2L)));

        Mockito.verify(tenantPackageMenuRelationService).cleanAndBind(3L, Set.of(1L, 2L));
        Assertions.assertEquals(3L, ret.getPackageId());
        Assertions.assertEquals(Set.of(91L), ret.getTenantRoleIdsMap().get(55L));
    }

    @Test
    void adminBindMenuEmptyMenuIdsAllowed() {
        Mockito.when(sysMenuFacade.listSuperAdminOnlySubtreeMenuIds()).thenReturn(Set.of());
        Mockito.when(tenantMetaMapper.selectList(Mockito.any())).thenReturn(List.of());

        TenantPackageBindMenuResult ret = service.adminBindMenu(bindMenu(3L, null));

        Mockito.verify(tenantPackageMenuRelationService).cleanAndBind(3L, null);
        Assertions.assertTrue(ret.getTenantRoleIdsMap().isEmpty());
    }

    @Test
    void getByIdBranches() {
        Assertions.assertNull(service.getById(null));

        Mockito.when(tenantPackageMapper.selectById(3L)).thenReturn(null);
        Assertions.assertThrows(NoRecordException.class, () -> service.getNonnullById(3L));

        Mockito.when(tenantPackageMapper.selectById(3L)).thenReturn(
                new TenantPackageEntity().setId(3L).setCode("p1").setName("套餐一"));
        Mockito.when(tenantPackageMenuRelationService.listMenuIdsByPackage(3L)).thenReturn(List.of(1L));

        Assertions.assertEquals(List.of(1L), service.getById(3L).getMenuIds());
    }

    @Test
    void adminListSelectOptionDelegates() {
        Mockito.when(tenantPackageMapper.selectList(Mockito.any())).thenReturn(List.of(
                new TenantPackageEntity().setId(3L).setCode("p1").setName("套餐一")));

        var ret = service.adminListSelectOption();

        Assertions.assertEquals(1, ret.size());
        Assertions.assertEquals("p1", ret.get(0).getCode());
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private AdminTenantPackageUpsertRequest upsert(Long id, String code) {
        return new AdminTenantPackageUpsertRequest().setId(id).setCode(code).setName("套餐").setDescription("d");
    }

    private AdminSetStatusRequest<Long, EnabledStatusEnum> status(Long id, EnabledStatusEnum newStatus) {
        return new AdminSetStatusRequest<Long, EnabledStatusEnum>().setId(id).setNewStatus(newStatus);
    }

    private AdminTenantPackageBindMenuRequest bindMenu(Long id, Set<Long> menuIds) {
        return new AdminTenantPackageBindMenuRequest().setId(id).setMenuIds(menuIds);
    }
}
