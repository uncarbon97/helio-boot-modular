package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.framework.core.constant.HeliumConstant;
import cc.uncarbon.framework.core.page.PageParam;
import cc.uncarbon.framework.core.page.PageResult;
import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.web.model.request.IdsDTO;
import cc.uncarbon.framework.web.model.reponse.ApiResult;
import cc.uncarbon.module.adminapi.constant.AdminApiConstant;
import cc.uncarbon.module.adminapi.event.KickOutSysUsersEvent;
import cc.uncarbon.module.adminapi.util.AdminStpUtil;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.annotation.SysOperateLog;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.model.request.AdminBindUserRoleRelationDTO;
import cc.uncarbon.module.sys.model.request.AdminInsertOrUpdateSysUserDTO;
import cc.uncarbon.module.sys.model.request.AdminListSysUserDTO;
import cc.uncarbon.module.sys.model.request.AdminResetSysUserPasswordDTO;
import cc.uncarbon.module.sys.model.response.SysUserBO;
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
import java.util.Set;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "后台用户管理接口")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysUserController {

    private static final String PERMISSION_PREFIX = "SysUser:";

    private final SysUserServiceImpl sysUserService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页列表")
    @GetMapping(value = "/sys/users")
    public ApiResult<PageResult<SysUserBO>> list(AdminListSysUserDTO dto) {
        return ApiResult.success(sysUserService.adminList(pageParam, dto));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @GetMapping(value = "/sys/users/{id}")
    public ApiResult<SysUserBO> getById(@PathVariable Long id) {
        return ApiResult.success(sysUserService.getOneById(id, true));
    }

    @SysOperateLog(value = "新增后台用户")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/sys/users")
    public ApiResult<Void> insert(@RequestBody @Valid AdminInsertOrUpdateSysUserDTO dto) {
        dto.setId(null).setTenantId(null).validate();
        sysUserService.adminInsert(dto);

        return ApiResult.success();
    }

    @SysOperateLog(value = "编辑后台用户")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "编辑")
    @PutMapping(value = "/sys/users/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Valid AdminInsertOrUpdateSysUserDTO dto) {
        dto.setId(id).setTenantId(null).validate();
        sysUserService.adminUpdate(dto);

        // 新状态是禁用，异步强制登出
        if (dto.getStatus() == SysUserStatusEnum.BANNED) {
            SpringUtil.publishEvent(new KickOutSysUsersEvent(
                    new KickOutSysUsersEvent.EventData(Collections.singleton(dto.getId()))
            ));
        }

        return ApiResult.success();
    }

    @SysOperateLog(value = "删除后台用户")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @DeleteMapping(value = "/sys/users")
    public ApiResult<Void> delete(@RequestBody @Valid IdsDTO<Long> dto) {
        sysUserService.adminDelete(dto.getIds());

        // 异步强制登出
        SpringUtil.publishEvent(new KickOutSysUsersEvent(
                new KickOutSysUsersEvent.EventData(dto.getIds())
        ));

        return ApiResult.success();
    }

    @SysOperateLog(value = "重置某用户密码")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + "resetPassword")
    @Operation(summary = "重置某用户密码")
    @PutMapping(value = "/sys/users/{userId}/password")
    public ApiResult<Void> resetPassword(@PathVariable Long userId, @RequestBody @Valid AdminResetSysUserPasswordDTO dto) {
        dto.setUserId(userId);
        sysUserService.adminResetUserPassword(dto);

        // 强制登出
        AdminStpUtil.kickout(dto.getUserId());

        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + "bindRoles")
    @Operation(summary = "绑定用户与角色关联关系")
    @PutMapping(value = "/sys/users/{userId}/roles")
    public ApiResult<Void> bindRoles(@PathVariable Long userId, @RequestBody AdminBindUserRoleRelationDTO dto) {
        dto.setUserId(userId);
        sysUserService.adminBindRoles(dto);

        // 异步强制登出，以更新对应权限；可以视业务需要决定是否删除该代码
        SpringUtil.publishEvent(new KickOutSysUsersEvent(
                new KickOutSysUsersEvent.EventData(Collections.singleton(dto.getUserId()))
        ));

        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + "kickOut")
    @Operation(summary = "踢某用户下线")
    @PostMapping(value = "/sys/users/{userId}:kick-out")
    public ApiResult<Void> kickOut(@PathVariable Long userId) {
        AdminStpUtil.kickout(userId);

        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "取指定用户关联角色ID")
    @GetMapping(value = "/sys/users/{userId}/roles")
    public ApiResult<Set<Long>> listRelatedRoleIds(@PathVariable Long userId) {
        return ApiResult.success(sysUserService.listRelatedRoleIds(userId));
    }

}
