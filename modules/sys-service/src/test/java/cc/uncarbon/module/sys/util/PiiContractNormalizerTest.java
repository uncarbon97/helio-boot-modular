package cc.uncarbon.module.sys.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link PiiContractNormalizer} PII 归一化分支测试
 *
 * <p>归一化口径须与盲索引提取器一致（手机号 CleanTrim、邮箱 LowerTrim），否则写入口与检索侧索引失配
 */
class PiiContractNormalizerTest {

    @Test
    void cleanPhone() {
        Assertions.assertEquals("13800001111", PiiContractNormalizer.cleanPhone("138 0000 1111"));
        Assertions.assertEquals("13800001111", PiiContractNormalizer.cleanPhone("  13800001111\t"));
        Assertions.assertNull(PiiContractNormalizer.cleanPhone(null));
        Assertions.assertNull(PiiContractNormalizer.cleanPhone(""));
        Assertions.assertNull(PiiContractNormalizer.cleanPhone("  "));
    }

    @Test
    void normalizeEmail() {
        Assertions.assertEquals("upper@example.com", PiiContractNormalizer.normalizeEmail("  UPPER@Example.COM "));
        Assertions.assertNull(PiiContractNormalizer.normalizeEmail(null));
        Assertions.assertNull(PiiContractNormalizer.normalizeEmail(""));
        Assertions.assertNull(PiiContractNormalizer.normalizeEmail("\t"));
    }
}
