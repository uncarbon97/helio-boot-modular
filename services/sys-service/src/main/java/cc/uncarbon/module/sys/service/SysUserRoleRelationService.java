package cc.uncarbon.module.sys.service;

import java.util.Collection;
import java.util.Set;

/**
 * 后台用户-角色关联
 */
public interface SysUserRoleRelationService {

    /**
     * 系统管理-新增
     * 注：本方法较为特殊，仅供SysTenantFacadeImpl调用
     */
    Long adminCreate(Long tenantId, Long userId, Long roleId);

    /**
     * 先清理用户ID所有关联关系, 再绑定用户ID与角色ID
     */
    void cleanAndBind(Long userId, Collection<Long> roleIds);

    /**
     * 取拥有角色Ids
     */
    Set<Long> listRoleIdsByUserId(Long userId) throws IllegalArgumentException;

    /**
     * 取角色IDs关联的用户IDs
     */
    Set<Long> listUserIdsByRoleIds(Collection<Long> roleIds);
}
