package cc.uncarbon.module.commons.errorcode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link DefaultErrorCodeEnum} 单元测试
 */
class DefaultErrorCodeEnumTest {

    /**
     * 枚举常量集合应非空
     */
    @Test
    void valuesNotEmpty() {
        Assertions.assertTrue(DefaultErrorCodeEnum.values().length > 0);
    }

    /**
     * 每个常量的友好描述应非空文本
     */
    @Test
    void everyConstantHasNonBlankFriendlyMessage() {
        for (DefaultErrorCodeEnum item : DefaultErrorCodeEnum.values()) {
            Assertions.assertNotNull(item.getErrorMsgFriendly(), item + " 缺少友好描述");
            Assertions.assertFalse(item.getErrorMsgFriendly().isBlank(), item + " 友好描述为空文本");
        }
    }

    /**
     * 每个常量的错误码应等于枚举名
     */
    @Test
    void everyConstantErrorCodeEqualsEnumName() {
        for (DefaultErrorCodeEnum item : DefaultErrorCodeEnum.values()) {
            Assertions.assertEquals(item.name(), item.getErrorCode(), item + " 错误码应等于枚举名");
        }
    }

    /**
     * 每个常量名应符合 [A][00][CCC] 未分类业务域格式
     */
    @Test
    void everyConstantFollowsUnclassifiedBizGroupPattern() {
        String pattern = "^A" + ErrorCodeBizGroup.UNCLASSIFIED + "\\d{3}$";

        for (DefaultErrorCodeEnum item : DefaultErrorCodeEnum.values()) {
            Assertions.assertTrue(item.name().matches(pattern),
                    item + " 未命中 [A][00][CCC] 格式: " + pattern);
        }
    }

    /**
     * 已知常量应保留既定文案
     */
    @Test
    void knownConstantsKeepFriendlyMessages() {
        Assertions.assertEquals("重复数据", DefaultErrorCodeEnum.A00001.getErrorMsgFriendly());
        Assertions.assertEquals("不存在数据", DefaultErrorCodeEnum.A00002.getErrorMsgFriendly());
        Assertions.assertEquals("长度不得大于 {}", DefaultErrorCodeEnum.A00003.getErrorMsgFriendly());
    }
}
