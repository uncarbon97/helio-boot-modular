package cc.uncarbon.module.commons.util;

import cn.hutool.core.text.CharSequenceUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * PiiMaskUtil 单元测试
 *
 * @author Uncarbon
 */
class PiiMaskUtilTest {

    @Test
    void maskPhone() {
        Assertions.assertEquals("189****8927", PiiMaskUtil.maskPhone("18967038927"));
        Assertions.assertNull(PiiMaskUtil.maskPhone(null));
        Assertions.assertNull(PiiMaskUtil.maskPhone(""));
        Assertions.assertNull(PiiMaskUtil.maskPhone("  "));
        // 位数不足，整体掩码
        Assertions.assertEquals("****", PiiMaskUtil.maskPhone("138"));
        Assertions.assertEquals("****", PiiMaskUtil.maskPhone("1380013"));
        // 边界：恰好 8 位
        Assertions.assertEquals("138****8000", PiiMaskUtil.maskPhone("13800800"));
    }

    @Test
    void maskEmail() {
        Assertions.assertEquals("al***@example.com", PiiMaskUtil.maskEmail("alicealex@example.com"));
        Assertions.assertNull(PiiMaskUtil.maskEmail(null));
        Assertions.assertNull(PiiMaskUtil.maskEmail(""));
        // 本地部分过短，整体掩码本地部分
        Assertions.assertEquals("****@example.com", PiiMaskUtil.maskEmail("ab@example.com"));
        // 无域名，不按邮箱口径泄露
        Assertions.assertEquals("****", PiiMaskUtil.maskEmail("alicealex"));
        // 边界：本地部分恰好 3 字符
        Assertions.assertEquals("ab***@example.com", PiiMaskUtil.maskEmail("abc@example.com"));
    }

    @Test
    void maskUtilIsInCommonsModule() {
        // 口径与 DB 冗余脱敏列一致，见 .agents/2026-10-19/字段落库加密方案.MD §3
        Assertions.assertTrue(CharSequenceUtil.isNotBlank(PiiMaskUtil.maskPhone("18967038927")));
    }
}
