package cc.uncarbon.module.adminapi.controller.auth;


import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.web.context.VisitorContextHolder;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.helper.CaptchaHelper;
import cc.uncarbon.module.adminapi.helper.RolePermissionCacheHelper;
import cc.uncarbon.module.adminapi.model.internal.AdminCaptchaScope;
import cc.uncarbon.module.adminapi.model.response.AdminAuthChallengeVO;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.enums.UserTypeCodeEnum;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.request.AdminAuthPasswordLoginRequest;
import cc.uncarbon.module.sys.model.response.AdminLoginResult;
import cc.uncarbon.module.sys.model.valueobj.SysUserLoginVO;
import cc.uncarbon.module.sys.service.AdminLoginService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpLogic;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "后台管理-鉴权接口")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/auth")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminAuthController {

    private final AdminLoginService adminLoginService;
    private final RolePermissionCacheHelper rolePermissionCacheHelper;
    private final CaptchaHelper captchaHelper;


    @Operation(summary = "登录")
    @PostMapping(value = "/password-login")
    public ApiResult<SysUserLoginVO> login(@RequestBody @Valid AdminAuthPasswordLoginRequest request) {
        // 登录验证码核验；前端项目搜索关键词「Helium: 登录验证码」
        // AdminApiErrorEnum.CAPTCHA_VALIDATE_FAILED.assertTrue(captchaHelper.validate(dto.getCaptchaId(), dto.getCaptchaAnswer()))

        AdminLoginResult loginResult = adminLoginService.passwordLogin(request, VisitorContextHolder.getContext());

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
        stpUtil.getSession().set(TenantContext.CAMEL_NAME, loginResult.getTenantContext());

        // 更新角色-权限缓存
        rolePermissionCacheHelper.putCache(loginResult.getRolePermissionMap());

        // 返回用户态
        SysUserLoginVO tokenInfo = new SysUserLoginVO()
                .setToken(stpUtil.getTokenValue())
                .setRoles(loginResult.getRoleCodes())
                .setPermissions(loginResult.getPermissions());
        return ApiResult.success(tokenInfo);
    }

    @SaCheckLogin(type = StpLoginType.ADMIN)
    @Operation(summary = "登出")
    @PostMapping(value = "/logout")
    public ApiResult<Void> logout() {
        final StpLogic stpUtil = StpKit.ADMIN;
        stpUtil.logout();
        return ApiResult.success();
    }

    @Operation(summary = "获取验证码")
    @PostMapping(value = "/challenge")
    public ApiResult<AdminAuthChallengeVO> captcha() {
        // 核验方法：captchaHelper.validate
        AdminCaptchaScope captcha = captchaHelper.generate();
        return ApiResult.success(new AdminAuthChallengeVO(captcha));
    }

}
