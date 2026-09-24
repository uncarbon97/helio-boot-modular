package cc.uncarbon.module.adminapi.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * admin-api 缓存键集中定义
 *
 * @author Uncarbon
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminCacheKeyConstant {

    public static final String PREFIX = "admin:";

    /**
     * 图形验证码（OCR）答案
     */
    public static final String LOGIN_CHALLENGE_OCR = PREFIX + "login-challenge:ocr:%s";

    /**
     * 登录失败次数（防撞库）
     */
    public static final String LOGIN_CHALLENGE_FAIL_COUNT = PREFIX + "login-challenge:fail-cnt:%s:%s";

    /**
     * 角色权限缓存键前缀
     */
    public static final String ROLE_PERMISSIONS = PREFIX + "role-perm:%s";

    /**
     * 租户-在会话用户登记簿
     */
    public static final String TENANT_LOGGED_IN = PREFIX + "tenant-logins:%s";

}
