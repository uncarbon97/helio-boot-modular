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

    ADMIN_USER("ADMIN_USER", "系统管理用户"),

    ;private final String value;
    private final String label;

}
