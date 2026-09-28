package cc.uncarbon.module.sys.util;


import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.module.sys.constant.SysConstant;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;


/**
 * 系统管理工具类
 */
@UtilityClass
public final class SysUtil {

    /**
     * 判断传入的用户ID，是否为超管
     *
     * @param userId 用户ID，可为 null
     * @return true=超管
     */
    public static boolean isSuperAdmin(@Nullable Long userId) {
        return Objects.equals(userId, SysConstant.SUPER_ADMIN_USER_ID);
    }

    /**
     * 判断传入的用户上下文，是否为超管
     *
     * @param userContext 用户上下文
     * @return true=超管
     */
    public static boolean isSuperAdmin(@Nullable UserContext userContext) {
        return Optional.ofNullable(userContext)
                .map(UserContext::getRoleCodes)
                .stream().anyMatch(codes -> codes.contains(SysConstant.SUPER_ADMIN_ROLE_CODE));
    }

    /**
     * 判断传入的用户上下文，是否为租户管理员
     *
     * @param userContext 用户上下文
     * @return true=租户管理员
     */
    public static boolean isTenantAdmin(@Nullable UserContext userContext) {
        return Optional.ofNullable(userContext)
                .map(UserContext::getRoleCodes)
                .stream().anyMatch(codes -> codes.contains(SysConstant.TENANT_ADMIN_ROLE_CODE));
    }
}
