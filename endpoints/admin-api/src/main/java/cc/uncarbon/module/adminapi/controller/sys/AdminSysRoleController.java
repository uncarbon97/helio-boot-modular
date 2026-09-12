package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.bizlog.context.LogRecordContext;
import cc.uncarbon.framework.helium.bizlog.service.impl.DiffParseFunction;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.annotation.SysOperateLog;
import cc.uncarbon.module.adminapi.event.RefreshRolePermissionCacheEvent;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.AdminBatchSetStatusRequest;
import cc.uncarbon.module.commons.model.request.IdRequest;
import cc.uncarbon.module.commons.model.response.IdResponse;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.query.AdminSysRoleListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysRoleBindMenuRequest;
import cc.uncarbon.module.sys.model.request.AdminSysRoleUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysRoleDTO;
import cc.uncarbon.module.sys.service.SysRoleService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "后台管理-" + AdminSysRoleController.BIZ_TYPE)
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/sys/role")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysRoleController {

    private static final String PERMISSION_PREFIX = "SysRole:";
    static final String BIZ_TYPE = "系统角色管理";

    private final SysRoleService sysRoleService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页查询")
    @PostMapping(value = "/list")
    public ApiResult<PageResult<SysRoleDTO>> list(@RequestBody @Valid AdminSysRoleListQuery query) {
        return ApiResult.success(sysRoleService.adminList(query));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<SysRoleDTO> detail(@RequestBody @Valid IdRequest<Long> request) {
        return ApiResult.success(sysRoleService.getNonnullById(request.getId()));
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "新增角色",
            success = "新增角色：{{#request.code}}|{{#request.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<IdResponse<Long>> create(@RequestBody @Valid AdminSysRoleUpsertRequest request) {
        Long newId = sysRoleService.adminCreate(request);
        return ApiResult.success(new IdResponse<>(newId));
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "修改角色",
            bizNo = "{{#request.id}}", success = "被操作角色：{{#old.code}}|{{#old.name}}：{_DIFF{#request}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminSysRoleUpsertRequest request) {
        var old = sysRoleService.getNonnullById(request.getId());
        sysRoleService.adminUpdate(request);
        // 用于操作日志；用于 Diff 比较的两个对象，类型必须一致
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtil.toBean(old, request.getClass()));
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "删除角色",
            bizNo = "{{#request.id}}", success = "被操作角色：{{#old.code}}|{{#old.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdRequest<Long> request) {
        var old = sysRoleService.getNonnullById(request.getId());
        sysRoleService.adminDelete(Set.of(request.getId()));
        SpringUtil.publishEvent(new RefreshRolePermissionCacheEvent(
                new RefreshRolePermissionCacheEvent.EventData(
                        Set.of(request.getId()), TenantContextHolder.getTenantId())
        ));
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "绑定角色菜单",
            bizNo = "{{#request.id}}", success = "被操作角色：{{#old.code}}|{{#old.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + "bind-menu")
    @Operation(summary = "绑定角色菜单")
    @PostMapping(value = "/bind-menu")
    public ApiResult<Void> bindMenu(@RequestBody @Valid AdminSysRoleBindMenuRequest request) {
        var old = sysRoleService.getNonnullById(request.getRoleId());
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        sysRoleService.adminBindMenu(request);
        SpringUtil.publishEvent(new RefreshRolePermissionCacheEvent(
                new RefreshRolePermissionCacheEvent.EventData(
                        Set.of(request.getRoleId()), TenantContextHolder.getTenantId())
        ));
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "修改角色状态",
            bizNo = "{{#request.id}}", success = "被操作角色：{{#old.code}}|{{#old.name}}，新状态：{{#request.newStatus}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + "set-status")
    @Operation(summary = "修改角色状态")
    @PostMapping(value = "/set-status")
    public ApiResult<Void> setStatus(@RequestBody @Valid AdminBatchSetStatusRequest<Long, EnabledStatusEnum> request) {
        // 限制 ID 数量
        request.throwIfIdsSizeGt(1);
        var old = sysRoleService.getNonnullById(CollUtil.getFirst(request.getIds()));
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        sysRoleService.adminSetStatus(request);
        SpringUtil.publishEvent(new RefreshRolePermissionCacheEvent(
                new RefreshRolePermissionCacheEvent.EventData(
                       request.getIds(), TenantContextHolder.getTenantId())
        ));
        return ApiResult.success();
    }
}
