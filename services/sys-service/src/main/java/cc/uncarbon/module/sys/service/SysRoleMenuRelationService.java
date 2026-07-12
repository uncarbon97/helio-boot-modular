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
     * 绑定角色菜单，增量更新
     */
    void cleanAndBind(Long roleId, Collection<Long> menuIds);
}
