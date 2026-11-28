package cc.uncarbon.module.sys.constant;


import lombok.experimental.UtilityClass;

/**
 * 系统管理常量
 */
@UtilityClass
public final class SysConstant {

    /**
     * 无上级节点的父级ID
     */
    public static final Long ROOT_PARENT_ID = 0L;

    /**
     * 固定超级管理员角色 ID
     */
    public static final Long SUPER_ADMIN_ROLE_ID = 0L;

    /**
     * 固定超级管理员用户 ID
     */
    public static final Long SUPER_ADMIN_USER_ID = 0L;

    /**
     * 固定超级管理员角色编码
     */
    public static final String SUPER_ADMIN_ROLE_CODE = "SuperAdmin";

    /**
     * 固定租户管理员角色编码
     * 为了外显美观，没有在前面增加 Tenant 字样
     */
    public static final String TENANT_ADMIN_ROLE_CODE = "OrgAdmin";

}
