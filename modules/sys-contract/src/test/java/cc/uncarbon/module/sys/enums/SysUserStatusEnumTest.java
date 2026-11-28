package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import cc.uncarbon.module.commons.enumdict.EnumDict;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * {@link SysUserStatusEnum} 单元测试
 */
class SysUserStatusEnumTest {

    /**
     * 已知常量的取值与标签应一一对应
     */
    @Test
    void knownConstantsValuesAndLabels() {
        Assertions.assertEquals(0, SysUserStatusEnum.DISABLED.getValue());
        Assertions.assertEquals("禁用", SysUserStatusEnum.DISABLED.getLabel());
        Assertions.assertEquals(1, SysUserStatusEnum.ENABLED.getValue());
        Assertions.assertEquals("正常", SysUserStatusEnum.ENABLED.getLabel());
        Assertions.assertEquals(2, SysUserStatusEnum.values().length);
    }

    /**
     * 所有常量应有取值且标签非空
     */
    @Test
    void everyConstantHasValueAndNonBlankLabel() {
        for (SysUserStatusEnum item : SysUserStatusEnum.values()) {
            Assertions.assertNotNull(item.getValue());
            Assertions.assertFalse(item.getLabel().isBlank());
        }
    }

    /**
     * BaseEnum.of 应支持禁用与正常状态按 value 反查
     */
    @Test
    void baseEnumOfRoundTrip() {
        Optional<SysUserStatusEnum> disabled = BaseEnum.of(SysUserStatusEnum.class, 0);
        Assertions.assertTrue(disabled.isPresent());
        Assertions.assertSame(SysUserStatusEnum.DISABLED, disabled.get());

        Optional<SysUserStatusEnum> enabled = BaseEnum.of(SysUserStatusEnum.class, 1);
        Assertions.assertTrue(enabled.isPresent());
        Assertions.assertSame(SysUserStatusEnum.ENABLED, enabled.get());
    }

    /**
     * BaseEnum.of 对未知取值应返回空 Optional
     */
    @Test
    void baseEnumOfReturnsEmptyForUnknownValue() {
        Assertions.assertFalse(BaseEnum.of(SysUserStatusEnum.class, 99).isPresent());
    }

    /**
     * 枚举应标注 @EnumDict 且名称为「系统用户状态」
     */
    @Test
    void annotatedAsEnumDict() {
        EnumDict dict = SysUserStatusEnum.class.getAnnotation(EnumDict.class);
        Assertions.assertNotNull(dict);
        Assertions.assertEquals("系统用户状态", dict.name());
    }
}
