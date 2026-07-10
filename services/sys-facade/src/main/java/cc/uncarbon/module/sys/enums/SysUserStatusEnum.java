package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import cc.uncarbon.module.commons.enumdict.EnumDict;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 系统用户状态枚举
 */
@EnumDict(name = "用户状态")
@AllArgsConstructor
@Getter
public enum SysUserStatusEnum implements BaseEnum<Integer> {

    BANNED(0, "禁用"),
    ENABLED(1, "正常"),

    ;@EnumValue
    private final Integer value;
    private final String label;

}
