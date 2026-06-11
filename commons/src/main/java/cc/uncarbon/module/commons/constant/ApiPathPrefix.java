package cc.uncarbon.module.commons.constant;


import lombok.experimental.UtilityClass;

/**
 * API 接口路径前缀
 */
@UtilityClass
public final class ApiPathPrefix {

    /**
     * admin-api 前缀
     */
    public static final String ADMIN = "/admin";

    /**
     * admin-api 前缀匹配符
     */
    public static final String ADMIN_PATTERN = ADMIN + "/**";

    /**
     * app-api 前缀
     */
    public static final String APP = "/app";

    /**
     * app-api 前缀匹配符
     */
    public static final String APP_PATTERN = APP + "/**";

}
