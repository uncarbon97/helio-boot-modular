package cc.uncarbon.module.sys.util;

import cc.uncarbon.framework.helium.db.fieldcrypto.blindindex.BlindIndexValueExtractor;
import cc.uncarbon.framework.helium.db.fieldcrypto.blindindex.BlindIndexValueExtractors;
import lombok.experimental.UtilityClass;

/**
 * PII（Personally Identifiable Information）归一化工具
 *
 * @author Uncarbon
 */
@UtilityClass
public class PiiContractNormalizer {

    private static final BlindIndexValueExtractor PHONE = new BlindIndexValueExtractors.CleanTrim();
    private static final BlindIndexValueExtractor EMAIL = new BlindIndexValueExtractors.LowerTrim();

    /**
     * @return 归一化手机号；空白入参返回 null
     */
    public static String normalizePhone(String raw) {
        String cleaned = raw == null ? null : PHONE.extract(raw);
        return cleaned == null || cleaned.isBlank() ? null : cleaned;
    }

    /**
     * @return 归一化邮箱；空白入参返回 null
     */
    public static String normalizeEmail(String raw) {
        String cleaned = raw == null ? null : EMAIL.extract(raw);
        return cleaned == null || cleaned.isBlank() ? null : cleaned;
    }
}
