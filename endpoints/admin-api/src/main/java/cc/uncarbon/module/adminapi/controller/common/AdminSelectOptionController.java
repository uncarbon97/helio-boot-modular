package cc.uncarbon.module.adminapi.controller.common;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.constant.AdminPermissionConstant;
import cc.uncarbon.module.adminapi.model.valueobj.AdminSelectOptionItemVO;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.commons.satoken.StpLoginType;
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
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 这里统一存放所有用于后台管理的下拉框数据源接口
 * 避免多人协作时，不知道原来是否已经有了，或者写在某个边边角角里，造成重复开发
 * <p>
 * {@code @SaCheckLogin(type = StpLoginType.ADMIN)} 表示只有登录后才能请求
 */
@Tag(name = "后台管理--下拉框数据源接口")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/select-option")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSelectOptionController {

    private final SysDictService sysDictService;
    private final SysRoleService sysRoleService;
    private final SysDeptService sysDeptService;
    private final FileStorageDataService fileStorageDataService;
    private final TenantPackageService tenantPackageService;


    @SaCheckLogin(type = StpLoginType.ADMIN)
    @Operation(summary = "字典数据")
    @PostMapping(value = "/dict/{categoryCode}")
    // SpringDoc 可能扫描有问题，不能用 @PathVariable 的 bare 写法，需要手动指定 value
    public ApiResult<List<AdminSelectOptionItemVO>> dict(@PathVariable(value = "categoryCode") String categoryCode) {
        return ApiResult.success(AdminSelectOptionItemVO.ofValueLabelBatch(
                sysDictService.listItemsByCategory(categoryCode, List.of(DictStatusEnum.ENABLED, DictStatusEnum.DEPRECATED)),
                SysDictItemDTO::getValue, SysDictItemDTO::getLabel,
                (source, target) -> target.setDictItemCode(source.getCode())));
    }

    @SaCheckLogin(type = StpLoginType.ADMIN)
    @Operation(summary = "系统角色下拉框数据")
    @PostMapping(value = "/sys/role")
    public ApiResult<List<AdminSelectOptionItemVO>> role() {
        return ApiResult.success(AdminSelectOptionItemVO.ofIdNameBatch(sysRoleService.adminListSelectOption(),
                SysRoleDTO::getId, SysRoleDTO::getName));
    }

    @SaCheckLogin(type = StpLoginType.ADMIN)
    @Operation(summary = "部门下拉框数据")
    @PostMapping(value = "/sys/dept")
    public ApiResult<List<AdminSelectOptionItemVO>> dept() {
        // true = 让高级 HR 等角色可以调整用户部门，那就需要 TA 可以看到所有部门
        // false = 只能看到本部门及以下
        boolean hasBindDeptPerm = StpKit.ADMIN.hasPermission(AdminPermissionConstant.BIND_DEPT);
        return ApiResult.success(AdminSelectOptionItemVO.ofIdNameParentBatch(sysDeptService.adminListSelectOption(!hasBindDeptPerm),
                SysDeptDTO::getId, SysDeptDTO::getName, SysDeptDTO::getParentId));
    }

    @SaCheckLogin(type = StpLoginType.ADMIN)
    @Operation(summary = "文件存储点下拉框数据")
    @PostMapping(value = "/file/storage")
    public ApiResult<List<AdminSelectOptionItemVO>> fileStorage() {
        return ApiResult.success(AdminSelectOptionItemVO.ofIdNameBatch(fileStorageDataService.adminListSelectOption(),
                FileStorageDTO::getId, FileStorageDTO::getName,
                (source, target) -> target.setStorageCode(source.getCode())));
    }

    @SaCheckLogin(type = StpLoginType.ADMIN)
    @Operation(summary = "租户套餐下拉框数据")
    @PostMapping(value = "/tenant/package")
    public ApiResult<List<AdminSelectOptionItemVO>> tenantPackage() {
        return ApiResult.success(AdminSelectOptionItemVO.ofIdNameBatch(tenantPackageService.adminListSelectOption(),
                TenantPackageDTO::getId, TenantPackageDTO::getName));
    }
}
