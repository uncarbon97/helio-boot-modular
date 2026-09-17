package cc.uncarbon.module.sys.model.response;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 租户角色-新增
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TenantRoleCreateResult implements Serializable {

    @Schema(description = "新的角色ID")
    private Long newRoleId;

    @Schema(description = "是否是租户管理员")
    private boolean tenantAdmin;

}
