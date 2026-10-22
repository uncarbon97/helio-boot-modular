package cc.uncarbon.module.sys.util;

import cc.uncarbon.framework.helium.fieldcrypto.annotation.BlindIndexValueExtractor;
import cc.uncarbon.framework.helium.fieldcrypto.annotation.BlindIndexValueExtractors;

/**
 * 手机号/邮箱写侧归一化
 *
 * <p>与实体 @BlindIndexOf 的 extractor 同源（同一实现类），避免两套口径漂移：
 * 手机号 trim + 去内嵌空格，邮箱 trim + 小写（不做 Gmail 点号/+别名折叠）</p>
 *
 * @author Uncarbon
 */
@lombok.experimental.UtilityClass
public class PiiContactNormalizer {

    private static final BlindIndexValueExtractor PHONE = new BlindIndexValueExtractors.CleanTrim();
    private static final BlindIndexValueExtractor EMAIL = new BlindIndexValueExtractors.LowerTrim();

    /**
     * @return 归一化手机号；空白入参返回 null（空串一律归一为 NULL，§3.3）
     */
    public static String cleanPhone(String raw) {
        String cleaned = raw == null ? null : PHONE.extract(raw);
        return cleaned == null || cleaned.isBlank() ? null : cleaned;
    }

    /**
     * @return 归一化邮箱（小写）；空白入参返回 null
     */
    public static String normalizeEmail(String raw) {
        String cleaned = raw == null ? null : EMAIL.extract(raw);
        return cleaned == null || cleaned.isBlank() ? null : cleaned;
    }
}
