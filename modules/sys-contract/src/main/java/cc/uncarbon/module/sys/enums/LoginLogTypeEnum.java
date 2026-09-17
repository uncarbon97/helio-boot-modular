package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 登录日志类型枚举
 */
@AllArgsConstructor
@Getter
public enum LoginLogTypeEnum implements BaseEnum<Integer> {

    PASSWORD_LOGIN(1, "密码登录"),
    LOGOUT(2, "登出"),

    ;@EnumValue
    private final Integer value;
    private final String label;

}
