package cc.uncarbon.module.adminapi.controller.tenant;

import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdsRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.tenant.model.query.AdminTenantPackageListQuery;
import cc.uncarbon.module.tenant.model.request.AdminBindPackageMenuRelationDTO;
import cc.uncarbon.module.tenant.model.request.AdminTenantPackageUpsertRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantPackageDTO;
import cc.uncarbon.module.tenant.service.TenantPackageService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "租户管理-租户套餐")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/tenant/package")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminTenantPackageController {

    private static final String PERMISSION_PREFIX = "TenantPackage:";

    private final TenantPackageService tenantPackageService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页查询")
    @PostMapping(value = "/list")
    public ApiResult<PageResult<TenantPackageDTO>> list(@RequestBody @Valid AdminTenantPackageListQuery query) {
        return ApiResult.success(tenantPackageService.adminList(query));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<TenantPackageDTO> detail(@RequestBody @Valid IdRequest<Long> request) {
        return ApiResult.success(tenantPackageService.getNonnullById(id));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminTenantPackageUpsertRequest request) {
        tenantPackageService.adminCreate(request);

        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminTenantPackageUpsertRequest request) {
        tenantPackageService.adminUpdate(request);

        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdsRequest<Long> request) {
        tenantPackageService.adminDelete(request.getIds());

        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + "bindMenus")
    @Operation(summary = "绑定套餐与菜单关联关系")
    @PostMapping(value = "/bind-menus")
    public ApiResult<Void> bindMenus(@RequestBody @Valid AdminBindPackageMenuRelationDTO request) {
        tenantPackageService.adminBindMenus(request);

        return ApiResult.success();
    }

}
