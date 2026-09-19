package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.framework.helium.bizlog.context.LogRecordContext;
import cc.uncarbon.framework.helium.bizlog.service.impl.DiffParseFunction;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.annotation.SysOperateLog;
import cc.uncarbon.module.adminapi.event.RefreshRolePermissionCacheEvent;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.constant.PermissionPattern;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.commons.model.request.IdRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.request.AdminSysMenuUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysMenuDTO;
import cc.uncarbon.module.sys.service.SysMenuService;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
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

import java.util.*;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "后台管理-" + AdminSysMenuController.BIZ_TYPE)
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/sys/menu")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysMenuController {

    private static final String PERMISSION_PREFIX = "sys:menu:";
    static final String BIZ_TYPE = "系统菜单管理";

    private final SysMenuService sysMenuService;
    private final SysRoleMenuRelationService sysRoleMenuRelationService;
    private final ApplicationEventPublisher eventPublisher;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "列表查询")
    @PostMapping(value = "/list")
    public ApiResult<List<SysMenuDTO>> list() {
        return ApiResult.success(sysMenuService.adminList());
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<SysMenuDTO> detail(@RequestBody @Valid IdRequest<Long> request) {
        return ApiResult.success(sysMenuService.getNonnullById(request.getId()));
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "新增菜单", success = "新增菜单：{{#request.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminSysMenuUpsertRequest request) {
        sysMenuService.adminCreate(request);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "修改菜单",
            bizNo = "{{#request.id}}", success = "被操作菜单：{{#old.name}}：{_DIFF{#request}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminSysMenuUpsertRequest request) {
        var old = sysMenuService.getNonnullById(request.getId());
        sysMenuService.adminUpdate(request);
        // 菜单的权限串/状态/可见性可能变化，刷新绑定角色的权限缓存
        refreshRolePermissionCacheAsync(request.getId());
        // 用于操作日志；用于 Diff 比较的两个对象，类型必须一致
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtil.toBean(old, request.getClass()));
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "删除菜单",
            bizNo = "{{#request.id}}", success = "被操作菜单：{{#old.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdRequest<Long> request) {
        var old = sysMenuService.getNonnullById(request.getId());
        sysMenuService.adminDelete(Set.of(request.getId()));
        // 删除的菜单不再参与鉴权，刷新绑定角色的权限缓存
        refreshRolePermissionCacheAsync(request.getId());
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "修改菜单状态",
            bizNo = "{{#request.id}}", success = "被操作菜单：{{#old.name}}，新状态：{{#request.newStatus.label}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改菜单状态")
    @PostMapping(value = "/set-status")
    public ApiResult<Void> setStatus(@RequestBody @Valid AdminSetStatusRequest<Long, EnabledStatusEnum> request) {
        var old = sysMenuService.getNonnullById(request.getId());
        sysMenuService.adminSetStatus(request);
        // 菜单禁用/启用即时生效，刷新绑定角色的权限缓存
        refreshRolePermissionCacheAsync(request.getId());
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

    @Operation(summary = "取侧边菜单")
    @PostMapping("/side")
    public ApiResult<List<SysMenuDTO>> side() {
        return ApiResult.success(sysMenuService.adminListSideMenus());
    }

    @Operation(summary = "取所有可见菜单")
    @PostMapping("/visible")
    public ApiResult<List<SysMenuDTO>> visible() {
        return ApiResult.success(sysMenuService.adminListVisibleMenus());
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 异步刷新菜单绑定角色的权限缓存
     * <p>
     * 菜单状态/可见性变化会级联影响其子孙菜单（如目录禁用后整棵子树不可见、权限收回），
     * 刷新范围需覆盖子孙菜单绑定的角色
     */
    private void refreshRolePermissionCacheAsync(Long menuId) {
        var boundRoleIds = sysRoleMenuRelationService.listRoleIdsByMenus(collectSelfAndDescendantMenuIds(menuId));
        if (CollUtil.isEmpty(boundRoleIds)) {
            // 没有角色绑定这些菜单，无需刷新
            return;
        }

        eventPublisher.publishEvent(new RefreshRolePermissionCacheEvent(
                new RefreshRolePermissionCacheEvent.EventData(
                        boundRoleIds, TenantContextHolder.getTenantId())
        ));
    }

    /**
     * 收集菜单自身及所有子孙菜单ID（沿 parentId 向下遍历，visited 防环）
     */
    private Set<Long> collectSelfAndDescendantMenuIds(Long menuId) {
        List<SysMenuDTO> allMenus = sysMenuService.adminList();
        Map<Long, Set<Long>> childrenIdsByParentId = new HashMap<>(allMenus.size() << 1);
        for (SysMenuDTO menu : allMenus) {
            if (menu.getParentId() != null) {
                childrenIdsByParentId.computeIfAbsent(menu.getParentId(), k -> new HashSet<>()).add(menu.getId());
            }
        }

        Set<Long> ret = new HashSet<>();
        Deque<Long> pending = new ArrayDeque<>();
        pending.add(menuId);
        while (!pending.isEmpty()) {
            Long current = pending.poll();
            if (!ret.add(current)) {
                // 已收集过，防环
                continue;
            }
            Set<Long> children = childrenIdsByParentId.get(current);
            if (children != null) {
                pending.addAll(children);
            }
        }
        return ret;
    }

}
