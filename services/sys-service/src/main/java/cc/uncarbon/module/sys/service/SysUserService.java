package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.model.query.AdminSysUserListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.AdminSysUserResetSpecifiedOnePasswordRequest;
import cc.uncarbon.module.sys.model.request.AdminSysUserUpsertRequest;
import cc.uncarbon.module.sys.model.request.TenantUserCreateRequest;
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

}
