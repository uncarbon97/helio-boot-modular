package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.bizlog.context.LogRecordContext;
import cc.uncarbon.framework.helium.bizlog.service.impl.DiffParseFunction;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.annotation.SysOperateLog;
import cc.uncarbon.module.adminapi.event.KickOutSysUsersEvent;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.query.AdminSysUserListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysUserBindDeptRequest;
import cc.uncarbon.module.sys.model.request.AdminSysUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.AdminSysUserResetSpecifiedOnePasswordRequest;
import cc.uncarbon.module.sys.model.request.AdminSysUserUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysUserDTO;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cc.uncarbon.module.sys.service.SysUserService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.extra.spring.SpringUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "后台管理-" + AdminSysUserController.BIZ_TYPE)
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/sys/user")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysUserController {

    private static final String PERMISSION_PREFIX = "SysUser:";
    private static final String BIND_DEPT_PERMISSION = PERMISSION_PREFIX + "bindDept";
    static final String BIZ_TYPE = "系统用户管理";

    private final SysUserService sysUserService;
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
    public ApiResult<SysUserDTO> detail(@RequestBody @Valid IdRequest<Long> request) {
        return ApiResult.success(sysUserService.getNonnullById(request.getId()));
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "新增用户",
            success = "新增用户：{{#request.pin}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminSysUserUpsertRequest request) {
        sysUserService.adminCreate(request);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "修改用户",
            bizNo = "{{#request.id}}", success = "被操作用户：{{#old.pin}}：{_DIFF{#request}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminSysUserUpsertRequest request) {
        var old = sysUserService.getNonnullById(request.getId());
        sysUserService.adminUpdate(request);
        // 用于操作日志；用于 Diff 比较的两个对象，类型必须一致
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtil.toBean(old, request.getClass()));
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "删除用户",
            bizNo = "{{#request.id}}", success = "被操作用户：{{#old.pin}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdRequest<Long> request) {
        var old = sysUserService.getNonnullById(request.getId());
        sysUserService.adminDelete(Set.of(request.getId()));
        kickOutAsync(request.getId());
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "重置指定用户密码",
            bizNo = "{{#request.id}}", success = "被操作用户：{{#old.pin}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + "resetPassword")
    @Operation(summary = "重置指定用户密码")
    @PostMapping(value = "/reset-password")
    public ApiResult<Void> resetPassword(@RequestBody @Valid AdminSysUserResetSpecifiedOnePasswordRequest request) {
        sysUserService.adminResetSpecifiedUserPassword(request);
        kickOutAsync(request.getUserId());
        // 用于操作日志
        var old = sysUserService.getNonnullById(request.getUserId());
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "绑定用户角色",
            bizNo = "{{#request.id}}", success = "被操作用户：{{#old.pin}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + "bindRole")
    @Operation(summary = "绑定用户角色")
    @PostMapping(value = "/bind-role")
    public ApiResult<Void> bindRole(@RequestBody @Valid AdminSysUserBindRoleRequest request) {
        sysUserService.adminBindRole(request);
        // 为了快速更新对应权限；可以视业务需要决定是否删除该代码
        kickOutAsync(request.getUserId());
        // 用于操作日志
        var old = sysUserService.getNonnullById(request.getUserId());
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "调整用户部门",
            bizNo = "{{#request.userId}}", success = "被操作用户：{{#old.pin}}，目标部门ID：{{#request.deptId}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = BIND_DEPT_PERMISSION)
    @Operation(summary = "调整用户所属部门")
    @PostMapping(value = "/bind-dept")
    public ApiResult<Void> bindDept(@RequestBody @Valid AdminSysUserBindDeptRequest request) {
        sysUserService.adminBindDept(request);
        // 用于操作日志
        var old = sysUserService.getNonnullById(request.getUserId());
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "踢用户下线",
            bizNo = "{{#request.id}}", success = "被操作用户：{{#old.pin}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + "kickOut")
    @Operation(summary = "踢用户下线")
    @PostMapping(value = "/kick-out")
    public ApiResult<Void> kickOut(@RequestBody @Valid IdRequest<Long> request) {
        kickOutAsync(request.getId());
        // 用于操作日志
        var old = sysUserService.getNonnullById(request.getId());
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "取指定用户关联角色ID")
    @PostMapping(value = "/list-related-role")
    public ApiResult<List<Long>> listRelatedRole(@RequestBody @Valid IdRequest<Long> request) {
        return ApiResult.success(sysUserRoleRelationService.listRoleIdsByUser(request.getId()));
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 异步强制登出指定用户
     */
    private static void kickOutAsync(long userId) {
        SpringUtil.publishEvent(new KickOutSysUsersEvent(
                new KickOutSysUsersEvent.EventData(Set.of(userId))
        ));
    }
}
