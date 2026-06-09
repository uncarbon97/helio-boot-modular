package cc.uncarbon.module.sys.service;

import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.model.request.AdminSysMenuUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysMenuInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 系统菜单
 */
public interface SysMenuService {

    /**
     * 后台管理-列表
     */
    List<SysMenuInfo> adminList();

    /**
     * 后台管理-新增
     */
    Long adminCreate(AdminSysMenuUpsertRequest request);

    /**
     * 后台管理-修改
     */
    void adminUpdate(AdminSysMenuUpsertRequest request);

    /**
     * 后台管理-删除
     */
    void adminDelete(Collection<Long> ids);

    /**
     * 后台管理-取侧边菜单
     */
    List<SysMenuInfo> adminListSideMenu();

    /**
     * 后台管理-取所有可见菜单
     */
    List<SysMenuInfo> adminListVisibleMenu();

    /**
     * 根据 ID 取详情
     */
    SysMenuInfo getById(Long id);

    /**
     * 根据 ID 取详情，未取到会抛出 {@link NoRecordException}
     */
    SysMenuInfo getNonnullById(Long id) throws NoRecordException;

    /**
     * 列举角色可见的菜单权限串集合
     */
    Map<Long, Set<String>> getPermissionMapByRole(Collection<Long> roleIds);

    /**
     * 根据菜单ID集合，取权限名集合
     */
    Set<String> listPermissionsByMenuIds(Collection<Long> menuIds);
}
