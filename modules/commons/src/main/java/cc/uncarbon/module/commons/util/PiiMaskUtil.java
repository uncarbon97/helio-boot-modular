package cc.uncarbon.module.commons.util;

import cn.hutool.core.text.CharSequenceUtil;

/**
 * PII 展示脱敏工具
 *
 * <p>与 DB 冗余脱敏列口径保持一致：手机号保前3后4，邮箱保本地部分前2字符+完整域名</p>
 *
 * @author Uncarbon
 */
public final class PiiMaskUtil {

    private PiiMaskUtil() {
    }

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
            return "****";
        }
        return trimmed.substring(0, 3) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    /**
     * 邮箱脱敏：alicealex@example.com -> al***@example.com
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
            return "****";
        }
        String localPart = trimmed.substring(0, atIndex);
        String domainPart = trimmed.substring(atIndex);
        if (localPart.length() < 3) {
            return "****" + domainPart;
        }
        return localPart.substring(0, 2) + "***" + domainPart;
    }
}
