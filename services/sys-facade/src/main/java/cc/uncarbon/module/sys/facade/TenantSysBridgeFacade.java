package cc.uncarbon.module.sys.facade;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.model.request.AppendTenantRoleRequest;
import cc.uncarbon.module.sys.model.request.AppendTenantUserRequest;
import cc.uncarbon.module.sys.model.request.BindTenantUserRoleRelationRequest;
import cc.uncarbon.module.sys.model.response.AppendTenantRoleResult;

import java.util.Collection;
import java.util.List;

/**
 * 租户-系统管理桥接门面
 */
public interface TenantSysBridgeFacade {

    /**
     * 增加租户相关的系统角色
     */
    AppendTenantRoleResult appendTenantRole(AppendTenantRoleRequest request);

    /**
     * 增加租户相关的系统用户
     */
    void appendTenantUser(AppendTenantUserRequest request);

    /**
     * 绑定租户相关的系统用户-系统角色关联关系
     */
    void bindTenantUserRoleRelation(BindTenantUserRoleRelationRequest request);

    /**
     * 系统管理 - 取租户用户IDs
     *
     * @param tenantId    租户ID，非主键ID
     * @param statusEnums 仅保留符合指定状态的，可以为null
     */
    List<Long> listUserIdsByTenantId(Long tenantId, Collection<EnabledStatusEnum> statusEnums);

}
