package cc.uncarbon.module.tenant.service.impl;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.model.response.TenantRoleCreateResult;
import cc.uncarbon.module.sys.model.response.TenantUserCreateResult;
import cc.uncarbon.module.sys.model.valueobj.TenantUserBasicProfileDTO;
import cc.uncarbon.module.tenant.MybatisPlusTestSupport;
import cc.uncarbon.module.tenant.constant.TenantConstant;
import cc.uncarbon.module.tenant.dal.entity.TenantMetaEntity;
import cc.uncarbon.module.tenant.dal.mapper.TenantMetaMapper;
import cc.uncarbon.module.tenant.errorcode.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.model.query.AdminTenantMetaListQuery;
import cc.uncarbon.module.tenant.model.request.AdminTenantCreateRequest;
import cc.uncarbon.module.tenant.model.request.AdminTenantMetaUpdateRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaDTO;
import cc.uncarbon.module.tenant.model.valueobj.TenantPackageDTO;
import cc.uncarbon.module.tenant.service.TenantPackageService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;

/**
 * {@link TenantServiceImpl} 租户生命周期分支测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantServiceImplTest {

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @Mock
    private TenantMetaMapper tenantMetaMapper;
    @Mock
    private TenantPackageService tenantPackageService;
    @Mock
    private TenantUserRoleFacade tenantUserRoleFacade;

    @InjectMocks
    private TenantServiceImpl service;


    @Test
    void adminCreateDuplicateCodeRejected() {
        Mockito.when(tenantMetaMapper.selectOne(Mockito.any())).thenReturn(new TenantMetaEntity().setId(1L));

        var ex = Assertions.assertThrows(HasRepeatRecordException.class,
                () -> service.adminCreate(createRequest(null)));
        Assertions.assertEquals(TenantErrorCodeEnum.A03005, ex.getErrorCode());
    }

    @Test
    void adminCreateDisabledPackageRejected() {
        Mockito.when(tenantMetaMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(tenantPackageService.getNonnullById(3L)).thenReturn(
                new TenantPackageDTO().setId(3L).setStatus(EnabledStatusEnum.DISABLED));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminCreate(createRequest(3L)));
        Assertions.assertEquals(TenantErrorCodeEnum.A03002, ex.getErrorCode());
    }

    @Test
    void adminCreateInitializesRoleUserBindingAndAdminMark() {
        Mockito.when(tenantMetaMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(tenantMetaMapper.insert(Mockito.any(TenantMetaEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, TenantMetaEntity.class).setId(55L);
            return 1;
        });
        Mockito.when(tenantPackageService.getNonnullById(3L)).thenReturn(
                new TenantPackageDTO().setId(3L).setStatus(EnabledStatusEnum.ENABLED).setMenuIds(List.of(1L)));
        Mockito.when(tenantUserRoleFacade.createTenantRole(Mockito.any()))
                .thenReturn(new TenantRoleCreateResult(91L, true));
        Mockito.when(tenantUserRoleFacade.createTenantUser(Mockito.any()))
                .thenReturn(new TenantUserCreateResult(66L, true));

        Long id = service.adminCreate(createRequest(3L));

        Assertions.assertEquals(55L, id);
        Mockito.verify(tenantUserRoleFacade).bindTenantUserRoleRelation(Mockito.argThat(
                req -> req.getUserId() == 66L && req.getRoleIds().contains(91L)));
        Mockito.verify(tenantMetaMapper).updateAdminUserId(55L, 66L);
        // 有套餐时按套餐菜单绑定租户管理员角色
        Mockito.verify(tenantUserRoleFacade).bindTenantRoleMenuRelation(Mockito.argThat(
                req -> req.getRoleId() == 91L && req.getMenuIds().contains(1L)));
    }

    @Test
    void adminCreateWithoutPackageSkipsPackageChecks() {
        Mockito.when(tenantMetaMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(tenantMetaMapper.insert(Mockito.any(TenantMetaEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, TenantMetaEntity.class).setId(56L);
            return 1;
        });
        Mockito.when(tenantUserRoleFacade.createTenantRole(Mockito.any()))
                .thenReturn(new TenantRoleCreateResult(92L, true));
        Mockito.when(tenantUserRoleFacade.createTenantUser(Mockito.any()))
                .thenReturn(new TenantUserCreateResult(67L, true));

        Assertions.assertEquals(56L, service.adminCreate(createRequest(null)));
        Mockito.verify(tenantPackageService, Mockito.never()).getNonnullById(Mockito.anyLong());
    }

    @Test
    void adminUpdateMissingRejected() {
        Mockito.when(tenantMetaMapper.selectById(9L)).thenReturn(null);

        Assertions.assertThrows(NoRecordException.class, () -> service.adminUpdate(updateRequest(9L, 3L)));
    }

    @Test
    void adminUpdateSamePackageSkipsSync() {
        Mockito.when(tenantMetaMapper.selectById(55L)).thenReturn(metaEntity(55L, 3L));

        Mockito.when(tenantPackageService.getNonnullById(3L)).thenReturn(
                new TenantPackageDTO().setId(3L).setStatus(EnabledStatusEnum.ENABLED));

        Set<Long> ret = service.adminUpdate(updateRequest(55L, 3L));

        Assertions.assertEquals(Set.of(), ret);
        Mockito.verify(tenantUserRoleFacade, Mockito.never()).syncTenantRoleMenus(Mockito.anyLong(), Mockito.anyCollection());
    }

    @Test
    void adminUpdatePackageChangeTrimsRoleMenus() {
        Mockito.when(tenantMetaMapper.selectById(55L)).thenReturn(metaEntity(55L, 3L));
        Mockito.when(tenantPackageService.getNonnullById(4L)).thenReturn(
                new TenantPackageDTO().setId(4L).setStatus(EnabledStatusEnum.ENABLED).setMenuIds(List.of(1L, 2L)));
        Mockito.when(tenantUserRoleFacade.syncTenantRoleMenus(55L, List.of(1L, 2L))).thenReturn(Set.of(91L));

        Set<Long> ret = service.adminUpdate(updateRequest(55L, 4L));

        Assertions.assertEquals(Set.of(91L), ret);
    }

    @Test
    void adminUpdatePackageClearedClearsMenus() {
        Mockito.when(tenantMetaMapper.selectById(55L)).thenReturn(metaEntity(55L, 3L));
        Mockito.when(tenantUserRoleFacade.syncTenantRoleMenus(55L, Set.of())).thenReturn(Set.of());

        service.adminUpdate(updateRequest(55L, null));

        Mockito.verify(tenantUserRoleFacade).syncTenantRoleMenus(55L, Set.of());
    }

    @Test
    void adminSetStatusBranches() {
        // 缺失
        Mockito.when(tenantMetaMapper.selectById(9L)).thenReturn(null);
        Assertions.assertThrows(NoRecordException.class,
                () -> service.adminSetStatus(status(9L, EnabledStatusEnum.ENABLED)));

        // 平台自营域不可禁用
        Mockito.when(tenantMetaMapper.selectById(TenantConstant.PLATFORM_TENANT_ID))
                .thenReturn(metaEntity(0L, null));
        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminSetStatus(status(0L, EnabledStatusEnum.DISABLED)));
        Assertions.assertEquals(TenantErrorCodeEnum.A03013, ex.getErrorCode());

        // 禁用 → 返回租户用户ID
        Mockito.when(tenantMetaMapper.selectById(55L)).thenReturn(metaEntity(55L, null));
        Mockito.when(tenantUserRoleFacade.listUserIdsByTenant(55L, null)).thenReturn(List.of(66L));
        List<Long> disabledUserIds = service.adminSetStatus(status(55L, EnabledStatusEnum.DISABLED));
        Assertions.assertEquals(List.of(66L), disabledUserIds);

        // 启用 → 空集合
        Assertions.assertEquals(List.of(), service.adminSetStatus(status(55L, EnabledStatusEnum.ENABLED)));
    }

    @Test
    void adminDeleteGuards() {
        // 平台自营域不可删除
        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminDelete(List.of(TenantConstant.PLATFORM_TENANT_ID)));
        Assertions.assertEquals(TenantErrorCodeEnum.A03013, ex.getErrorCode());

        // 仍有用户不可删除
        Mockito.when(tenantUserRoleFacade.listUserIdsByTenant(55L, null)).thenReturn(List.of(66L));
        var ex2 = Assertions.assertThrows(BusinessException.class,
                () -> service.adminDelete(List.of(55L)));
        Assertions.assertEquals(TenantErrorCodeEnum.A03007, ex2.getErrorCode());

        // 正常删除
        Mockito.when(tenantUserRoleFacade.listUserIdsByTenant(55L, null)).thenReturn(List.of());
        service.adminDelete(List.of(55L));
        Mockito.verify(tenantMetaMapper).deleteByIds(List.of(55L));
    }

    @Test
    void getBranches() {
        Assertions.assertNull(service.getById(null, false));
        Assertions.assertNull(service.getByCode("  ", false));

        Mockito.when(tenantMetaMapper.selectById(55L)).thenReturn(metaEntity(55L, 3L));
        Assertions.assertEquals(55L, service.getById(55L, false).getId());

        // 填充管理员资料
        Mockito.when(tenantMetaMapper.selectByCode("t55")).thenReturn(
                metaEntity(55L, 3L).setAdminUserId(66L));
        Mockito.when(tenantUserRoleFacade.getTenantUserBasicProfile(55L, 66L)).thenReturn(
                new TenantUserBasicProfileDTO().setPin("tu"));
        TenantMetaDTO dto = service.getByCode("t55", true);
        Assertions.assertNotNull(dto.getAdminUserProfile());
        Assertions.assertEquals("tu", dto.getAdminUserProfile().getPin());
    }

    @Test
    void listEnabledDelegates() {
        Mockito.when(tenantMetaMapper.selectList(Mockito.any()))
                .thenReturn(List.of(metaEntity(55L, null)));

        Assertions.assertEquals(1, service.listEnabled().size());
    }

    @Test
    void adminListFillsTenantAdminProfile() {
        Mockito.when(tenantMetaMapper.selectPage(Mockito.any(), Mockito.any())).thenAnswer(inv -> {
            Page<TenantMetaEntity> page = inv.getArgument(0);
            // 一个有管理员用户，一个没有
            page.setRecords(List.of(
                    metaEntity(55L, null).setAdminUserId(66L),
                    metaEntity(56L, null)));
            page.setTotal(2);
            return page;
        });
        Mockito.when(tenantUserRoleFacade.getTenantUserBasicProfile(55L, 66L))
                .thenReturn(new TenantUserBasicProfileDTO().setPin("tu55"));

        var ret = service.adminList(new AdminTenantMetaListQuery()
                .setPageParam(new PageParam(1, 10)).setCode("t55").setName("租户").setStatus(EnabledStatusEnum.ENABLED));

        Assertions.assertEquals(2, ret.getTotal());
        Assertions.assertNotNull(ret.getRecords().get(0).getAdminUserProfile());
        Assertions.assertEquals("tu55", ret.getRecords().get(0).getAdminUserProfile().getPin());
        // 未设置管理员的租户不查资料
        Assertions.assertNull(ret.getRecords().get(1).getAdminUserProfile());
        Mockito.verify(tenantUserRoleFacade, Mockito.never())
                .getTenantUserBasicProfile(Mockito.eq(56L), Mockito.anyLong());
    }

    @Test
    void adminListEmptyPage() {
        Mockito.when(tenantMetaMapper.selectPage(Mockito.any(), Mockito.any())).thenAnswer(inv -> {
            Page<TenantMetaEntity> page = inv.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        var ret = service.adminList(new AdminTenantMetaListQuery().setPageParam(new PageParam(1, 10)));

        Assertions.assertEquals(0, ret.getTotal());
        Assertions.assertTrue(ret.getRecords().isEmpty());
    }

    @Test
    void lookupMissingBranches() {
        // 主键缺失 → NoRecordException
        Mockito.when(tenantMetaMapper.selectById(9L)).thenReturn(null);
        Assertions.assertThrows(NoRecordException.class, () -> service.getNonnullById(9L, true));

        // 编码查不到 → null（填充逻辑跳过）
        Mockito.when(tenantMetaMapper.selectByCode("missing")).thenReturn(null);
        Assertions.assertNull(service.getByCode("missing", true));

        // 命中但未设置管理员 → 不触发资料填充
        Mockito.when(tenantMetaMapper.selectById(55L)).thenReturn(metaEntity(55L, null));
        TenantMetaDTO dto = service.getById(55L, true);
        Assertions.assertEquals(55L, dto.getId());
        Assertions.assertNull(dto.getAdminUserProfile());
        Mockito.verify(tenantUserRoleFacade, Mockito.never())
                .getTenantUserBasicProfile(Mockito.anyLong(), Mockito.anyLong());
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    private AdminTenantCreateRequest createRequest(Long packageId) {
        return new AdminTenantCreateRequest()
                .setCode("t55").setName("租户五五")
                .setTenantAdminPin("tu55").setTenantAdminPwd("pass12345678")
                .setTenantAdminEmail("a@b.c").setTenantAdminPhoneNo("13800000000")
                .setPackageId(packageId);
    }

    private AdminTenantMetaUpdateRequest updateRequest(Long id, Long packageId) {
        return new AdminTenantMetaUpdateRequest()
                .setId(id).setName("改名").setPackageId(packageId);
    }

    private AdminSetStatusRequest<Long, EnabledStatusEnum> status(Long id, EnabledStatusEnum newStatus) {
        return new AdminSetStatusRequest<Long, EnabledStatusEnum>().setId(id).setNewStatus(newStatus);
    }

    private TenantMetaEntity metaEntity(Long id, Long packageId) {
        return new TenantMetaEntity().setId(id).setCode("t" + id).setName("T" + id)
                .setStatus(EnabledStatusEnum.ENABLED).setPackageId(packageId);
    }
}
