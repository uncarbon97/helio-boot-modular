package cc.uncarbon.module.adminapi.controller.sys;


import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.bizlog.context.LogRecordContext;
import cc.uncarbon.framework.helium.bizlog.service.impl.DiffParseFunction;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.annotation.SysOperateLog;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.AdminBatchSetStatusRequest;
import cc.uncarbon.module.commons.model.request.IdRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.request.AdminSysDeptUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysDeptDTO;
import cc.uncarbon.module.sys.service.SysDeptService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
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
@Tag(name = "后台管理-" + AdminSysDeptController.BIZ_TYPE)
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/sys/dept")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysDeptController {

    private static final String PERMISSION_PREFIX = "SysDept:";
    static final String BIZ_TYPE = "部门管理";

    private final SysDeptService sysDeptService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "列表查询")
    @PostMapping(value = "/list")
    public ApiResult<List<SysDeptDTO>> list() {
        return ApiResult.success(sysDeptService.adminList());
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<SysDeptDTO> detail(@RequestBody @Valid IdRequest<Long> request) {
        return ApiResult.success(sysDeptService.getNonnullById(request.getId()));
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "新增部门",
            success = "新增部门：{{#request.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminSysDeptUpsertRequest request) {
        sysDeptService.adminCreate(request);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "修改部门",
            bizNo = "{{#request.id}}", success = "被操作部门：{{#old.name}}：{_DIFF{#request}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminSysDeptUpsertRequest request) {
        var old = sysDeptService.getNonnullById(request.getId());
        sysDeptService.adminUpdate(request);
        // 用于操作日志；用于 Diff 比较的两个对象，类型必须一致
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtil.toBean(old, request.getClass()));
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "被操作部门",
            success = "删除部门：{{#old.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdRequest<Long> request) {
        var old = sysDeptService.getNonnullById(request.getId());
        sysDeptService.adminDelete(Set.of(request.getId()));
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "修改部门状态",
            bizNo = "{{#request.id}}", success = "被操作部门：{{#old.name}}，新状态：{{#request.newStatus}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + "set-status")
    @Operation(summary = "修改部门状态")
    @PostMapping(value = "/set-status")
    public ApiResult<Void> setStatus(@RequestBody @Valid AdminBatchSetStatusRequest<Long, EnabledStatusEnum> request) {
        // 限制 ID 数量
        request.throwIfIdsSizeGt(1);
        var old = sysDeptService.getNonnullById(CollUtil.getFirst(request.getIds()));
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        sysDeptService.adminSetStatus(request);
        return ApiResult.success();
    }

}
