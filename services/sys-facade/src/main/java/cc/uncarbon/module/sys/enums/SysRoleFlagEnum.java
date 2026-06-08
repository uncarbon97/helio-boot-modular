package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;


/**
 * 系统角色特殊标记枚举
 */
@AllArgsConstructor
@Getter
public enum SysRoleFlagEnum implements BaseEnum<String> {

    SUPER_ADMIN("super_admin", "超级管理员"),
    TENANT_ADMIN("tenant_admin", "租户管理员"),

    ;@EnumValue
    private final String value;
    private final String label;

    public static SysRoleFlagEnum of(String value) {
        Arrays.stream(SysRoleFlagEnum.class.getEnumConstants())
                .filter()
    }

}
