package cc.uncarbon.module.adminapi.controller.file;


import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.bizlog.context.LogRecordContext;
import cc.uncarbon.framework.helium.bizlog.service.impl.DiffParseFunction;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.annotation.SysOperateLog;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.file.model.query.AdminFileStorageListQuery;
import cc.uncarbon.module.file.model.request.AdminFileStorageUpsertRequest;
import cc.uncarbon.module.file.model.valueobj.FileStorageDTO;
import cc.uncarbon.module.file.service.FileStorageDataService;
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

import java.util.Set;

@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "后台管理-" + AdminFileStorageController.BIZ_TYPE)
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/file/storage")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminFileStorageController {

    // 功能权限串前缀
    private static final String PERMISSION_PREFIX = "FileStorage:";
    static final String BIZ_TYPE = "文件存储点管理";

    private final FileStorageDataService fileStorageDataService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页查询")
    @PostMapping(value = "/list")
    public ApiResult<PageResult<FileStorageDTO>> list(@RequestBody @Valid AdminFileStorageListQuery query) {
        return ApiResult.success(fileStorageDataService.adminList(query));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<FileStorageDTO> detail(@RequestBody @Valid IdRequest<Long> request) {
        return ApiResult.success(fileStorageDataService.getNonnullById(request.getId()));
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "新增文件存储点",
            success = "新增文件存储点：{{#request.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminFileStorageUpsertRequest request) {
        fileStorageDataService.adminCreate(request);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "修改文件存储点",
            bizNo = "{{#request.id}}", success = "被操作文件存储点：{{#old.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminFileStorageUpsertRequest request) {
        var old = fileStorageDataService.getNonnullById(request.getId());
        fileStorageDataService.adminUpdate(request);
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtil.toBean(old, request.getClass()));
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "删除文件存储点",
            bizNo = "{{#request.id}}", success = "删除文件存储点：{{#old.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdRequest<Long> request) {
        var old = fileStorageDataService.getNonnullById(request.getId());
        fileStorageDataService.adminDelete(Set.of(request.getId()));
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

}
