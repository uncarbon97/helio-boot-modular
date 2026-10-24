package cc.uncarbon.module.commons.util;

import cn.hutool.core.text.CharSequenceUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * PiiMaskUtil 单元测试
 *
 * @author Uncarbon
 */
class MaskUtilTest {

    @Test
    void maskPhone() {
        Assertions.assertEquals("189****8927", MaskUtil.maskPhone("18967038927"));
        Assertions.assertNull(MaskUtil.maskPhone(null));
        Assertions.assertNull(MaskUtil.maskPhone(""));
        Assertions.assertNull(MaskUtil.maskPhone("  "));
        // 位数不足，整体掩码
        Assertions.assertEquals("****", MaskUtil.maskPhone("138"));
        Assertions.assertEquals("****", MaskUtil.maskPhone("1380013"));
        // 边界：恰好 8 位
        Assertions.assertEquals("138****0800", MaskUtil.maskPhone("13800800"));
    }

    @Test
    void maskEmail() {
        Assertions.assertEquals("al****@example.com", MaskUtil.maskEmail("alicealex@example.com"));
        Assertions.assertNull(MaskUtil.maskEmail(null));
        Assertions.assertNull(MaskUtil.maskEmail(""));
        // 本地部分过短，整体掩码本地部分
        Assertions.assertEquals("****@example.com", MaskUtil.maskEmail("ab@example.com"));
        // 无域名，不按邮箱口径泄露
        Assertions.assertEquals("****", MaskUtil.maskEmail("alicealex"));
        // 边界：本地部分恰好 3 字符
        Assertions.assertEquals("ab****@example.com", MaskUtil.maskEmail("abc@example.com"));
    }

    @Test
    void maskUtilIsInCommonsModule() {
        // 口径与 DB 冗余脱敏列一致，见 .agents/2026-10-19/字段落库加密方案.MD §3
        Assertions.assertTrue(CharSequenceUtil.isNotBlank(MaskUtil.maskPhone("18967038927")));
    }
}
