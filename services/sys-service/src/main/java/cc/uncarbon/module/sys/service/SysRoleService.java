package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.core.exception.BusinessException;
import cc.uncarbon.framework.core.page.PageParam;
import cc.uncarbon.framework.core.page.PageResult;
import cc.uncarbon.module.sys.model.interior.UserRoleContainer;
import cc.uncarbon.module.sys.model.request.AdminBindRoleMenuRelationDTO;
import cc.uncarbon.module.sys.model.request.AdminInsertOrUpdateSysRoleDTO;
import cc.uncarbon.module.sys.model.request.AdminListSysRoleDTO;
import cc.uncarbon.module.sys.model.response.SysRoleBO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 系统角色
 */
public interface SysRoleService {

    /**
     * 系统管理-分页列表
     */
    PageResult<SysRoleBO> adminList(AdminListSysRoleDTO dto);

    /**
     * 系统管理-新增
     */
    Long adminInsert(AdminInsertOrUpdateSysRoleDTO dto);

    /**
     * 系统管理-修改
     */
    void adminUpdate(AdminInsertOrUpdateSysRoleDTO dto);

    /**
     * 系统管理-删除
     */
    void adminDelete(Collection<Long> ids);

    /**
     * 系统管理-绑定角色与菜单关联关系
     */
    Set<String> adminBindMenus(AdminBindRoleMenuRelationDTO dto);

    /**
     * 系统管理-下拉框数据
     */
    List<SysRoleBO> adminSelectOptions();

    /**
     * 系统管理-删除指定租户的特定角色
     */
    void adminDeleteTenantRoles(Collection<Long> tenantIds, Collection<String> roleValues);

    /**
     * 根据 ID 取详情
     */
    SysRoleBO getOneById(Long id);

    /**
     * 根据 ID 取详情
     */
    SysRoleBO getOneById(Long id, boolean throwIfInvalidId) throws BusinessException;

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
