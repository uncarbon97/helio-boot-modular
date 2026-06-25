package cc.uncarbon.module.sys.service;

import cc.uncarbon.module.sys.model.request.*;
import cc.uncarbon.module.sys.model.valueobj.MyProfileDTO;

/**
 * 后台管理-用户中心
 */
public interface AdminUCenterService {

    /**
     * 取当前用户资料
     */
    MyProfileDTO getMyProfile();

    /**
     * 修改当前用户密码
     */
    void updateMyPassword(AdminUpdateMyPasswordRequest request);

    /**
     * 更新当前用户资料
     */
    void updateMyProfile(AdminUpdateMyProfileRequest request);

    /**
     * 更新当前用户头像
     */
    void updateMyAvatar(String url);

}
