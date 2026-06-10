package cc.uncarbon.module.sys.service;

import cc.uncarbon.module.sys.model.request.BindTenantUserRoleRelationRequest;

import java.util.Collection;
import java.util.List;

/**
 * 系统用户-角色关联关系
 */
public interface SysUserRoleRelationService {

    /**
     * 后台管理-新增
     * 注：本方法较为特殊，仅供SysTenantFacadeImpl调用
     */
    Long adminCreate(Long tenantId, Long userId, Long roleId);

    /**
     * 绑定用户与角色关联关系，增量更新
     */
    void cleanAndBind(Long userId, Collection<Long> roleIds);

    /**
     * 根据用户ID，查询关联的角色IDs
     */
    List<Long> listRoleIdsByUser(Long userId);

    /**
     * 绑定租户相关的系统用户-系统角色关联关系
     */
    void bindTenantUserRoleRelation(BindTenantUserRoleRelationRequest request);

}
