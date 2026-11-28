package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import cc.uncarbon.module.commons.enumdict.EnumDict;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

/**
 * {@link MenuTypeEnum} 单元测试
 */
class MenuTypeEnumTest {

    /**
     * 所有常量应有取值且标签非空
     */
    @Test
    void allConstantsHaveValueAndNonBlankLabel() {
        for (MenuTypeEnum item : MenuTypeEnum.values()) {
            Assertions.assertNotNull(item.getValue(), item + " 缺少 value");
            Assertions.assertFalse(item.getLabel().isBlank(), item + " label 为空文本");
        }
    }

    /**
     * 已知常量的取值与标签应一一对应
     */
    @Test
    void knownConstantsAndValues() {
        Assertions.assertEquals(0, MenuTypeEnum.DIR.getValue());
        Assertions.assertEquals("目录", MenuTypeEnum.DIR.getLabel());
        Assertions.assertEquals(1, MenuTypeEnum.MENU.getValue());
        Assertions.assertEquals("菜单", MenuTypeEnum.MENU.getLabel());
        Assertions.assertEquals(2, MenuTypeEnum.BUTTON.getValue());
        Assertions.assertEquals("按钮", MenuTypeEnum.BUTTON.getLabel());
        Assertions.assertEquals(3, MenuTypeEnum.EXTERNAL_LINK.getValue());
        Assertions.assertEquals("外链", MenuTypeEnum.EXTERNAL_LINK.getLabel());
    }

    /**
     * all() 应按声明顺序返回全部菜单类型
     */
    @Test
    void allReturnsEveryTypeInDeclarationOrder() {
        Assertions.assertEquals(List.of(
                MenuTypeEnum.DIR, MenuTypeEnum.MENU, MenuTypeEnum.BUTTON, MenuTypeEnum.EXTERNAL_LINK
        ), MenuTypeEnum.all());
    }

    /**
     * forAdminSide() 应仅包含目录、菜单、外链，不含按钮
     */
    @Test
    void forAdminSideExcludesButton() {
        List<MenuTypeEnum> side = MenuTypeEnum.forAdminSide();

        Assertions.assertEquals(List.of(MenuTypeEnum.DIR, MenuTypeEnum.MENU, MenuTypeEnum.EXTERNAL_LINK), side);
        Assertions.assertFalse(side.contains(MenuTypeEnum.BUTTON));
    }

    /**
     * BaseEnum.of 应支持所有常量按 value 往返反查
     */
    @Test
    void baseEnumOfRoundTripForAllConstants() {
        for (MenuTypeEnum item : MenuTypeEnum.values()) {
            Optional<MenuTypeEnum> hit = BaseEnum.of(MenuTypeEnum.class, item.getValue());
            Assertions.assertTrue(hit.isPresent(), item + " 应能按 value 反查");
            Assertions.assertSame(item, hit.get());
        }
    }

    /**
     * BaseEnum.of 对未知取值应返回空 Optional
     */
    @Test
    void baseEnumOfReturnsEmptyForUnknownValue() {
        Assertions.assertFalse(BaseEnum.of(MenuTypeEnum.class, 99).isPresent());
    }

    /**
     * 枚举应标注 @EnumDict 且名称为「系统菜单类型」
     */
    @Test
    void annotatedAsEnumDict() {
        EnumDict dict = MenuTypeEnum.class.getAnnotation(EnumDict.class);
        Assertions.assertNotNull(dict);
        Assertions.assertEquals("系统菜单类型", dict.name());
        Assertions.assertTrue(dict.enabled());
    }
}
