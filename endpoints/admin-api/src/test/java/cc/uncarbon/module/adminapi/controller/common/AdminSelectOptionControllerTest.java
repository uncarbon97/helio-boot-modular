package cc.uncarbon.module.adminapi.controller.common;

import cc.uncarbon.module.adminapi.model.valueobj.AdminSelectOptionItemVO;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.file.model.valueobj.FileStorageDTO;
import cc.uncarbon.module.file.service.FileStorageDataService;
import cc.uncarbon.module.sys.enums.DictStatusEnum;
import cc.uncarbon.module.sys.model.valueobj.SysDeptDTO;
import cc.uncarbon.module.sys.model.valueobj.SysDictItemDTO;
import cc.uncarbon.module.sys.model.valueobj.SysRoleDTO;
import cc.uncarbon.module.sys.service.SysDeptService;
import cc.uncarbon.module.sys.service.SysDictService;
import cc.uncarbon.module.sys.service.SysRoleService;
import cc.uncarbon.module.tenant.model.valueobj.TenantPackageDTO;
import cc.uncarbon.module.tenant.service.TenantPackageService;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import cn.dev33.satoken.stp.StpInterface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

/**
 * {@link AdminSelectOptionController} 下拉框数据源接口薄测（纯委托 + 权限分支）
 * <p>
 * dept 接口依赖 sa-token 权限判断，通过 {@link SaTokenContextMockUtil} Mock 上下文模拟
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminSelectOptionControllerTest {

    private static final Long USER_ID = 1L;

    @Mock
    private SysDictService sysDictService;
    @Mock
    private SysRoleService sysRoleService;
    @Mock
    private SysDeptService sysDeptService;
    @Mock
    private FileStorageDataService fileStorageDataService;
    @Mock
    private TenantPackageService tenantPackageService;

    private AdminSelectOptionController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminSelectOptionController(
                sysDictService, sysRoleService, sysDeptService, fileStorageDataService, tenantPackageService);
    }

    @AfterEach
    void tearDown() {
        SaTokenContextMockUtil.clearContext();
        SaManager.setStpInterface(null);
        StpKit.ADMIN.logout(USER_ID);
    }

    @Test
    void dictDelegatesWithEnabledAndDeprecatedStatuses() {
        Mockito.when(sysDictService.listItemsByCategory("gender", List.of(DictStatusEnum.ENABLED, DictStatusEnum.DEPRECATED)))
                .thenReturn(List.of(new SysDictItemDTO().setValue("M").setLabel("男").setCode("G_M")));

        var ret = controller.dict("gender");

        Assertions.assertEquals(1, ret.getData().size());
        AdminSelectOptionItemVO item = ret.getData().get(0);
        Assertions.assertEquals("M", item.getValue());
        Assertions.assertEquals("男", item.getLabel());
        Assertions.assertEquals("G_M", item.getDictItemCode());
    }

    @Test
    void roleDelegatesToListSelectOption() {
        Mockito.when(sysRoleService.adminListSelectOption())
                .thenReturn(List.of(new SysRoleDTO().setId(5L).setName("管理员")));

        var ret = controller.role();

        Assertions.assertEquals(5L, ret.getData().get(0).getId());
        Assertions.assertEquals("管理员", ret.getData().get(0).getName());
    }

    @Test
    void deptSeesSubtreeOnlyWithoutBindDeptPermission() {
        // 未登录（Mock 上下文无 token）→ hasPermission 捕获未登录异常返回 false → 仅本部门及以下
        Mockito.when(sysDeptService.adminListSelectOption(true))
                .thenReturn(List.of(new SysDeptDTO().setId(2L).setName("研发组").setParentId(1L)));

        SaTokenContextMockUtil.setMockContext(() -> {
            var ret = controller.dept();

            Mockito.verify(sysDeptService).adminListSelectOption(true);
            Assertions.assertEquals(2L, ret.getData().get(0).getId());
            Assertions.assertEquals(1L, ret.getData().get(0).getParentId());
        });
    }

    @Test
    void deptSeesAllDepartmentsWithBindDeptPermission() {
        // 已登录 + 拥有 sys:user:bind-dept 权限 → 可见所有部门
        Mockito.when(sysDeptService.adminListSelectOption(false))
                .thenReturn(List.of(new SysDeptDTO().setId(1L).setName("总经办")));
        SaManager.setStpInterface(new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                return List.of("sys:user:bind-dept");
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                return List.of();
            }
        });

        SaTokenContextMockUtil.setMockContext(() -> {
            StpKit.ADMIN.login(USER_ID);

            var ret = controller.dept();

            Mockito.verify(sysDeptService).adminListSelectOption(false);
            Assertions.assertEquals("总经办", ret.getData().get(0).getName());
        });
    }

    @Test
    void fileStorageDelegatesAndMapsStorageCode() {
        Mockito.when(fileStorageDataService.adminListSelectOption())
                .thenReturn(List.of(new FileStorageDTO().setId(3L).setName("主存储").setCode("MINIO")));

        var ret = controller.fileStorage();

        Assertions.assertEquals(3L, ret.getData().get(0).getId());
        Assertions.assertEquals("MINIO", ret.getData().get(0).getStorageCode());
    }

    @Test
    void tenantPackageDelegatesToListSelectOption() {
        Mockito.when(tenantPackageService.adminListSelectOption())
                .thenReturn(List.of(new TenantPackageDTO().setId(8L).setName("旗舰版")));

        var ret = controller.tenantPackage();

        Assertions.assertEquals(8L, ret.getData().get(0).getId());
        Assertions.assertEquals("旗舰版", ret.getData().get(0).getName());
    }
}
