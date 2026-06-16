package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdsRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.query.AdminSysDictCategoryListQuery;
import cc.uncarbon.module.sys.model.query.AdminSysDictItemListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysDictCategoryUpsertRequest;
import cc.uncarbon.module.sys.model.request.AdminSysDictItemUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysDictCategoryDTO;
import cc.uncarbon.module.sys.model.valueobj.SysDictItemDTO;
import cc.uncarbon.module.sys.service.SysDictService;
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
@Tag(name = "后台管理-字典管理")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/sys/dict")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysDictController {

    private static final String PERMISSION_PREFIX = "SysDict:";

    private final SysDictService sysDictService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页查询字典分类")
    @PostMapping(value = "/category/list")
    public ApiResult<PageResult<SysDictCategoryDTO>> list(AdminSysDictCategoryListQuery query) {
        return ApiResult.success(sysDictService.adminListCategory(query));
    }

    // @SysOperateLog(value = "新增字典分类")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增字典分类")
    @PostMapping(value = "/category/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminSysDictCategoryUpsertRequest request) {
        sysDictService.adminCreateCategory(request);

        return ApiResult.success();
    }

    // @SysOperateLog(value = "修改字典分类")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改字典分类")
    @PostMapping(value = "/category/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminSysDictCategoryUpsertRequest request) {
        sysDictService.adminUpdateCategory(request);

        return ApiResult.success();
    }

    // @SysOperateLog(value = "删除字典分类")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除字典分类")
    @PostMapping(value = "/category/delete")
    public ApiResult<Void> deleteClassified(@RequestBody @Valid IdsRequest<Long> request) {
        sysDictService.adminDeleteCategory(request.getIds());

        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页查询字典项")
    @PostMapping(value = "/item/list")
    public ApiResult<PageResult<SysDictItemDTO>> list(@RequestBody @Valid AdminSysDictItemListQuery query) {
        return ApiResult.success(sysDictService.adminListItem(query));
    }

    // @SysOperateLog(value = "新增字典项")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增字典项")
    @PostMapping(value = "/item/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminSysDictItemUpsertRequest request) {
        sysDictService.adminCreateItem(request);

        return ApiResult.success();
    }

    // @SysOperateLog(value = "修改字典项")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改字典项")
    @PostMapping(value = "/item/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminSysDictItemUpsertRequest request) {
        sysDictService.adminUpdateItem(request);

        return ApiResult.success();
    }

    // @SysOperateLog(value = "删除字典项")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除字典项")
    @PostMapping(value = "/item/delete")
    public ApiResult<Void> deleteItem(@RequestBody @Valid IdsRequest<Long> request) {
        sysDictService.adminDeleteItem(request.getIds());

        return ApiResult.success();
    }
}
