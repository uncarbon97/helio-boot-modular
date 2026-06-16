package cc.uncarbon.module.sys.model.response;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 租户用户-新增
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TenantUserCreateResult implements Serializable {

    @Schema(description = "新的用户ID")
    private Long newUserId;

    @Schema(description = "是否是租户管理员")
    private boolean tenantAdmin;

}
