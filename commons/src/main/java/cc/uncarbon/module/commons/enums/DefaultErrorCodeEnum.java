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
    [BB] 本枚举内固定为 00，表示未分类
    [CCC] 按具体错误区分
     */

    // A 开头错误码，表示一般性错误，如用户输入有误
    A00001("A00001", "重复数据：{}"),

    ;private final String errorCode;
    private final String errorMsgFriendly;

}
