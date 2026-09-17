package cc.uncarbon.module.tenant.errorcode;

import cc.uncarbon.framework.helium.base.errorcode.StructuredErrorCode;
import cc.uncarbon.module.commons.errorcode.ErrorCodeBizGroup;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 租户管理错误码枚举
 * 错误码格式 [A][BB][CCC]
 * [BB] 本枚举内固定为 {@link ErrorCodeBizGroup#TENANT}
 * [CCC] 具体错误代号
 */
@AllArgsConstructor
@Getter
public enum TenantErrorCodeEnum implements StructuredErrorCode {

    /*
     A 开头错误码，表示一般性错误，如用户输入有误
     */
    A03001("租户不存在"),
    A03002("租户套餐非启用状态"),
    A03003("已有租户正在使用此套餐，无法禁用"),
    A03004("已存在相同的套餐编码"),
    A03005("已存在相同的租户编码"),
    A03006("租户已禁用"),

    /*
    B 开头错误码，表示本服务内部错误
    */
    B03001("验证租户编码失败"),

    ;private final String errorMsgFriendly;

}
