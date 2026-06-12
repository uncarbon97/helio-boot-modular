package cc.uncarbon.module.sys.model.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Collection;

/**
 * 租户用户-绑定角色
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TenantUserBindRoleRequest implements Serializable {


    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private long tenantId;

    @Schema(description = "租户编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantCode;

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private long userId;

    @Schema(description = "角色IDs；如果为空则视为解除所有绑定")
    private Collection<Long> roleIds;

}
