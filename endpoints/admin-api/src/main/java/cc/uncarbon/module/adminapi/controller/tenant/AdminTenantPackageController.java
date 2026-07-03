package cc.uncarbon.module.adminapi.controller.tenant;

import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.bizlog.context.LogRecordContext;
import cc.uncarbon.framework.helium.bizlog.service.impl.DiffParseFunction;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.annotation.SysOperateLog;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.tenant.model.query.AdminTenantPackageListQuery;
import cc.uncarbon.module.tenant.model.request.AdminTenantPackageBindMenuRequest;
import cc.uncarbon.module.tenant.model.request.AdminTenantPackageUpsertRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantPackageDTO;
import cc.uncarbon.module.tenant.service.TenantPackageService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Set;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "租户管理-" + AdminTenantPackageController.BIZ_TYPE)
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/tenant/package")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminTenantPackageController {

    private static final String PERMISSION_PREFIX = "TenantPackage:";
    static final String BIZ_TYPE = "租户套餐";

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
        return ApiResult.success(tenantPackageService.getNonnullById(request.getId()));
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "新增租户套餐",
            success = "新增租户套餐：{{#request.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminTenantPackageUpsertRequest request) {
        tenantPackageService.adminCreate(request);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "修改租户套餐",
            bizNo = "{{#request.id}}", success = "被操作租户套餐：{{#old.code}}|{{#old.name}}：{_DIFF{#request}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminTenantPackageUpsertRequest request) {
        var old = tenantPackageService.getNonnullById(request.getId());
        tenantPackageService.adminUpdate(request);
        // 用于操作日志；用于 Diff 比较的两个对象，类型必须一致
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtil.toBean(old, request.getClass()));
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "删除租户套餐",
            bizNo = "{{#request.id}}", success = "被操作租户套餐：{{#old.code}}|{{#old.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdRequest<Long> request) {
        var old = tenantPackageService.getNonnullById(request.getId());
        tenantPackageService.adminDelete(Set.of(request.getId()));
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "绑定租户套餐菜单",
            bizNo = "{{#request.id}}", success = "被操作租户套餐：{{#old.code}}|{{#old.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + "bindMenu")
    @Operation(summary = "绑定租户套餐菜单")
    @PostMapping(value = "/bind-menu")
    public ApiResult<Void> bindMenu(@RequestBody @Valid AdminTenantPackageBindMenuRequest request) {
        tenantPackageService.adminBindMenus(request);
        // 用于操作日志
        var old = tenantPackageService.getNonnullById(request.getPackageId());
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

}
