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
    // DISABLED_TENANT(400, "所属租户已禁用"),

    // CANNOT_DELETE_TENANT_ADMIN_ROLE(403, "为减少脏数据，不建议直接删除租户管理员角色，需通过【删除租户】关联删除"),
    // CANNOT_BIND_MENUS_FOR_TENANT_ADMIN_ROLE(403, "无权为租户管理员绑定菜单"),

    // CANNOT_DELETE_PRIVILEGED_TENANT(403, "不能删除超级租户"),
    // NEED_DELETE_EXISTING_TENANT_ADMIN_ROLE(500, "租户ID {} 对应的租户管理员角色已存在，请使用超级管理员账号删除"),

    /*
    B 开头错误码，表示本服务内部错误
    */
    B03001("验证租户编码失败"),

    ;private final String errorMsgFriendly;

}
