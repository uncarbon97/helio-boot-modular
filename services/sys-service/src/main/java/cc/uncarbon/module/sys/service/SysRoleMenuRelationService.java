package cc.uncarbon.module.sys.service;

import java.util.Collection;
import java.util.Set;

/**
 * 系统角色-可见菜单关联
 */
public interface SysRoleMenuRelationService {

    /**
     * 根据角色Ids取菜单Ids
     */
    Set<Long> listMenuIdsByRoleIds(Collection<Long> roleIds) throws IllegalArgumentException;

    /**
     * 绑定角色ID与菜单ID关联关系，增量更新
     */
    void cleanAndBind(Long roleId, Collection<Long> menuIds);
}
