package cc.uncarbon.module.adminapi.controller.sys;


import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdsRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.request.AdminSysDeptUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysDeptDTO;
import cc.uncarbon.module.sys.service.impl.SysDeptServiceImpl;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "系统管理-部门管理")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/sys/dept")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysDeptController {

    private static final String PERMISSION_PREFIX = "SysDept:";

    private final SysDeptServiceImpl sysDeptService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "查询")
    @PostMapping(value = "/list")
    public ApiResult<List<SysDeptDTO>> list() {
        return ApiResult.success(sysDeptService.adminList());
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<SysDeptDTO> detail(@RequestParam Long id) {
        return ApiResult.success(sysDeptService.getById(id, true));
    }

    // @SysOperateLog(value = "新增部门")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> insert(@RequestBody @Valid AdminSysDeptUpsertRequest request) {
        sysDeptService.adminCreate(request);

        return ApiResult.success();
    }

    // @SysOperateLog(value = "编辑部门")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "编辑")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminSysDeptUpsertRequest request) {
        sysDeptService.adminUpdate(request);

        return ApiResult.success();
    }

    // @SysOperateLog(value = "删除部门")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdsRequest<Long> request) {
        sysDeptService.adminDelete(request.getIds());

        return ApiResult.success();
    }

}
