package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import cc.uncarbon.module.commons.enumdict.EnumDict;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * {@link DictStatusEnum} 单元测试
 */
class DictStatusEnumTest {

    /**
     * 已知常量的取值与标签应一一对应
     */
    @Test
    void knownConstantsValuesAndLabels() {
        Assertions.assertEquals(0, DictStatusEnum.DISABLED.getValue());
        Assertions.assertEquals("禁用", DictStatusEnum.DISABLED.getLabel());
        Assertions.assertEquals(1, DictStatusEnum.ENABLED.getValue());
        Assertions.assertEquals("启用", DictStatusEnum.ENABLED.getLabel());
        Assertions.assertEquals(2, DictStatusEnum.DEPRECATED.getValue());
        Assertions.assertEquals("过时", DictStatusEnum.DEPRECATED.getLabel());
        Assertions.assertEquals(3, DictStatusEnum.values().length);
    }

    /**
     * 所有常量应有取值且标签非空
     */
    @Test
    void everyConstantHasValueAndNonBlankLabel() {
        for (DictStatusEnum item : DictStatusEnum.values()) {
            Assertions.assertNotNull(item.getValue());
            Assertions.assertFalse(item.getLabel().isBlank());
        }
    }

    /**
     * BaseEnum.of 应支持所有常量按 value 往返反查
     */
    @Test
    void baseEnumOfRoundTrip() {
        for (DictStatusEnum item : DictStatusEnum.values()) {
            Optional<DictStatusEnum> hit = BaseEnum.of(DictStatusEnum.class, item.getValue());
            Assertions.assertTrue(hit.isPresent(), item + " 应能按 value 反查");
            Assertions.assertSame(item, hit.get());
        }
    }

    /**
     * BaseEnum.of 对未知取值应返回空 Optional
     */
    @Test
    void baseEnumOfReturnsEmptyForUnknownValue() {
        Assertions.assertFalse(BaseEnum.of(DictStatusEnum.class, 99).isPresent());
    }

    /**
     * 枚举应标注 @EnumDict 且名称为「字典状态」
     */
    @Test
    void annotatedAsEnumDict() {
        EnumDict dict = DictStatusEnum.class.getAnnotation(EnumDict.class);
        Assertions.assertNotNull(dict);
        Assertions.assertEquals("字典状态", dict.name());
    }
}
