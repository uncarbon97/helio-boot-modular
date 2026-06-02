package cc.uncarbon.module.tenant.enums;

import cc.uncarbon.framework.helium.base.enums.ErrorCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 租户管理错误码枚举类
 */
@AllArgsConstructor
@Getter
public enum TenantErrorCodeEnum implements ErrorCodeEnum {


    /*
    错误码格式 [A][BB][CCC]
    [BB] 本枚举内固定为 03，表示租户管理子模块
    [CCC] 按具体错误区分
     */

    /*
     A 开头错误码，表示一般性错误，如用户输入有误
     */
    // INVALID_TENANT(400, "所属租户无效"),
    // DISABLED_TENANT(400, "所属租户已禁用"),

    // CANNOT_DELETE_TENANT_ADMIN_ROLE(403, "为减少脏数据，不建议直接删除租户管理员角色，需通过【删除租户】关联删除"),
    // CANNOT_BIND_MENUS_FOR_TENANT_ADMIN_ROLE(403, "无权为租户管理员绑定菜单"),

    // CANNOT_DELETE_PRIVILEGED_TENANT(403, "不能删除超级租户"),
    // NEED_DELETE_EXISTING_TENANT_ADMIN_ROLE(500, "租户ID {} 对应的租户管理员角色已存在，请使用超级管理员账号删除"),

    ;private final String errorCode;
    private final String errorMsgFriendly;

}
