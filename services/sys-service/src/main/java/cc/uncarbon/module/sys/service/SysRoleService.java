package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.model.query.AdminSysRoleListQuery;
import cc.uncarbon.module.sys.model.request.AdminBindRoleMenusRequest;
import cc.uncarbon.module.sys.model.request.AdminSysRoleUpsertRequest;
import cc.uncarbon.module.sys.model.request.AppendTenantRoleRequest;
import cc.uncarbon.module.sys.model.response.AppendTenantRoleResult;
import cc.uncarbon.module.sys.model.valueobj.SysRoleDTO;

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
    PageResult<SysRoleDTO> adminList(AdminSysRoleListQuery query);

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
     * 根据 ID 取详情
     */
    SysRoleDTO getById(Long id);

    /**
     * 根据 ID 取详情，未取到会抛出 {@link NoRecordException}
     */
    SysRoleDTO getNonnullById(Long id) throws NoRecordException;

    /**
     * 后台管理-绑定角色与菜单关联关系
     *
     * @return 新菜单ID集合对应的权限名
     */
    Set<String> adminBindMenus(AdminBindRoleMenusRequest dto);

    /**
     * 后台管理-下拉框数据
     */
    List<SysRoleDTO> adminSelectOptions();

    /**
     * 增加租户角色
     */
    AppendTenantRoleResult appendTenantRole(AppendTenantRoleRequest request);

    /**
     * 后台管理-删除指定租户的特定角色
     */
    void adminDeleteTenantRoles(Collection<Long> tenantIds, Collection<String> roleValues);

    /**
     * 取用户ID拥有角色对应的 角色ID-角色名 map
     */
    Map<Long, String> getRoleMapByUserId(Long userId);

}
