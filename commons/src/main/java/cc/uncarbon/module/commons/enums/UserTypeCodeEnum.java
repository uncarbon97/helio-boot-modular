package cc.uncarbon.module.commons.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 用户类型编码枚举
 */
@AllArgsConstructor
@Getter
public enum UserTypeCodeEnum implements BaseEnum<String> {

    ADMIN_USER("后台管理用户"),
    APP_USER("C端用户"),

    ;private final String label;

    @Override
    public String getValue() {
        return name();
    }
}
