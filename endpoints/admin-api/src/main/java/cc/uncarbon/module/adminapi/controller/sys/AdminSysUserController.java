package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.event.KickOutSysUsersEvent;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdsRequest;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.model.query.AdminSysUserListQuery;
import cc.uncarbon.module.sys.model.request.AdminBindUserRolesRequest;
import cc.uncarbon.module.sys.model.request.AdminResetSysUserPwdRequest;
import cc.uncarbon.module.sys.model.request.AdminSysUserUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysUserDTO;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cc.uncarbon.module.sys.service.impl.SysUserServiceImpl;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.extra.spring.SpringUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "系统用户管理接口")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/sys/user")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysUserController {

    private static final String PERMISSION_PREFIX = "SysUser:";

    private final SysUserServiceImpl sysUserService;
    private final SysUserRoleRelationService sysUserRoleRelationService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页查询")
    @PostMapping(value = "/list")
    public ApiResult<PageResult<SysUserDTO>> list(@RequestBody @Valid AdminSysUserListQuery query) {
        return ApiResult.success(sysUserService.adminList(query));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<SysUserDTO> detail(@RequestParam Long id) {
        return ApiResult.success(sysUserService.getNonnullById(id));
    }

    // @SysOperateLog(value = "新增系统用户")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> insert(@RequestBody @Valid AdminSysUserUpsertRequest request) {
        request.setId(null).setTenantId(null).validate();
        sysUserService.adminCreate(request);

        return ApiResult.success();
    }

    // @SysOperateLog(value = "编辑系统用户")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "编辑")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminSysUserUpsertRequest request) {
        request.setTenantId(null).validate();
        sysUserService.adminUpdate(request);

        // 新状态是禁用，异步强制登出
        if (request.getStatus() == SysUserStatusEnum.BANNED) {
            SpringUtil.publishEvent(new KickOutSysUsersEvent(
                    new KickOutSysUsersEvent.EventData(Collections.singleton(request.getId()))
            ));
        }

        return ApiResult.success();
    }

    // @SysOperateLog(value = "删除系统用户")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdsRequest<Long> request) {
        sysUserService.adminDelete(request.getIds());

        // 异步强制登出
        SpringUtil.publishEvent(new KickOutSysUsersEvent(
                new KickOutSysUsersEvent.EventData(request.getIds())
        ));

        return ApiResult.success();
    }

    // @SysOperateLog(value = "重置某用户密码")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + "resetPassword")
    @Operation(summary = "重置某用户密码")
    @PostMapping(value = "/reset-password")
    public ApiResult<Void> resetPassword(@RequestBody @Valid AdminResetSysUserPwdRequest request) {
        sysUserService.adminResetUserPassword(request);

        // 强制登出
        StpKit.ADMIN.kickout(request.getUserId());

        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + "bindRoles")
    @Operation(summary = "绑定用户与角色关联关系")
    @PostMapping(value = "/bind-roles")
    public ApiResult<Void> bindRoles(@RequestBody AdminBindUserRolesRequest request) {
        sysUserService.adminBindRoles(request);

        // 异步强制登出，以更新对应权限；可以视业务需要决定是否删除该代码
        SpringUtil.publishEvent(new KickOutSysUsersEvent(
                new KickOutSysUsersEvent.EventData(Collections.singleton(request.getUserId()))
        ));

        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + "kickOut")
    @Operation(summary = "踢某用户下线")
    @PostMapping(value = "/kick-out")
    public ApiResult<Void> kickOut(@RequestParam Long userId) {
        StpKit.ADMIN.kickout(userId);

        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "取指定用户关联角色ID")
    @PostMapping(value = "/list-related-role-ids")
    public ApiResult<List<Long>> listRelatedRoleIds(@RequestParam Long userId) {
        return ApiResult.success(sysUserRoleRelationService.listRoleIdsByUser(userId));
    }

}
