package cc.uncarbon.module.adminapi.controller.sys;


import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.query.AdminSysOperateLogListQuery;
import cc.uncarbon.module.sys.model.valueobj.SysOperateLogDTO;
import cc.uncarbon.module.sys.service.SysOperateLogService;
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
@Tag(name = "后台管理-系统操作日志管理")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/sys/operate-log")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminSysOperateLogController {

    // 功能权限串前缀
    private static final String PERMISSION_PREFIX = "SysOperateLog:";

    private final SysOperateLogService sysOperateLogService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页查询")
    @PostMapping(value = "/list")
    public ApiResult<PageResult<SysOperateLogDTO>> list(@RequestBody @Valid AdminSysOperateLogListQuery query) {
        return ApiResult.success(sysOperateLogService.adminList(query));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<SysOperateLogDTO> detail(@RequestBody @Valid IdRequest<Long> request) {
        return ApiResult.success(sysOperateLogService.getNonnullById(request.getId()));
    }
}
