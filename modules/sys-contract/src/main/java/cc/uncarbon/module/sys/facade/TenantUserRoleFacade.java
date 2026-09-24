package cc.uncarbon.module.sys.facade;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.model.request.TenantRoleBindMenuRequest;
import cc.uncarbon.module.sys.model.request.TenantRoleCreateRequest;
import cc.uncarbon.module.sys.model.request.TenantUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.TenantUserCreateRequest;
import cc.uncarbon.module.sys.model.response.TenantRoleCreateResult;
import cc.uncarbon.module.sys.model.response.TenantUserCreateResult;
import cc.uncarbon.module.sys.model.valueobj.TenantUserBasicProfileDTO;
import org.jspecify.annotations.Nullable;

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
    TenantUserBasicProfileDTO getTenantUserBasicProfile(long tenantId, long userId);

    /**
     * 系统管理 - 取租户用户IDs
     *
     * @param tenantId    租户ID，非主键ID
     * @param statusEnums 仅保留符合指定状态的，可以为null
     */
    List<Long> listUserIdsByTenantId(Long tenantId, Collection<EnabledStatusEnum> statusEnums);

    /**
     * 取指定用户启用状态的关联租户ID列表（真源 sys_user_tenant_relation，忽略租户态读取）
     *
     * @return 按 relation 主键 ASC（即加入先后）排序；无关联时为空列表
     */
    List<Long> listEnabledTenantIdsByUser(Long userId);

    /**
     * 记忆用户当前激活租户（仅更新 sys_user.tenant_id 投影列，供 USER_FIRST 下次登录首选）
     * <p>超级管理员归属平台自营域，视角切换不落投影列</p>
     */
    void rememberActiveTenant(Long userId, Long tenantId);

    /**
     * 指定用户是否为超级管理员（按归属租户解析角色，切换视角后判断不受影响）
     */
    boolean isSuperAdmin(Long userId);

    /**
     * 取指定用户的归属租户ID（sys_user.tenant_id 投影列，忽略租户态读取）
     *
     * @return 无归属时返回 null
     */
    @Nullable
    Long getUserHomeTenantId(Long userId);

}
