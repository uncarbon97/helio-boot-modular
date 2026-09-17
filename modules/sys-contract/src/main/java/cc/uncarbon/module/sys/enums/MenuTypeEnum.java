package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import cc.uncarbon.module.commons.enumdict.EnumDict;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;


/**
 * 预置系统菜单类型枚举
 */
@EnumDict(name = "菜单类型")
@AllArgsConstructor
@Getter
public enum MenuTypeEnum implements BaseEnum<Integer> {

    /**
     * 可以认为是父级菜单
     */
    DIR(0, "目录"),

    /**
     * 可以认为是子菜单
     */
    MENU(1, "菜单"),

    /**
     * 可以认为是页内按钮
     */
    BUTTON(2, "按钮"),

    /**
     * 外链
     */
    EXTERNAL_LINK(3, "外链"),;

    @EnumValue
    private final Integer value;
    private final String label;

    /**
     * 所有菜单类型
     */
    public static List<MenuTypeEnum> all() {
        return Arrays.asList(DIR, MENU, BUTTON, EXTERNAL_LINK);
    }

    /**
     * 用于系统管理-侧边菜单的几种菜单类型
     */
    public static List<MenuTypeEnum> forAdminSide() {
        return Arrays.asList(DIR, MENU, EXTERNAL_LINK);
    }

}
