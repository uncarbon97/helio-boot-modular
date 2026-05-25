package cc.uncarbon.module.adminapi.controller.auth;


import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.helper.CaptchaHelper;
import cc.uncarbon.module.adminapi.helper.RolePermissionCacheHelper;
import cc.uncarbon.module.adminapi.model.internal.AdminCaptchaContainer;
import cc.uncarbon.module.adminapi.model.response.AdminCaptchaVO;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.sys.model.request.SysUserLoginDTO;
import cc.uncarbon.module.sys.model.response.SysUserLoginBO;
import cc.uncarbon.module.sys.model.response.SysUserLoginVO;
import cc.uncarbon.module.sys.service.impl.SysUserService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpLogic;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@Tag(name = "系统管理-鉴权接口")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/auth")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminAuthController {

    private final SysUserService sysUserService;
    private final RolePermissionCacheHelper rolePermissionCacheHelper;
    private final CaptchaHelper captchaHelper;


    @Operation(summary = "登录")
    @PostMapping(value = "/login")
    public ApiResult<SysUserLoginVO> login(@RequestBody @Valid SysUserLoginDTO dto) {
        // 登录验证码核验；前端项目搜索关键词「Helium: 登录验证码」
        // AdminApiErrorEnum.CAPTCHA_VALIDATE_FAILED.assertTrue(captchaHelper.validate(dto.getCaptchaId(), dto.getCaptchaAnswer()))

        // RPC调用, 失败抛异常, 成功返回用户信息
        SysUserLoginBO userInfo = sysUserService.adminLogin(dto);

        // 构造用户上下文
        UserContext userContext = UserContext.builder()
                .userId(userInfo.getId())
                .userName(userInfo.getUsername())
                .userPhoneNo(userInfo.getPhoneNo())
                .userTypeStr("ADMIN_USER")
                .extraData(null)
                .rolesIds(userInfo.getRoleIds())
                .roles(userInfo.getRoles())
                .build();

        // 将用户ID注册到 SA-Token ，并附加一些业务字段
        StpLogic AdminStpUtil = StpKit.ADMIN;
        AdminStpUtil.login(userInfo.getId(), dto.getRememberMe());
        AdminStpUtil.getSession().set(UserContext.CAMEL_NAME, userContext);
        AdminStpUtil.getSession().set(TenantContext.CAMEL_NAME, userInfo.getTenantContext());

        // 更新角色-权限缓存
        rolePermissionCacheHelper.putCache(userInfo.getRoleIdPermissionMap());

        // 返回登录token
        SysUserLoginVO tokenInfo = SysUserLoginVO.builder()
                .tokenName(AdminStpUtil.getTokenName())
                .tokenValue(AdminStpUtil.getTokenValue())
                .roles(userInfo.getRoles())
                .permissions(userInfo.getPermissions())
                .build();

        return ApiResult.success("登录成功", tokenInfo);
    }

    @SaCheckLogin(type = StpLoginType.ADMIN)
    @Operation(summary = "登出")
    @PostMapping(value = "/logout")
    public ApiResult<Void> logout() {
        AdminStpUtil.logout();
        UserContextHolder.clear();
        TenantContextHolder.clear();

        return ApiResult.success();
    }

    @Operation(summary = "获取验证码")
    @GetMapping(value = "/captcha")
    public ApiResult<AdminCaptchaVO> captcha() {
        // 核验方法：captchaHelper.validate
        AdminCaptchaContainer captchaContainer = captchaHelper.generate();
        return ApiResult.success(new AdminCaptchaVO(captchaContainer));
    }

}
