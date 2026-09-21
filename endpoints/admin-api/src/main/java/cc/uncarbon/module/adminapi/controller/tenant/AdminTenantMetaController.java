package cc.uncarbon.module.adminapi.controller.tenant;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.bizlog.context.LogRecordContext;
import cc.uncarbon.framework.helium.bizlog.service.impl.DiffParseFunction;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.annotation.SysOperateLog;
import cc.uncarbon.module.adminapi.event.KickOutSysUsersEvent;
import cc.uncarbon.module.adminapi.event.RefreshRolePermissionCacheEvent;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.constant.PermissionPattern;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.commons.model.request.IdRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.tenant.model.query.AdminTenantMetaListQuery;
import cc.uncarbon.module.tenant.model.request.AdminTenantCreateRequest;
import cc.uncarbon.module.tenant.model.request.AdminTenantMetaUpdateRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaDTO;
import cc.uncarbon.module.tenant.service.TenantService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "租户管理-" + AdminTenantMetaController.BIZ_TYPE)
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/tenant")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminTenantMetaController {

    private static final String PERMISSION_PREFIX = "tenant:meta:";
    static final String BIZ_TYPE = "租户管理";

    private final TenantService tenantService;
    private final ApplicationEventPublisher eventPublisher;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页查询")
    @PostMapping(value = "/list")
    public ApiResult<PageResult<TenantMetaDTO>> list(@RequestBody @Valid AdminTenantMetaListQuery query) {
        return ApiResult.success(tenantService.adminList(query));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<TenantMetaDTO> detail(@RequestBody @Valid IdRequest<Long> request) {
        return ApiResult.success(tenantService.getNonnullById(request.getId()));
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "新增租户", success = "新增租户：{{#request.code}}|{{#request.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminTenantCreateRequest request) {
        tenantService.adminCreate(request);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "修改租户",
            bizNo = "{{#request.id}}", success = "被操作租户：{{#old.code}}|{{#old.name}}：{_DIFF{#request}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminTenantMetaUpdateRequest request) {
        var old = tenantService.getNonnullById(request.getId());
        var affectedRoleIds = tenantService.adminUpdate(request);
        if (!affectedRoleIds.isEmpty()) {
            // 套餐发生变化，刷新角色权限缓存
            eventPublisher.publishEvent(new RefreshRolePermissionCacheEvent(
                    new RefreshRolePermissionCacheEvent.EventData(affectedRoleIds, request.getId())
            ));
        }
        // 用于操作日志；用于 Diff 比较的两个对象，类型必须一致
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtil.toBean(old, request.getClass()));
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "修改租户状态",
            bizNo = "{{#request.id}}", success = "被操作租户：{{#old.code}}|{{#old.name}}，新状态：{{#request.newStatus.label}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改状态")
    @PostMapping(value = "/set-status")
    public ApiResult<Void> setStatus(@RequestBody @Valid AdminSetStatusRequest<Long, EnabledStatusEnum> request) {
        var old = tenantService.getNonnullById(request.getId());
        List<Long> kickedUserIds = tenantService.adminSetStatus(request);
        if (!kickedUserIds.isEmpty()) {
            // 租户被禁用，强制登出该租户全部用户
            eventPublisher.publishEvent(new KickOutSysUsersEvent(
                    new KickOutSysUsersEvent.EventData(kickedUserIds)
            ));
        }
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "删除租户",
            bizNo = "{{#request.id}}", success = "被操作租户：{{#old.code}}|{{#old.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdRequest<Long> request) {
        var old = tenantService.getNonnullById(request.getId());
        tenantService.adminDelete(Set.of(request.getId()));
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

}
