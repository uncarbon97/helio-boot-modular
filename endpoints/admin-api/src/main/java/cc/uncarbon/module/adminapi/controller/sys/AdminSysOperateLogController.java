package cc.uncarbon.module.adminapi.controller.sys;


import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.bizlog.context.LogRecordContext;
import cc.uncarbon.framework.helium.bizlog.service.impl.DiffParseFunction;
import cc.uncarbon.framework.helium.ratelimiter.annotation.UseRateLimit;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.annotation.SysOperateLog;
import cc.uncarbon.module.adminapi.support.excel.ExcelResponseUtil;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.constant.PermPattern;
import cc.uncarbon.module.commons.model.request.IdRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.query.AdminSysOperateLogListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysOperateLogExportRequest;
import cc.uncarbon.module.sys.model.valueobj.SysOperateLogDTO;
import cc.uncarbon.module.sys.service.SysOperateLogService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Tag(name = "后台管理-" + AdminSysOperateLogController.BIZ_TYPE)
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/sys/operate-log")
@SaCheckLogin(type = StpLoginType.ADMIN)
public class AdminSysOperateLogController {

    /**
     * 权限码前缀
     */
    private static final String PERM_PREFIX = "sys:operate-log:";
    static final String BIZ_TYPE = "系统操作日志管理";

    private final SysOperateLogService sysOperateLogService;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERM_PREFIX + PermPattern.READ)
    @Operation(summary = "分页查询")
    @PostMapping(value = "/list")
    public ApiResult<PageResult<SysOperateLogDTO>> list(@RequestBody @Valid AdminSysOperateLogListQuery query) {
        return ApiResult.success(sysOperateLogService.adminList(query));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERM_PREFIX + PermPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<SysOperateLogDTO> detail(@RequestBody @Valid IdRequest<Long> request) {
        return ApiResult.success(sysOperateLogService.getNonnullById(request.getId()));
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "批量导出",
            success = "批量导出：筛选条件：{{#old}}")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERM_PREFIX + "export")
    @UseRateLimit(mark = "sys-operate-log-export", duration = 60, max = 10)
    @Operation(summary = "批量导出Excel")
    @PostMapping(value = "/export")
    public void export(HttpServletResponse servletResponse, @RequestBody @Valid AdminSysOperateLogExportRequest request)
            throws IOException {
        // 用于操作日志
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, JSONUtil.toJsonStr(request));
        ExcelResponseUtil.writeXlsx(servletResponse, "系统操作日志",
                out -> sysOperateLogService.adminExportExcel(request, out));
    }
}
