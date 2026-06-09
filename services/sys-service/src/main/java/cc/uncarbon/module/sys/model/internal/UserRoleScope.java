package cc.uncarbon.module.sys.model.internal;

import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import lombok.Getter;

import java.util.List;

/**
 * 用户关联角色
 */
@Getter
public class UserRoleScope {

    /**
     * 直接关联的角色IDs
     */
    private final List<Long> relatedRoleIds;

    /**
     * 直接关联的角色实例集合
     */
    private final List<SysRoleEntity> relatedRoles;

    /**
     * 为超级管理员
     */
    private final boolean superAdmin;

    /**
     * 为租户管理员
     */
    private final boolean tenantAdmin;

    /**
     * 非超级管理员or租户管理员
     */
    private final boolean notAnyAdmin;


    public UserRoleScope(List<Long> relatedRoleIds, List<SysRoleEntity> relatedRoles) {
        this.relatedRoleIds = relatedRoleIds;
        this.relatedRoles = relatedRoles;
        this.superAdmin = relatedRoles.stream().anyMatch(SysRoleEntity::isSuperAdmin);
        this.tenantAdmin = relatedRoles.stream().anyMatch(SysRoleEntity::isTenantAdmin);
        this.notAnyAdmin = !this.superAdmin && !this.tenantAdmin;
    }
}
