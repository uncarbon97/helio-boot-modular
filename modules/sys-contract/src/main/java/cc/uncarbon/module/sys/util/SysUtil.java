package cc.uncarbon.module.sys.util;


import cc.uncarbon.module.sys.constant.SysConstant;
import lombok.experimental.UtilityClass;

import java.util.Objects;


/**
 * 系统管理工具类
 */
@UtilityClass
public final class SysUtil {

    /**
     * 判断传入的用户ID是否为固定超级管理员
     *
     * @param userId 用户ID，可为 null
     * @return true=超级管理员
     */
    public static boolean isSuperAdmin(Long userId) {
        return Objects.equals(userId, SysConstant.SUPER_ADMIN_USER_ID);
    }
}
