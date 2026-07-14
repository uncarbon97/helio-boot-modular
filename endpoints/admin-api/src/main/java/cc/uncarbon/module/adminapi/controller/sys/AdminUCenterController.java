package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.errorcode.AdminApiErrorCodeEnum;
import cc.uncarbon.module.adminapi.helper.HashidsHelper;
import cc.uncarbon.module.adminapi.model.response.FileUploadResultVO;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.file.model.valueobj.FileMetaDTO;
import cc.uncarbon.module.file.service.FileMetaService;
import cc.uncarbon.module.sys.model.request.AdminUpdateMyPasswordRequest;
import cc.uncarbon.module.sys.model.request.AdminUpdateMyProfileRequest;
import cc.uncarbon.module.sys.model.valueobj.MyProfileDTO;
import cc.uncarbon.module.sys.service.AdminUCenterService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ArrayUtil;
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
@Tag(name = "后台管理-用户中心")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/ucenter")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminUCenterController {

    /**
     * 允许上传的头像文件扩展名
     */
    public static final String[] ALLOWED_AVATAR_EXT_NAMES = {"jpg", "jpeg", "png", "webp"};

    private final AdminUCenterService adminUCenterService;
    private final FileMetaService fileMetaService;
    private final HashidsHelper hashidsHelper;


    @Operation(summary = "取当前用户资料")
    @PostMapping(value = "/profile/get")
    public ApiResult<MyProfileDTO> profileGet() {
        return ApiResult.success(adminUCenterService.getMyProfile());
    }

    @Operation(summary = "修改当前用户资料")
    @PostMapping(value = "/profile/update")
    public ApiResult<Void> profileUpdate(@RequestBody @Valid AdminUpdateMyProfileRequest request) {
        adminUCenterService.updateMyProfile(request);
        return ApiResult.success();
    }

    @Operation(summary = "修改当前用户密码")
    @PostMapping(value = "/password/update")
    public ApiResult<Void> passwordUpdate(@RequestBody @Valid AdminUpdateMyPasswordRequest request) {
        adminUCenterService.updateMyPassword(request);

        // 用户更改密码后使其当前会话直接过期
        StpKit.ADMIN.logout();
        return ApiResult.success();
    }

    @Operation(summary = "修改当前用户头像")
    @PostMapping(value = "/avatar/update")
    public ApiResult<Void> avatarUpdate(@RequestBody FileUploadResultVO request) {
        // 不直接信任前端传来的，需先校验
        Long fileId = hashidsHelper.decode(request.getOutFileId());
        FileMetaDTO fileMeta = fileMetaService.getNonnullById(fileId);
        if (!ArrayUtil.contains(ALLOWED_AVATAR_EXT_NAMES, fileMeta.getExtendName())) {
            throw new BusinessException(AdminApiErrorCodeEnum.A04002);
        }

        // 提交的文件名要和元数据一致
        if (!CharSequenceUtil.equals(fileMeta.getStorageFilenameFull(), request.getFilename())
                || !CharSequenceUtil.equals(fileMeta.getOriginalFilenameFull(), request.getOriginalFilename())
        ) {
            throw new BusinessException(AdminApiErrorCodeEnum.A04003);
        }

        adminUCenterService.updateMyAvatar(request.getUrl());
        return ApiResult.success();
    }

}
