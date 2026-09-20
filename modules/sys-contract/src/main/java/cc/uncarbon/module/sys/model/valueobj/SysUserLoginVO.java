package cc.uncarbon.module.sys.model.valueobj;

import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;


/**
 * 登录后返回的字段
 * 用于返回给前端
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SysUserLoginVO implements Serializable {

    @Schema(description = "token值")
    private String token;

    @Schema(description = "对应角色")
    private Collection<String> roles;

    @Schema(description = "拥有权限")
    private Collection<String> permissions;

    @Schema(description = "租户上下文；null=平台视角（超级管理员）或个人空间（用户优先模式无归属）")
    private TenantContext tenantContext;

    @Schema(description = "是否为平台视角（超级管理员未切换租户）")
    private Boolean platformView;

    @Schema(description = "可选租户列表；用户优先模式且用户属多个租户时返回，供前端租户切换器使用")
    private List<TenantContext> tenantOptions;

}
