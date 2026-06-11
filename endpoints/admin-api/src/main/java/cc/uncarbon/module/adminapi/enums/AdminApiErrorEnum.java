package cc.uncarbon.module.adminapi.enums;

import cc.uncarbon.framework.helium.base.enums.ErrorCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 后台管理 API 端点错误枚举
 * 错误码格式 [A][BB][CCC]
 * [BB] 本枚举内固定为 04，表示后台管理 API 端点
 * [CCC] 按具体错误区分
 */
@AllArgsConstructor
@Getter
public enum AdminApiErrorEnum implements ErrorCodeEnum {

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
