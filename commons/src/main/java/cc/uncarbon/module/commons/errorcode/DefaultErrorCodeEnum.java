package cc.uncarbon.module.commons.errorcode;

import cc.uncarbon.framework.helium.base.errorcode.StructuredErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 默认错误码枚举
 * 错误码格式 [A][BB][CCC]
 * [BB] 本枚举内固定为 {@link ErrorCodeBizGroup#UNCLASSIFIED}
 * [CCC] 具体错误代号
 */
@AllArgsConstructor
@Getter
public enum DefaultErrorCodeEnum implements StructuredErrorCode {

    /*
     A 开头错误码，表示一般性错误，如用户输入有误
     */
    A00001("重复数据"),
    A00002("不存在数据"),

    ;private final String errorMsgFriendly;

}
