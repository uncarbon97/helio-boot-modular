package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.model.request.AdminBatchSetStatusRequest;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.model.query.AdminSysUserListQuery;
import cc.uncarbon.module.sys.model.request.*;
import cc.uncarbon.module.sys.model.response.TenantUserCreateResult;
import cc.uncarbon.module.sys.model.valueobj.SysUserDTO;

import java.util.Collection;

public interface SysUserService {

    /**
     * 后台管理-分页查询
     */
    PageResult<SysUserDTO> adminList(AdminSysUserListQuery query);

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
    void adminSetStatus(AdminBatchSetStatusRequest<Long, SysUserStatusEnum> request);

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
     * 根据 ID 取实体，未取到会抛出 {@link NoRecordException}
     */
    SysUserEntity getNonnullEntityById(Long id) throws NoRecordException;

    /**
     * 新增租户用户
     */
    TenantUserCreateResult createTenantUser(TenantUserCreateRequest request);

    /**
     * 后台管理-重置指定用户密码
     */
    void adminResetSpecifiedUserPassword(AdminSysUserResetSpecifiedOnePasswordRequest request);

    /**
     * 后台管理-绑定用户角色
     */
    void adminBindRole(AdminSysUserBindRoleRequest request);

    /**
     * 后台管理-调整用户部门
     */
    void adminBindDept(AdminSysUserBindDeptRequest request);

}
