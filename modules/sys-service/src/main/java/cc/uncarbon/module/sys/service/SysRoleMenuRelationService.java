package cc.uncarbon.module.sys.service;

import java.util.Collection;
import java.util.Set;

/**
 * 系统角色-可见菜单关联
 */
public interface SysRoleMenuRelationService {

    /**
     * 列举角色关联的菜单IDs
     */
    Set<Long> listMenuIdsByRoles(Collection<Long> roleIds);

    /**
     * 列举菜单关联的角色IDs
     */
    Set<Long> listRoleIdsByMenus(Collection<Long> menuIds);

    /**
     * 绑定角色菜单，增量更新
     */
    void cleanAndBind(Long roleId, Collection<Long> menuIds);

    /**
     * 根据角色IDs，删除角色-菜单关联关系
     * 用于角色被删除后清理孤儿数据
     */
    void deleteByRoleIds(Collection<Long> roleIds);

    /**
     * 根据菜单IDs，删除角色-菜单关联关系
     * 用于菜单被删除后清理孤儿数据
     */
    void deleteByMenuIds(Collection<Long> menuIds);
}
