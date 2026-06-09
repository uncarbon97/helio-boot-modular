package cc.uncarbon.module.sys.model.request;


import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 增加租户角色
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AppendTenantRoleRequest implements Serializable {


    @Schema(description = "所属租户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private long tenantId;

    @Schema(description = "角色编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private EnabledStatusEnum status;

    @Schema(description = "是否是租户管理员", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean tenantAdmin;

}
