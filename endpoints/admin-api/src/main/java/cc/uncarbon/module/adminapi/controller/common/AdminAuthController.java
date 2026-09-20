package cc.uncarbon.module.adminapi.controller.common;


import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.web.context.VisitorContextHolder;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.errorcode.AdminApiErrorCodeEnum;
import cc.uncarbon.module.adminapi.helper.TenantSwitchRegistry;
import cc.uncarbon.module.adminapi.props.LoginChallengeProperties;
import cc.uncarbon.module.adminapi.support.loginchallenge.enums.LoginChallengeStrategyTypeEnum;
import cc.uncarbon.module.adminapi.support.loginchallenge.strategy.LoginChallengeStrategy;
import cc.uncarbon.module.adminapi.support.loginchallenge.valueobj.AdminAuthChallengeVO;
import cc.uncarbon.module.adminapi.support.loginguard.LoginFailureGuard;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.enums.UserTypeCodeEnum;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.internal.TenantSwitchInfo;
import cc.uncarbon.module.sys.model.request.AdminAuthPasswordLoginRequest;
import cc.uncarbon.module.sys.model.response.SysUserLoginResult;
import cc.uncarbon.module.sys.model.valueobj.SysUserLoginVO;
import cc.uncarbon.module.sys.service.AdminLoginService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpLogic;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "后台管理--鉴权接口")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/auth")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminAuthController {

    private final AdminLoginService adminLoginService;
    private final LoginChallengeProperties loginChallengeProperties;
    private final List<LoginChallengeStrategy> loginChallengeStrategies;
    private final LoginFailureGuard loginFailureGuard;
    private final TenantSwitchRegistry tenantSwitchRegistry;


    /**
     * fail-fast：配置了挑战策略但无对应实现时，启动即失败，避免静默降级为无验证码登录
     */
    @PostConstruct
    void validateChallengeStrategyConfig() {
        var type = loginChallengeProperties.getStrategy();
        if (type != null && type != LoginChallengeStrategyTypeEnum.NONE && resolveChallengeStrategy() == null) {
            throw new IllegalStateException("已配置登录挑战策略 " + type + "，但容器中无对应实现，拒绝启动");
        }
    }

    @Operation(summary = "登录")
    @PostMapping(value = "/password-login")
    public ApiResult<SysUserLoginVO> login(@RequestBody @Valid AdminAuthPasswordLoginRequest request) {
        LoginChallengeStrategy strategy = resolveChallengeStrategy();
        if (strategy != null && !strategy.validate(request.getCaptchaId(), request.getCaptchaAnswer())) {
            throw new BusinessException(AdminApiErrorCodeEnum.A04001);
        }

        loginFailureGuard.assertNotLocked(request.getTenantCode(), request.getPin());

        SysUserLoginResult loginResult;
        try {
            loginResult = adminLoginService.passwordLogin(request, VisitorContextHolder.getContext());
        } catch (BusinessException be) {
            if (SysErrorCodeEnum.A01001.equals(be.getErrorCode())) {
                // 账号不存在与密码错误统一计数，兼顾防撞库与防枚举
                loginFailureGuard.recordFailure(request.getTenantCode(), request.getPin());
            }
            throw be;
        }
        loginFailureGuard.clear(request.getTenantCode(), request.getPin());

        // 构造用户上下文
        UserContext userContext = new SimpleUserContext()
                .setUserId(loginResult.getId())
                .setUserPin(loginResult.getPin())
                .setUserTypeCode(UserTypeCodeEnum.ADMIN_USER.getValue())
                .setRoleIds(loginResult.getRoleIds())
                .setRoleCodes(loginResult.getRoleCodes())
                .setUserPhoneNo(loginResult.getPhoneNo())
                .setUserNickname(loginResult.getNickname());

        // 注册到 SA-Token ，并附加一些业务字段
        final StpLogic stpUtil = StpKit.ADMIN;
        stpUtil.login(loginResult.getId(), false);
        stpUtil.getSession().set(UserContext.CAMEL_NAME, userContext);
        if (loginResult.getTenantContext() != null) {
            stpUtil.getSession().set(TenantContext.CAMEL_NAME, loginResult.getTenantContext());
            // 登记会话当前处于的租户，供租户禁用时强制登出
            tenantSwitchRegistry.register(loginResult.getTenantContext().getTenantId(), loginResult.getId());
        } else {
            // 平台视角或个人空间：清空租户上下文，防止同一账号会话残留
            stpUtil.getSession().delete(TenantContext.CAMEL_NAME);
        }
        // 清理上一会话的切换标记，新会话恒为默认视角
        stpUtil.getSession().delete(TenantSwitchInfo.CAMEL_NAME);

        // 返回用户态
        SysUserLoginVO tokenInfo = new SysUserLoginVO()
                .setToken(stpUtil.getTokenValue())
                .setRoles(loginResult.getRoleCodes())
                .setPermissions(loginResult.getPermissions())
                .setTenantContext(loginResult.getTenantContext())
                .setPlatformView(loginResult.getTenantContext() == null
                        && loginResult.getRoleCodes() != null
                        && loginResult.getRoleCodes().contains(SysConstant.SUPER_ADMIN_ROLE_CODE))
                .setTenantOptions(loginResult.getTenantOptions());
        return ApiResult.success(tokenInfo);
    }

    @SaCheckLogin(type = StpLoginType.ADMIN)
    @Operation(summary = "登出")
    @PostMapping(value = "/logout")
    public ApiResult<Void> logout() {
        final StpLogic stpUtil = StpKit.ADMIN;
        // 清理租户在会话登记与切换标记
        if (stpUtil.getSession().get(TenantContext.CAMEL_NAME) instanceof TenantContext t
                && t.getTenantId() != null) {
            tenantSwitchRegistry.unregister(t.getTenantId(), stpUtil.getLoginIdAsLong());
        }
        stpUtil.getSession().delete(TenantSwitchInfo.CAMEL_NAME);
        stpUtil.logout();
        return ApiResult.success();
    }

    @Operation(summary = "获取登录挑战")
    @PostMapping(value = "/challenge")
    public ApiResult<AdminAuthChallengeVO> challenge() {
        LoginChallengeStrategy strategy = resolveChallengeStrategy();
        if (strategy == null) {
            return ApiResult.success(AdminAuthChallengeVO.noChallenge());
        }
        return ApiResult.success(strategy.generate());
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 按配置的策略解析挑战处理器；无挑战或未注册的策略返回 null
     */
    private LoginChallengeStrategy resolveChallengeStrategy() {
        var type = loginChallengeProperties.getStrategy();
        if (type == null || type == LoginChallengeStrategyTypeEnum.NONE) {
            return null;
        }

        return loginChallengeStrategies.stream().filter(strategy -> type == strategy.type())
                .findFirst().orElse(null);
    }

}
