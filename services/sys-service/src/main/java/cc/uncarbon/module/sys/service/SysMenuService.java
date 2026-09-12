package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.model.request.AdminBatchSetStatusRequest;
import cc.uncarbon.module.sys.model.request.AdminSysMenuUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysMenuDTO;

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
    List<SysMenuDTO> adminList();

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
     * 后台管理-修改状态
     * <p>
     * 禁用/启用菜单不会立即刷新已缓存的角色权限串（Redis 缓存最长 6 小时后过期），新登录用户即时生效
     */
    void adminSetStatus(AdminBatchSetStatusRequest<Long, EnabledStatusEnum> request);

    /**
     * 根据 ID 取详情
     */
    SysMenuDTO getById(Long id);

    /**
     * 根据 ID 取详情，未取到会抛出 {@link NoRecordException}
     */
    SysMenuDTO getNonnullById(Long id) throws NoRecordException;

    /**
     * 后台管理-取侧边菜单
     */
    List<SysMenuDTO> adminListSideMenus();

    /**
     * 后台管理-取可用菜单
     */
    List<SysMenuDTO> adminListVisibleMenus();

    /**
     * 列举角色可见的菜单权限串集合
     * 已禁用或已删除的角色返回空权限集合
     */
    Map<Long, Set<String>> getPermissionsByRole(Collection<Long> roleIds);

    /**
     * 列举菜单对应的权限串集合
     */
    Set<String> listPermissionsByMenus(Collection<Long> menuIds);
}
