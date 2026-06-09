package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdsRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.adminapi.helper.RolePermissionCacheHelper;
import cc.uncarbon.module.sys.model.query.AdminSysRoleListQuery;
import cc.uncarbon.module.sys.model.request.AdminBindRoleMenusRequest;
import cc.uncarbon.module.sys.model.request.AdminSysRoleUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysRoleDTO;
import cc.uncarbon.module.sys.service.SysRoleService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Set;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "系统角色管理接口")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/sys/role")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysRoleController {

    private static final String PERMISSION_PREFIX = "SysRole:";

    private final SysRoleService sysRoleService;

    private final RolePermissionCacheHelper rolePermissionCacheHelper;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页查询")
    @PostMapping(value = "/list")
    public ApiResult<PageResult<SysRoleDTO>> list(@RequestBody @Valid AdminSysRoleListQuery query) {
        return ApiResult.success(sysRoleService.adminList(query));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<SysRoleDTO> detail(@RequestParam Long id) {
        return ApiResult.success(sysRoleService.getNonnullById(id));
    }

    // @SysOperateLog(value = "新增系统角色")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminSysRoleUpsertRequest request) {
        request.setTenantId(null);
        sysRoleService.adminCreate(request);

        return ApiResult.success();
    }

    // @SysOperateLog(value = "编辑系统角色")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "编辑")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminSysRoleUpsertRequest request) {
        request.setTenantId(null);
        sysRoleService.adminUpdate(request);

        return ApiResult.success();
    }

    // @SysOperateLog(value = "删除系统角色")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdsRequest<Long> request) {
        sysRoleService.adminDelete(request.getIds());

        // 角色删除时，删除对应缓存键
        rolePermissionCacheHelper.deleteCache(request.getIds());

        return ApiResult.success();
    }

    // @SysOperateLog(value = "绑定角色与菜单关联关系")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + "bindMenus")
    @Operation(summary = "绑定角色与菜单关联关系")
    @PostMapping(value = "/bind-menus")
    public ApiResult<Void> bindMenus(@RequestBody @Valid AdminBindRoleMenusRequest request) {
        Set<String> newPermissions = sysRoleService.adminBindMenus(request);

        // 覆盖更新缓存
        rolePermissionCacheHelper.putCache(request.getRoleId(), newPermissions);

        return ApiResult.success();
    }

}
