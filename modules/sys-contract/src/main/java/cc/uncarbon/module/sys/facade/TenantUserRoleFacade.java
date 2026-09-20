package cc.uncarbon.module.sys.facade;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.model.request.TenantRoleBindMenuRequest;
import cc.uncarbon.module.sys.model.request.TenantRoleCreateRequest;
import cc.uncarbon.module.sys.model.request.TenantUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.TenantUserCreateRequest;
import cc.uncarbon.module.sys.model.response.TenantRoleCreateResult;
import cc.uncarbon.module.sys.model.response.TenantUserBasicProfile;
import cc.uncarbon.module.sys.model.response.TenantUserCreateResult;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 租户用户、角色门面
 */
public interface TenantUserRoleFacade {

    /**
     * 新增租户角色
     */
    TenantRoleCreateResult createTenantRole(TenantRoleCreateRequest request);

    /**
     * 新增租户用户
     */
    TenantUserCreateResult createTenantUser(TenantUserCreateRequest request);

    /**
     * 绑定租户相关的系统用户-系统角色关联关系
     */
    void bindTenantUserRoleRelation(TenantUserBindRoleRequest request);

    /**
     * 绑定租户角色-菜单关联关系
     */
    void bindTenantRoleMenuRelation(TenantRoleBindMenuRequest request);

    /**
     * 按租户套餐菜单同步租户角色菜单
     *
     * @return 返回租户内角色ID集合
     */
    Set<Long> syncTenantRoleMenus(long tenantId, Collection<Long> packageMenuIds);

    /**
     * 查询租户用户基本资料
     */
    TenantUserBasicProfile getTenantUserBasicProfile(long tenantId, long userId);

    /**
     * 系统管理 - 取租户用户IDs
     *
     * @param tenantId    租户ID，非主键ID
     * @param statusEnums 仅保留符合指定状态的，可以为null
     */
    List<Long> listUserIdsByTenantId(Long tenantId, Collection<EnabledStatusEnum> statusEnums);

    /**
     * 用户活体角色快照是否为超级管理员
     * 会话内角色码为登录时快照，高权限操作前以此做活体二次校验
     */
    boolean isSuperAdmin(Long userId);

    /**
     * 用户优先模式（USER_FIRST）下用户归属的启用租户ID列表，默认租户排最前
     */
    List<Long> listUserEnabledTenantIds(Long userId);

    /**
     * 租户优先模式（TENANT_FIRST）下用户归属租户ID（sys_user.tenant_id 单值）
     *
     * @return 用户不存在或未归属时返回 null
     */
    Long getUserTenantId(Long userId);

}
