package cc.uncarbon.module.sys.model.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Collection;

/**
 * 租户角色-绑定菜单
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TenantRoleBindMenuRequest implements Serializable {


    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private long tenantId;

    @Schema(description = "租户编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantCode;

    @Schema(description = "角色ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private long roleId;

    @Schema(description = "菜单IDs；如果为空则视为解除所有绑定")
    private Collection<Long> menuIds;

}
