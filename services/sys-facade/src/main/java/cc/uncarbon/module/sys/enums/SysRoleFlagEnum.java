package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;


/**
 * 系统角色特殊标记枚举
 */
@AllArgsConstructor
@Getter
public enum SysRoleFlagEnum implements BaseEnum<String> {

    BUILTIN("builtin", "内置"),

    ;@EnumValue
    private final String value;
    private final String label;

    public static SysRoleFlagEnum of(String value) {
        return Arrays.stream(SysRoleFlagEnum.class.getEnumConstants())
                .filter(item -> Objects.equals(item.getValue(), value))
                .findFirst().orElse(null);
    }

}
