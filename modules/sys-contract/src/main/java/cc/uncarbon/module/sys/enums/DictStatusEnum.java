package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import cc.uncarbon.module.commons.enumdict.EnumDict;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 字典状态枚举（分类与字典项通用）
 */
@EnumDict(name = "字典状态")
@AllArgsConstructor
@Getter
public enum DictStatusEnum implements BaseEnum<Integer> {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用"),
    DEPRECATED(2, "过时"),

    ;@EnumValue
    private final Integer value;
    private final String label;

}
