package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import cc.uncarbon.module.commons.enumdict.EnumDict;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * {@link MenuVisibleScopeEnum} 单元测试
 */
class MenuVisibleScopeEnumTest {

    /**
     * 已知常量的取值与标签应一一对应
     */
    @Test
    void knownConstantsValuesAndLabels() {
        Assertions.assertEquals(1, MenuVisibleScopeEnum.ALL.getValue());
        Assertions.assertEquals("通用", MenuVisibleScopeEnum.ALL.getLabel());
        Assertions.assertEquals(2, MenuVisibleScopeEnum.SUPER_ADMIN_ONLY.getValue());
        Assertions.assertEquals("仅超级管理员", MenuVisibleScopeEnum.SUPER_ADMIN_ONLY.getLabel());
        Assertions.assertEquals(2, MenuVisibleScopeEnum.values().length);
    }

    /**
     * 所有常量应有取值且标签非空
     */
    @Test
    void everyConstantHasValueAndNonBlankLabel() {
        for (MenuVisibleScopeEnum item : MenuVisibleScopeEnum.values()) {
            Assertions.assertNotNull(item.getValue());
            Assertions.assertFalse(item.getLabel().isBlank());
        }
    }

    /**
     * BaseEnum.of 应支持两种可见范围按 value 反查
     */
    @Test
    void baseEnumOfRoundTrip() {
        Optional<MenuVisibleScopeEnum> all = BaseEnum.of(MenuVisibleScopeEnum.class, 1);
        Assertions.assertTrue(all.isPresent());
        Assertions.assertSame(MenuVisibleScopeEnum.ALL, all.get());

        Optional<MenuVisibleScopeEnum> superOnly = BaseEnum.of(MenuVisibleScopeEnum.class, 2);
        Assertions.assertTrue(superOnly.isPresent());
        Assertions.assertSame(MenuVisibleScopeEnum.SUPER_ADMIN_ONLY, superOnly.get());
    }

    /**
     * BaseEnum.of 对未知取值应返回空 Optional
     */
    @Test
    void baseEnumOfReturnsEmptyForUnknownValue() {
        Assertions.assertFalse(BaseEnum.of(MenuVisibleScopeEnum.class, 99).isPresent());
    }

    /**
     * 枚举应标注 @EnumDict 且名称为「菜单可见范围」
     */
    @Test
    void annotatedAsEnumDict() {
        EnumDict dict = MenuVisibleScopeEnum.class.getAnnotation(EnumDict.class);
        Assertions.assertNotNull(dict);
        Assertions.assertEquals("菜单可见范围", dict.name());
    }
}
