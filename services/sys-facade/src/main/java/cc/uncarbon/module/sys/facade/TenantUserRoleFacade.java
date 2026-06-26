package cc.uncarbon.module.sys.facade;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.model.request.TenantUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.TenantRoleBindMenuRequest;
import cc.uncarbon.module.sys.model.request.TenantRoleCreateRequest;
import cc.uncarbon.module.sys.model.request.TenantUserCreateRequest;
import cc.uncarbon.module.sys.model.response.TenantRoleCreateResult;
import cc.uncarbon.module.sys.model.response.TenantUserCreateResult;
import cc.uncarbon.module.sys.model.response.TenantUserBasicProfile;

import java.util.Collection;
import java.util.List;

/**
 * 租户用户、角色门面
 */
public interface TenantUserRoleFacade {

    /**
     * 新增租户角色
     */
    TenantRoleCreateResult createTenantRole(TenantRoleCreateRequest request);

    /**
     * 新增租户用户
     */
    TenantUserCreateResult createTenantUser(TenantUserCreateRequest request);

    /**
     * 绑定租户相关的系统用户-系统角色关联关系
     */
    void bindTenantUserRoleRelation(TenantUserBindRoleRequest request);

    /**
     * 绑定租户角色-菜单关联关系
     */
    void bindTenantRoleMenuRelation(TenantRoleBindMenuRequest request);

    /**
     * 查询租户用户基本资料
     */
    TenantUserBasicProfile getTenantUserBasicProfile(long tenantId, long userId);

    /**
     * 系统管理 - 取租户用户IDs
     *
     * @param tenantId    租户ID，非主键ID
     * @param statusEnums 仅保留符合指定状态的，可以为null
     */
    List<Long> listUserIdsByTenantId(Long tenantId, Collection<EnabledStatusEnum> statusEnums);

}
