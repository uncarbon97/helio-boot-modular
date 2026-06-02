package cc.uncarbon.module.sys.service;

import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.model.query.AdminSysUserListQuery;
import cc.uncarbon.module.sys.model.request.*;
import cc.uncarbon.module.sys.model.valueobj.SysUserBO;
import cc.uncarbon.module.sys.model.valueobj.SysUserLoginBO;
import cc.uncarbon.module.sys.model.valueobj.VbenAdminUserInfoVO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface SysUserService {

    /**
     * 后台管理-分页查询
     */
    PageResult<SysUserBO> adminList(AdminSysUserListQuery query);

    /**
     * 根据 ID 取详情
     */
    SysUserBO getById(Long id);

    /**
     * 根据 ID 取详情，未取到会抛出 {@link NoRecordException}
     */
    SysUserBO getNonnullById(Long id) throws NoRecordException;

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
     * 后台管理-登录
     */
    SysUserLoginBO adminLogin(SysUserLoginDTO dto);

    /**
     * 后台管理-取当前用户信息
     */
    VbenAdminUserInfoVO adminGetCurrentUserInfo();

    /**
     * 后台管理-重置某用户密码
     */
    void adminResetUserPassword(AdminResetSysUserPasswordDTO dto);

    /**
     * 后台管理-修改当前用户密码
     */
    void adminUpdateCurrentUserPassword(AdminUpdateCurrentSysUserPasswordDTO dto);

    /**
     * 后台管理-绑定用户与角色关联关系
     */
    void adminBindRoles(AdminBindUserRoleRelationDTO dto);

    /**
     * 根据用户账号查询
     */
    SysUserEntity getUserByPin(String pin);

    /**
     * 系统管理 - 取指定用户关联角色ID
     *
     * @param userId 用户ID
     * @return 角色Ids
     */
    Set<Long> listRelatedRoleIds(Long userId);

    /**
     * 系统管理 - 取租户用户IDs
     *
     * @param tenantId    租户ID，非主键ID
     * @param statusEnums 仅保留符合指定状态的，可以为null
     */
    List<Long> listUserIdsByTenantId(Long tenantId, Collection<EnabledStatusEnum> statusEnums);

    /**
     * 后台管理-更新当前用户信息资料
     */
    void adminUpdateCurrentUserInfo(AdminUpdateCurrentSysUserInfoDTO dto);

    /**
     * 后台管理-更新当前用户头像
     */
    void adminUpdateCurrentUserAvatar(AdminUpdateCurrentSysUserAvatarDTO dto);

}
