package cc.uncarbon.module.commons.enums;

import cc.uncarbon.framework.helium.base.enums.ErrorCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 默认错误码枚举
 * 错误码格式 [A][BB][CCC]
 * [BB] 本枚举内固定为 00，表示不区分子模块
 * [CCC] 按具体错误区分
 */
@AllArgsConstructor
@Getter
public enum DefaultErrorCodeEnum implements ErrorCodeEnum {

    /*
     A 开头错误码，表示一般性错误，如用户输入有误
     */
    A00001("重复数据"),
    A00002("不存在数据"),

    ;private final String errorMsgFriendly;

    public String getErrorCode() {
        return name();
    }
}
