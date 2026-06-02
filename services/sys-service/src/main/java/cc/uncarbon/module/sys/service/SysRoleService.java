package cc.uncarbon.module.sys.service;

import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.sys.model.interior.UserRoleContainer;
import cc.uncarbon.module.sys.model.query.AdminSysRoleListQuery;
import cc.uncarbon.module.sys.model.request.AdminBindRoleMenuRelationDTO;
import cc.uncarbon.module.sys.model.request.AdminSysRoleUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysRoleBO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 系统角色
 */
public interface SysRoleService {

    /**
     * 后台管理-分页查询
     */
    PageResult<SysRoleBO> adminList(AdminSysRoleListQuery query);

    /**
     * 后台管理-新增
     */
    Long adminCreate(AdminSysRoleUpsertRequest request);

    /**
     * 后台管理-修改
     */
    void adminUpdate(AdminSysRoleUpsertRequest request);

    /**
     * 后台管理-删除
     */
    void adminDelete(Collection<Long> ids);

    /**
     * 后台管理-绑定角色与菜单关联关系
     */
    Set<String> adminBindMenus(AdminBindRoleMenuRelationDTO dto);

    /**
     * 后台管理-下拉框数据
     */
    List<SysRoleBO> adminSelectOptions();

    /**
     * 后台管理-删除指定租户的特定角色
     */
    void adminDeleteTenantRoles(Collection<Long> tenantIds, Collection<String> roleValues);

    /**
     * 根据 ID 取详情
     */
    SysRoleBO getById(Long id);

    /**
     * 根据 ID 取详情，未取到会抛出 {@link NoRecordException}
     */
    SysRoleBO getNonnullById(Long id) throws NoRecordException;

    /**
     * 取用户ID拥有角色对应的 角色ID-角色名 map
     */
    Map<Long, String> getRoleMapByUserId(Long userId);

    /**
     * 取当前用户关联角色信息
     */
    UserRoleContainer getCurrentUserRoleContainer();

    /**
     * 取指定用户关联角色信息
     */
    UserRoleContainer getSpecifiedUserRoleContainer(Long specifiedUserId);

    /**
     * 确定不可见角色IDs
     */
    Set<Long> determineInvisibleRoleIds();
}
