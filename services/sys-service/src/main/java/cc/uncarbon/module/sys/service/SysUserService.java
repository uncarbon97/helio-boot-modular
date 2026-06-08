package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.model.query.AdminSysUserListQuery;
import cc.uncarbon.module.sys.model.request.*;
import cc.uncarbon.module.sys.model.valueobj.SysUserDTO;
import cc.uncarbon.module.sys.model.response.AdminSysUserLoginResult;
import cc.uncarbon.module.sys.model.valueobj.MyProfileDTO;

import java.util.Collection;

public interface SysUserService {

    /**
     * 后台管理-分页查询
     */
    PageResult<SysUserDTO> adminList(AdminSysUserListQuery query);

    /**
     * 后台管理-新增
     *
     * @return 主键ID
     */
    Long adminCreate(AdminSysUserUpsertRequest request);

    /**
     * 后台管理-修改
     */
    void adminUpdate(AdminSysUserUpsertRequest request);

    /**
     * 后台管理-删除
     */
    void adminDelete(Collection<Long> ids);

    /**
     * 根据 ID 取详情
     */
    SysUserDTO getById(Long id);

    /**
     * 根据 ID 取详情，未取到会抛出 {@link NoRecordException}
     */
    SysUserDTO getNonnullById(Long id) throws NoRecordException;

    /**
     * 后台管理-登录
     */
    AdminSysUserLoginResult adminLogin(AdminSysUserLoginRequest request);

    /**
     * 后台管理-取当前用户资料
     */
    MyProfileDTO adminGetMyProfile();

    /**
     * 后台管理-重置某用户密码
     */
    void adminResetUserPassword(AdminResetSysUserPwdRequest request);

    /**
     * 后台管理-修改当前用户密码
     */
    void adminUpdateCurrentUserPassword(AdminUpdateMyPwdRequest request);

    /**
     * 后台管理-绑定用户与角色关联关系
     */
    void adminBindRoles(AdminBindUserRolesRequest request);

    /**
     * 后台管理-更新当前用户资料
     */
    void adminUpdateMyProfile(AdminUpdateMyProfileRequest request);

    /**
     * 后台管理-更新当前用户头像
     */
    void adminUpdateMyAvatar(AdminUpdateMyAvatarRequest request);

}
