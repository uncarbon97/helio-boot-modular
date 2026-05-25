package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.framework.core.page.PageParam;
import cc.uncarbon.framework.core.page.PageResult;
import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.web.model.reponse.ApiResult;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.request.AdminListSysLogDTO;
import cc.uncarbon.module.sys.model.response.SysLogBO;
import cc.uncarbon.module.sys.service.SysLogService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "系统日志管理接口")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysLogController {

    private static final String PERMISSION_PREFIX = "SysLog:";

    private final SysLogService sysLogService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页列表")
    @GetMapping(value = "/sys/logs")
    public ApiResult<PageResult<SysLogBO>> list(AdminListSysLogDTO dto) {
        return ApiResult.success(sysLogService.adminList(pageParam, dto));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @GetMapping(value = "/sys/logs/{id}")
    public ApiResult<SysLogBO> getById(@PathVariable Long id) {
        return ApiResult.success(sysLogService.getOneById(id, true));
    }

}
