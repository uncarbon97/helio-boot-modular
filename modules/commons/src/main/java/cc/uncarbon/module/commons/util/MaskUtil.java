package cc.uncarbon.module.commons.util;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.experimental.UtilityClass;

/**
 * 脱敏工具类
 *
 * @author Uncarbon
 */
@UtilityClass
public final class MaskUtil {

    public static final String STAR_4 = "****";


    /**
     * 手机号脱敏：18967038927 -> 189****8927
     *
     * <p>位数不足 8 位时整体掩码，避免短号码泄露面过大</p>
     */
    public static String maskPhone(String phoneNo) {
        if (CharSequenceUtil.isBlank(phoneNo)) {
            return null;
        }
        String trimmed = phoneNo.trim();
        if (trimmed.length() < 8) {
            return STAR_4;
        }
        return trimmed.substring(0, 3) + STAR_4 + trimmed.substring(trimmed.length() - 4);
    }

    /**
     * 邮箱脱敏：alicealex@example.com -> al****@example.com
     *
     * <p>本地部分不足 3 字符时本地部分整体掩码（避免 1~2 字符邮箱被完整还原）</p>
     */
    public static String maskEmail(String email) {
        if (CharSequenceUtil.isBlank(email)) {
            return null;
        }
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex < 0) {
            // 非标准邮箱，不按邮箱口径泄露
            return STAR_4;
        }
        String localPart = trimmed.substring(0, atIndex);
        String domainPart = trimmed.substring(atIndex);
        if (localPart.length() < 3) {
            return STAR_4 + domainPart;
        }
        return localPart.substring(0, 2) + STAR_4 + domainPart;
    }
}
