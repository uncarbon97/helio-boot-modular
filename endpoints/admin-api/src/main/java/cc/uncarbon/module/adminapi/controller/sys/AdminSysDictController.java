package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.web.model.reponse.ApiResult;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.annotation.SysOperateLog;
import cc.uncarbon.module.sys.service.SysDictService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "字典管理接口")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysDictController {

    private static final String PERMISSION_PREFIX = "SysDict:";

    private final SysDictService sysDictService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页列表字典分类")
    @GetMapping(value = "/sys/data-dict/classifieds")
    public ApiResult<PageResult<SysDataDictClassifiedBO>> list(AdminSysDataDictClassifiedListDTO dto) {
        return ApiResult.success(sysDictService.adminListCategory(pageParam, dto));
    }

    @SysOperateLog(value = "新增字典分类")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增字典分类")
    @PostMapping(value = "/sys/data-dict/classifieds")
    public ApiResult<Void> insert(@RequestBody @Valid AdminSysDataDictClassifiedInsertOrUpdateDTO dto) {
        sysDictService.adminInsertCategory(dto);

        return ApiResult.success();
    }

    @SysOperateLog(value = "编辑字典分类")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "编辑字典分类")
    @PutMapping(value = "/sys/data-dict/classifieds/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Valid AdminSysDataDictClassifiedInsertOrUpdateDTO dto) {
        dto.setId(id);
        sysDictService.adminUpdateCategory(dto);

        return ApiResult.success();
    }

    @SysOperateLog(value = "删除字典分类")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除字典分类")
    @DeleteMapping(value = "/sys/data-dict/classifieds")
    public ApiResult<Void> deleteClassified(@RequestBody @Valid IdsDTO<Long> dto) {
        sysDictService.adminDeleteCategory(dto.getIds());

        return ApiResult.success();
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页列表字典分类下的字典项")
    @GetMapping(value = "/sys/data-dict/classifieds/{classifiedId}/items")
    public ApiResult<PageResult<SysDataDictItemBO>> list(@PathVariable Long classifiedId, AdminSysDataDictItemListDTO dto) {
        dto.setClassifiedId(classifiedId);
        return ApiResult.success(sysDictService.adminListItem(pageParam, dto));
    }

    @SysOperateLog(value = "新增字典项")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增字典项")
    @PostMapping(value = "/sys/data-dict/classifieds/{classifiedId}/items")
    public ApiResult<Void> insert(@PathVariable Long classifiedId, @RequestBody @Valid AdminSysDataDictItemInsertOrUpdateDTO dto) {
        dto.setClassifiedId(classifiedId);
        sysDictService.adminInsertItem(dto);

        return ApiResult.success();
    }

    @SysOperateLog(value = "编辑字典项")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "编辑字典项")
    @PutMapping(value = "/sys/data-dict/classifieds/{classifiedId}/items/{id}")
    public ApiResult<Void> update(@PathVariable Long classifiedId, @PathVariable Long id, @RequestBody @Valid AdminSysDataDictItemInsertOrUpdateDTO dto) {
        dto
                .setId(id)
                .setClassifiedId(classifiedId);
        sysDictService.adminUpdateItem(dto);

        return ApiResult.success();
    }

    @SysOperateLog(value = "删除字典项")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除字典项")
    @DeleteMapping(value = "/sys/data-dict/classifieds/{classifiedId}/items")
    public ApiResult<Void> deleteItem(@PathVariable Long classifiedId, @RequestBody @Valid IdsDTO<Long> dto) {
        sysDictService.adminDeleteItem(dto.getIds(), classifiedId);

        return ApiResult.success();
    }
}
