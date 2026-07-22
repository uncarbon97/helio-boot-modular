package cc.uncarbon.module.sys.errorcode;

import cc.uncarbon.framework.helium.base.errorcode.StructuredErrorCode;
import cc.uncarbon.module.commons.errorcode.ErrorCodeBizGroup;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 系统管理错误码枚举
 * 错误码格式 [A][BB][CCC]
 * [BB] 本枚举内固定为 {@link ErrorCodeBizGroup#SYS}
 * [CCC] 具体错误代号
 */
@AllArgsConstructor
@Getter
public enum SysErrorCodeEnum implements StructuredErrorCode {

    /*
     A 开头错误码，表示一般性错误，如用户输入有误
     */
    A01001("账号或密码不正确"),
    A01002("用户处于禁用或锁定状态"),
    A01003("两次输入的密码不一致"),
    A01004("原密码有误"),
    A01005("当前用户没有可用角色"),
    A01006("当前角色没有可用菜单"),

    // 以下枚举用于角色的越权检查
    A01010("不能使用 {} 作为角色编码"),
    A01011("不能删除特殊角色"),
    A01012("不能删除自身角色"),
    A01013("不能变动特殊角色"),
    A01014("不能变动自身角色"),
    A01015("不得超越自身菜单权限"),

    // 以下枚举用于用户的越权检查
    A01020("不能对自身进行此操作"),
    A01021("不能该用户进行此操作"),
    A01022("不得超越自身角色权限"),
    A01023("不能删除特殊角色用户"),

    /*
     B 开头错误码，表示本服务内部错误
     */
    B01001("登录组件错误，请联系管理员"),

    ;private final String errorMsgFriendly;

}
