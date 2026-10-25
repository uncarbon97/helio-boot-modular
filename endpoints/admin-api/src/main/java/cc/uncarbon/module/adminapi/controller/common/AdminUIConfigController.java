package cc.uncarbon.module.adminapi.controller.common;

import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.model.valueobj.AdminSysUserUIConfigVO;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.valueobj.AdminLoginUIConfigVO;
import cc.uncarbon.module.sys.service.AdminLoginService;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cc.uncarbon.module.tenant.util.TenantUtil;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 这里统一存放所有会影响前端 UI 功能开关（显示/隐藏）的后台管理配置契约接口
 * 禁止前端本地硬编码开关常量
 * <p>
 * 登录页渲染前需要拉取的接口允许未登录访问，不加 {@code @SaCheckLogin}；其余接口登录后才可请求
 */
@Tag(name = "后台管理--UI配置接口")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/ui-config")
public class AdminUIConfigController {

    private final AdminLoginService adminLoginService;
    private final TenantFacade tenantFacade;


    @Operation(summary = "获取租户相关UI配置")
    @PostMapping(value = "/login")
    public ApiResult<AdminLoginUIConfigVO> loginUIConfig() {
        return ApiResult.success(adminLoginService.getUIConfig());
    }

    @SaCheckLogin(type = StpLoginType.ADMIN)
    @Operation(summary = "获取用户管理相关UI配置（全局找人/租户加入移出入口）")
    @PostMapping(value = "/sys-user")
    public ApiResult<AdminSysUserUIConfigVO> sysUserUIConfig() {
        boolean platformDomainFlag = TenantUtil.isPlatformDomain(TenantContextHolder.getTenantId());
        return ApiResult.success(AdminSysUserUIConfigVO.of(tenantFacade.getLoginMode(), platformDomainFlag));
    }

}
