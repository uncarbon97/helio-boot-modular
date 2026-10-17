package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * {@link LogResultStatusEnum} 单元测试
 */
class LogResultStatusEnumTest {

    /**
     * 已知常量的取值与标签应一一对应
     */
    @Test
    void knownConstantsValuesAndLabels() {
        Assertions.assertEquals(200, LogResultStatusEnum.SUCCESS.getValue());
        Assertions.assertEquals("成功", LogResultStatusEnum.SUCCESS.getLabel());
        Assertions.assertEquals(400, LogResultStatusEnum.FAILED.getValue());
        Assertions.assertEquals("失败", LogResultStatusEnum.FAILED.getLabel());
        Assertions.assertEquals(2, LogResultStatusEnum.values().length);
    }

    /**
     * 所有常量应有取值且标签非空
     */
    @Test
    void everyConstantHasValueAndNonBlankLabel() {
        for (LogResultStatusEnum item : LogResultStatusEnum.values()) {
            Assertions.assertNotNull(item.getValue());
            Assertions.assertFalse(item.getLabel().isBlank());
        }
    }

    /**
     * BaseEnum.of 应支持成功与失败状态按 value 反查
     */
    @Test
    void baseEnumOfRoundTrip() {
        Optional<LogResultStatusEnum> ok = BaseEnum.of(LogResultStatusEnum.class, 200);
        Assertions.assertTrue(ok.isPresent());
        Assertions.assertSame(LogResultStatusEnum.SUCCESS, ok.get());

        Optional<LogResultStatusEnum> bad = BaseEnum.of(LogResultStatusEnum.class, 400);
        Assertions.assertTrue(bad.isPresent());
        Assertions.assertSame(LogResultStatusEnum.FAILED, bad.get());
    }

    /**
     * BaseEnum.of 对未知取值应返回空 Optional
     */
    @Test
    void baseEnumOfReturnsEmptyForUnknownValue() {
        Assertions.assertFalse(BaseEnum.of(LogResultStatusEnum.class, 500).isPresent());
    }
}
