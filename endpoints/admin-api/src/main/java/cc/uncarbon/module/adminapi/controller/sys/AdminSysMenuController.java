package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.bizlog.context.LogRecordContext;
import cc.uncarbon.framework.helium.bizlog.service.impl.DiffParseFunction;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.annotation.SysOperateLog;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.request.AdminSysMenuUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysMenuDTO;
import cc.uncarbon.module.sys.service.SysMenuService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "后台管理-" + AdminSysMenuController.BIZ_TYPE)
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/sys/menu")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysMenuController {

    private static final String PERMISSION_PREFIX = "SysMenu:";
    static final String BIZ_TYPE = "系统菜单管理";

    private final SysMenuService sysMenuService;


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

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "新增菜单",
            success = "新增菜单：{{#request.name}}")
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

}
