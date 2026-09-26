package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.model.query.AdminSysRoleListRelatedUserQuery;
import cc.uncarbon.module.sys.model.query.AdminSysUserListQuery;
import cc.uncarbon.module.sys.model.request.*;
import cc.uncarbon.module.sys.model.response.SysUserBindRoleResult;
import cc.uncarbon.module.sys.model.response.TenantUserCreateResult;
import cc.uncarbon.module.sys.model.valueobj.SysUserBasicProfileDTO;
import cc.uncarbon.module.sys.model.valueobj.SysUserDTO;

import java.util.Collection;

public interface SysUserService {

    /**
     * 后台管理-分页查询
     */
    PageResult<SysUserDTO> adminList(AdminSysUserListQuery query);

    /**
     * 后台管理-分页查询指定角色关联的用户
     */
    PageResult<SysUserDTO> adminListRoleRelatedUsers(AdminSysRoleListRelatedUserQuery query);

    /**
     * 后台管理-新增
     *
     * @param hasBindDeptPerm 当前用户是否有「调整用户部门」权限
     * @return 主键ID
     */
    Long adminCreate(AdminSysUserCreateRequest request, boolean hasBindDeptPerm);

    /**
     * 后台管理-修改
     */
    void adminUpdate(AdminSysUserUpdateRequest request);

    /**
     * 后台管理-修改状态
     */
    void adminSetStatus(AdminSetStatusRequest<Long, SysUserStatusEnum> request);

    /**
     * 后台管理-删除
     */
    void adminDelete(Collection<Long> ids);

    /**
     * 取当前用户可操作的指定用户详情，无权限会抛出业务异常
     */
    SysUserDTO getOperableById(Long id);

    /**
     * 根据 ID 取当前用户可操作的指定用户详情，未取到会抛出 {@link NoRecordException}
     */
    SysUserDTO getNonnullOperableById(Long id) throws NoRecordException;

    /**
     * 根据 ID 取用户基本信息，未取到会抛出 {@link NoRecordException}
     */
    SysUserBasicProfileDTO getNonnullBasicProfileById(Long id) throws NoRecordException;

    /**
     * 新增租户用户
     */
    TenantUserCreateResult createTenantUser(TenantUserCreateRequest request);

    /**
     * 后台管理-重置密码
     */
    void adminResetPassword(AdminSysUserResetPasswordRequest request);

    /**
     * 后台管理-绑定用户角色
     */
    SysUserBindRoleResult adminBindRole(AdminSysUserBindRoleRequest request);

    /**
     * 后台管理-调整用户部门
     */
    void adminBindDept(AdminSysUserBindDeptRequest request);

    /**
     * 校验当前用户是否可查询目标用户的关联信息（防止跨租户/越权 ID 枚举）
     */
    void checkUserQueryAccess(Long userId);

    /**
     * 校验当前用户是否可查询目标角色的关联信息（防止跨租户/越权 ID 枚举）
     */
    void checkRoleQueryAccess(Long roleId);

}
