package cc.uncarbon.module.commons.enums;

import cc.uncarbon.framework.helium.base.enums.ErrorCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 默认错误码枚举类
 */
@AllArgsConstructor
@Getter
public enum DefaultErrorCodeEnum implements ErrorCodeEnum {

    /*
    错误码格式 [A][BB][CCC]
    [A] 固定为 A，表示框架内置错误
    [BB] 固定为 00
    [CCC] 按具体错误区分
     */

    A00000("A00000", "{}"),

    ;private final String errorCode;
    private final String errorMsgFriendly;

}
