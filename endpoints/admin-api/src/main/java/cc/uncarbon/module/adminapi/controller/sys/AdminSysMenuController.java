package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdsRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.request.AdminSysMenuUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysMenuInfo;
import cc.uncarbon.module.sys.service.SysMenuService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "系统菜单管理接口")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/sys/menu")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysMenuController {

    private static final String PERMISSION_PREFIX = "SysMenu:";

    private final SysMenuService sysMenuService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "列表")
    @PostMapping(value = "/list")
    public ApiResult<List<SysMenuInfo>> list() {
        return ApiResult.success(sysMenuService.adminList());
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<SysMenuInfo> detail(@RequestParam Long id) {
        return ApiResult.success(sysMenuService.getNonnullById(id));
    }

    // @SysOperateLog(value = "新增系统菜单")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> insert(@RequestBody @Valid AdminSysMenuUpsertRequest request) {
        sysMenuService.adminCreate(request);

        return ApiResult.success();
    }

    // @SysOperateLog(value = "编辑系统菜单")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "编辑")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminSysMenuUpsertRequest request) {
        sysMenuService.adminUpdate(request);

        return ApiResult.success();
    }

    // @SysOperateLog(value = "删除系统菜单")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdsRequest<Long> request) {
        sysMenuService.adminDelete(request.getIds());

        return ApiResult.success();
    }

    @Operation(summary = "取侧边菜单")
    @PostMapping("/side-list")
    public ApiResult<List<SysMenuInfo>> adminListSideMenu() {
        return ApiResult.success(sysMenuService.adminListSideMenu());
    }

    @Operation(summary = "取所有可见菜单")
    @PostMapping("/visible-list")
    public ApiResult<List<SysMenuInfo>> adminListVisibleMenu() {
        return ApiResult.success(sysMenuService.adminListVisibleMenu());
    }

}
