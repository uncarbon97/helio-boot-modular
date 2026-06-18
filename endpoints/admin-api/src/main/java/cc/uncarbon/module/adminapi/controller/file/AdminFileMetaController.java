package cc.uncarbon.module.adminapi.controller.file;


import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdRequest;
import cc.uncarbon.module.commons.model.request.IdsRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.file.model.query.AdminFileMetaListQuery;
import cc.uncarbon.module.file.model.request.AdminFileMetaUpsertRequest;
import cc.uncarbon.module.file.service.FileMetaService;
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
@Tag(name = "后台管理-文件管理")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/file/file")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminFileMetaController {

    // 功能权限串前缀
    private static final String PERMISSION_PREFIX = "File:";

    private final FileMetaService fileMetaService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页查询")
    @PostMapping(value = "/list")
    public ApiResult<PageResult<FileMetaDTO>> list(@RequestBody @Valid AdminFileMetaListQuery query) {
        return ApiResult.success(fileMetaService.adminList(query));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<FileMetaDTO> detail(@RequestBody @Valid IdRequest<Long> request) {
        return ApiResult.success(fileMetaService.getNonnullById(request.getId()));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminFileMetaUpsertRequest request) {
        fileMetaService.adminCreate(request);
        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminFileMetaUpsertRequest request) {
        fileMetaService.adminUpdate(request);
        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdsRequest<Long> request) {
        fileMetaService.adminDelete(request.getIds());
        return ApiResult.success();
    }

}
