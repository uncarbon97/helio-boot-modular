package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * {@link SysRoleFlagEnum} 单元测试
 */
class SysRoleFlagEnumTest {

    /**
     * 已知常量的取值与标签应一一对应
     */
    @Test
    void knownConstantValueAndLabel() {
        Assertions.assertEquals(1, SysRoleFlagEnum.values().length);
        Assertions.assertEquals("builtin", SysRoleFlagEnum.BUILTIN.getValue());
        Assertions.assertEquals("内置", SysRoleFlagEnum.BUILTIN.getLabel());
    }

    /**
     * of() 应能按已知取值反查到内置标记
     */
    @Test
    void ofMatchesKnownValue() {
        Assertions.assertSame(SysRoleFlagEnum.BUILTIN, SysRoleFlagEnum.of("builtin"));
    }

    /**
     * of() 对未知取值应返回 null
     */
    @Test
    void ofReturnsNullForUnknownValue() {
        Assertions.assertNull(SysRoleFlagEnum.of("NOT_EXIST"));
        Assertions.assertNull(SysRoleFlagEnum.of(""));
    }

    /**
     * of() 对 null 入参应返回 null 而非抛出异常
     */
    @Test
    void ofReturnsNullForNullValue() {
        Assertions.assertNull(SysRoleFlagEnum.of(null));
    }

    /**
     * BaseEnum.of 应支持精确匹配与忽略大小写匹配
     */
    @Test
    void baseEnumOfRoundTripAndCaseInsensitive() {
        Optional<SysRoleFlagEnum> exact = BaseEnum.of(SysRoleFlagEnum.class, "builtin");
        Assertions.assertTrue(exact.isPresent());
        Assertions.assertSame(SysRoleFlagEnum.BUILTIN, exact.get());

        Optional<SysRoleFlagEnum> caseInsensitive = BaseEnum.of(SysRoleFlagEnum.class, "BUILTIN");
        Assertions.assertTrue(caseInsensitive.isPresent());
        Assertions.assertSame(SysRoleFlagEnum.BUILTIN, caseInsensitive.get());
    }

    /**
     * BaseEnum.of 对未知取值应返回空 Optional
     */
    @Test
    void baseEnumOfReturnsEmptyForUnknownValue() {
        Assertions.assertFalse(BaseEnum.of(SysRoleFlagEnum.class, "reserved").isPresent());
    }
}
