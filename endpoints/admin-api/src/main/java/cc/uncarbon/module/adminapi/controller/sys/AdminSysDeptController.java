package cc.uncarbon.module.adminapi.controller.sys;


import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdRequest;
import cc.uncarbon.module.commons.model.request.IdsRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.request.AdminSysDeptUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysDeptDTO;
import cc.uncarbon.module.sys.service.impl.SysDeptServiceImpl;
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

import java.util.List;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "后台管理-部门管理")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/sys/dept")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysDeptController {

    private static final String PERMISSION_PREFIX = "SysDept:";

    private final SysDeptServiceImpl sysDeptService;


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

    // @SysOperateLog(value = "新增部门")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> create(@RequestBody @Valid AdminSysDeptUpsertRequest request) {
        sysDeptService.adminCreate(request);

        return ApiResult.success();
    }

    // @SysOperateLog(value = "修改部门")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "修改")
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
