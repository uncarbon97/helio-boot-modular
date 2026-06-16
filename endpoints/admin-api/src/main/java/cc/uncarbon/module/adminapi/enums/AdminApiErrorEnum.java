package cc.uncarbon.module.adminapi.enums;

import cc.uncarbon.framework.helium.base.errorcode.StructuredErrorCode;
import cc.uncarbon.module.commons.errorcode.ErrorCodeBizGroup;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 后台管理 API 端点错误枚举
 * 错误码格式 [A][BB][CCC]
 * [BB] 本枚举内固定为 {@link ErrorCodeBizGroup#ADMIN_API}
 * [CCC] 具体错误代号
 */
@AllArgsConstructor
@Getter
public enum AdminApiErrorEnum implements StructuredErrorCode {

    /*
     A 开头错误码，表示一般性错误，如用户输入有误
     */
    A04001("验证码不正确，请重新输入"),

    /*
     B 开头错误码，表示本服务内部错误
     */
    B04001("生成验证码失败"),

    ;private final String errorMsgFriendly;

}
