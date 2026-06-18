package cc.uncarbon.module.adminapi.controller.file;


import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdRequest;
import cc.uncarbon.module.commons.model.request.IdsRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.file.model.query.AdminFileStorageListQuery;
import cc.uncarbon.module.file.model.request.AdminFileStorageUpsertRequest;
import cc.uncarbon.module.file.model.valueobj.FileStorageDTO;
import cc.uncarbon.module.file.service.FileStorageDataService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "后台管理-文件存储点管理")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/file/storage")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminFileStorageController {

    // 功能权限串前缀
    private static final String PERMISSION_PREFIX = "FileStorage:";

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

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminFileStorageUpsertRequest request) {
        fileStorageDataService.adminCreate(request);
        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminFileStorageUpsertRequest request) {
        fileStorageDataService.adminUpdate(request);
        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdsRequest<Long> request) {
        fileStorageDataService.adminDelete(request.getIds());
        return ApiResult.success();
    }

}
