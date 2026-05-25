package cc.uncarbon.module.adminapi.controller.file;

import cc.uncarbon.framework.core.constant.HeliumConstant;
import cc.uncarbon.framework.core.page.PageParam;
import cc.uncarbon.framework.core.page.PageResult;
import cc.uncarbon.framework.web.model.request.IdsDTO;
import cc.uncarbon.framework.web.model.reponse.ApiResult;
import cc.uncarbon.module.adminapi.constant.AdminApiConstant;
import cc.uncarbon.module.adminapi.util.AdminStpUtil;
import cc.uncarbon.module.oss.model.query.AdminFileInfoQuery;
import cc.uncarbon.module.oss.model.response.OssFileInfoBO;
import cc.uncarbon.module.oss.service.OssFileInfoService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "系统管理-上传文件信息管理接口")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminOssFileInfoController {

    // 功能权限串前缀
    private static final String PERMISSION_PREFIX = "OssFileInfo:";

    private final OssFileInfoService ossFileInfoService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页列表")
    @GetMapping(value = "/oss/file/infos")
    public ApiResult<PageResult<OssFileInfoBO>> list(AdminFileInfoQuery dto) {
        return ApiResult.success(ossFileInfoService.adminList(pageParam, dto));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @GetMapping(value = "/oss/file/infos/{id}")
    public ApiResult<OssFileInfoBO> getById(@PathVariable Long id) {
        return ApiResult.success(ossFileInfoService.getOneById(id, true));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @DeleteMapping(value = "/oss/file/infos")
    public ApiResult<Void> delete(@RequestBody @Valid IdsDTO<Long> dto) {
        ossFileInfoService.adminDelete(dto.getIds());

        return ApiResult.success();
    }

}
