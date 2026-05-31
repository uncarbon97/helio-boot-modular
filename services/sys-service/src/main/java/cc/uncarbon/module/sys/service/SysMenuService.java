package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.sys.model.request.AdminSysMenuUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysMenuBO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 系统菜单
 */
public interface SysMenuService {

    /**
     * 系统管理-列表
     */
    List<SysMenuBO> adminList();

    /**
     * 系统管理-新增
     */
    Long adminCreate(AdminSysMenuUpsertRequest request);

    /**
     * 系统管理-修改
     */
    void adminUpdate(AdminSysMenuUpsertRequest request);

    /**
     * 系统管理-删除
     */
    void adminDelete(Collection<Long> ids);

    /**
     * 系统管理-取侧边菜单
     */
    List<SysMenuBO> adminListSideMenu();

    /**
     * 系统管理-取所有可见菜单
     */
    List<SysMenuBO> adminListVisibleMenu();

    /**
     * 根据 ID 取详情
     */
    SysMenuBO getOneById(Long id);

    /**
     * 根据 ID 取详情
     */
    SysMenuBO getOneById(Long id, boolean throwIfInvalidId) throws BusinessException;

    /**
     * 根据角色Ids，获取角色ID 对应的权限名 Map
     */
    Map<Long, Set<String>> getRoleIdPermissionMap(Collection<Long> roleIds);

    /**
     * 根据菜单ID集合，取权限名集合
     */
    Set<String> listPermissionsByMenuIds(Collection<Long> menuIds);
}
