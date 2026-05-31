package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.query.AdminSysLogListQuery;
import cc.uncarbon.module.sys.model.valueobj.SysLogBO;
import cc.uncarbon.module.sys.service.SysLogService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "系统日志管理接口")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/sys/log")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysLogController {

    private static final String PERMISSION_PREFIX = "SysLog:";

    private final SysLogService sysLogService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页查询")
    @PostMapping(value = "/list")
    public ApiResult<PageResult<SysLogBO>> list(@RequestBody @Valid AdminSysLogListQuery query) {
        return ApiResult.success(sysLogService.adminList(query));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<SysLogBO> detail(@RequestParam Long id) {
        return ApiResult.success(sysLogService.getOneById(id, true));
    }

}
