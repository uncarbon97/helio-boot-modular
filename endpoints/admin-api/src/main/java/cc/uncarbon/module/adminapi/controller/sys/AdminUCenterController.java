package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.model.request.AdminUpdateMyAvatarRequest;
import cc.uncarbon.module.sys.model.request.AdminUpdateMyProfileRequest;
import cc.uncarbon.module.sys.model.request.AdminUpdateMyPwdRequest;
import cc.uncarbon.module.sys.model.valueobj.MyProfileDTO;
import cc.uncarbon.module.sys.service.impl.SysUserServiceImpl;
import cn.dev33.satoken.annotation.SaCheckLogin;
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
@Tag(name = "系统管理-用户中心")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/ucenter")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminUCenterController {

    private final SysUserServiceImpl sysUserService;


    @Operation(summary = "取当前用户信息资料")
    @PostMapping(value = "/profile/get")
    public ApiResult<MyProfileDTO> profileGet() {
        return ApiResult.success(sysUserService.adminGetMyProfile());
    }

    // @SysOperateLog(value = "修改当前用户密码")
    @Operation(summary = "修改当前用户密码")
    @PostMapping(value = "/password/update")
    public ApiResult<Void> passwordUpdate(@RequestBody @Valid AdminUpdateMyPwdRequest request) {
        sysUserService.adminUpdateCurrentUserPassword(request);

        // 用户更改密码后使其当前会话直接过期
        StpKit.ADMIN.logout();
        return ApiResult.success();
    }

    @Operation(summary = "更新当前用户信息资料")
    @PostMapping(value = "/profile/update")
    public ApiResult<Void> profileUpdate(@RequestBody @Valid AdminUpdateMyProfileRequest request) {
        sysUserService.adminUpdateMyProfile(request);
        return ApiResult.success();
    }

    @Operation(summary = "更新当前用户头像")
    @PostMapping(value = "/avatar/update")
    public ApiResult<Void> avatarUpdate(@RequestBody @Valid AdminUpdateMyAvatarRequest request) {
        sysUserService.adminUpdateMyAvatar(request);
        return ApiResult.success();
    }

}
