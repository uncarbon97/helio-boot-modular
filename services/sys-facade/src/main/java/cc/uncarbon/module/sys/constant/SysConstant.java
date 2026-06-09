package cc.uncarbon.module.sys.constant;


/**
 * 系统管理常量
 */
public final class SysConstant {
    private SysConstant() {
    }

    /**
     * 无上级节点的父级ID
     */
    public static final Long ROOT_PARENT_ID = 0L;

    /**
     * Vben Admin系统管理-空页面
     */
    public static final String VBEN_ADMIN_BLANK_VIEW = "LAYOUT";

    /**
     * 固定超级管理员角色ID
     */
    public static final Long SUPER_ADMIN_ROLE_ID = 1L;

    /**
     * 固定超级管理员角色编码
     */
    public static final String SUPER_ADMIN_ROLE_CODE = "SuperAdmin";

    /**
     * 固定租户管理员角色编码
     * 为了外显美观，没有在前面增加 Tenant 字样
     */
    public static final String TENANT_ADMIN_ROLE_CODE = "Admin";

}
