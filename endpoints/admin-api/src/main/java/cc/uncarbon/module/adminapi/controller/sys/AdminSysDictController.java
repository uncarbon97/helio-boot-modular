package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.bizlog.context.LogRecordContext;
import cc.uncarbon.framework.helium.bizlog.service.impl.DiffParseFunction;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.annotation.SysOperateLog;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.query.AdminSysDictCategoryListQuery;
import cc.uncarbon.module.sys.model.query.AdminSysDictItemListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysDictCategoryUpsertRequest;
import cc.uncarbon.module.sys.model.request.AdminSysDictItemUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysDictBuiltinDTO;
import cc.uncarbon.module.sys.model.valueobj.SysDictCategoryDTO;
import cc.uncarbon.module.sys.model.valueobj.SysDictItemDTO;
import cc.uncarbon.module.sys.service.SysDictService;
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
@Tag(name = "后台管理-" + AdminSysDictController.BIZ_TYPE)
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/sys/dict")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysDictController {

    private static final String PERMISSION_PREFIX = "SysDict:";
    static final String BIZ_TYPE = "字典管理";

    private final SysDictService sysDictService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页查询字典分类")
    @PostMapping(value = "/category/list")
    public ApiResult<PageResult<SysDictCategoryDTO>> list(AdminSysDictCategoryListQuery query) {
        return ApiResult.success(sysDictService.adminListCategory(query));
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "新增字典分类",
            success = "新增字典分类：{{#request.code}}|{{#request.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增字典分类")
    @PostMapping(value = "/category/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminSysDictCategoryUpsertRequest request) {
        sysDictService.adminCreateCategory(request);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "修改字典分类",
            bizNo = "{{#request.id}}", success = "被操作字典分类：{{#old.code}}|{{#old.name}}：{_DIFF{#request}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改字典分类")
    @PostMapping(value = "/category/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminSysDictCategoryUpsertRequest request) {
        var old = sysDictService.getCategoryNonnullById(request.getId());
        sysDictService.adminUpdateCategory(request);
        // 用于操作日志；用于 Diff 比较的两个对象，类型必须一致
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtil.toBean(old, request.getClass()));
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "删除字典分类",
            bizNo = "{{#request.id}}", success = "被操作字典分类：{{#old.code}}|{{#old.name}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除字典分类")
    @PostMapping(value = "/category/delete")
    public ApiResult<Void> deleteCategory(@RequestBody @Valid IdRequest<Long> request) {
        var old = sysDictService.getCategoryNonnullById(request.getId());
        sysDictService.adminDeleteCategory(Set.of(request.getId()));
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页查询字典项")
    @PostMapping(value = "/item/list")
    public ApiResult<PageResult<SysDictItemDTO>> list(@RequestBody @Valid AdminSysDictItemListQuery query) {
        return ApiResult.success(sysDictService.adminListItem(query));
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "新增字典项",
            success = "新增字典项：{{#request.code}}|{{#request.label}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增字典项")
    @PostMapping(value = "/item/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminSysDictItemUpsertRequest request) {
        sysDictService.adminCreateItem(request);
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "修改字典项",
            bizNo = "{{#request.id}}", success = "被操作字典项：{{#old.code}}|{{#old.label}}：{_DIFF{#request}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改字典项")
    @PostMapping(value = "/item/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminSysDictItemUpsertRequest request) {
        var old = sysDictService.getItemNonnullById(request.getId());
        sysDictService.adminUpdateItem(request);
        // 用于操作日志；用于 Diff 比较的两个对象，类型必须一致
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtil.toBean(old, request.getClass()));
        return ApiResult.success();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "删除字典项",
            bizNo = "{{#request.id}}", success = "被操作字典项：{{#old.code}}|{{#old.label}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除字典项")
    @PostMapping(value = "/item/delete")
    public ApiResult<Void> deleteItem(@RequestBody @Valid IdRequest<Long> request) {
        var old = sysDictService.getItemNonnullById(request.getId());
        sysDictService.adminDeleteItem(Set.of(request.getId()));
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, old);
        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页查询内置字典分类")
    @PostMapping(value = "/builtin/list")
    public ApiResult<PageResult<SysDictBuiltinDTO>> listBuiltin(AdminSysDictCategoryListQuery query) {
        return ApiResult.success(sysDictService.adminListBuiltin(query));
    }
}
