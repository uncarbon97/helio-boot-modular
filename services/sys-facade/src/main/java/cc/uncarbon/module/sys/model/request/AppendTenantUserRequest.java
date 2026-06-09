package cc.uncarbon.module.sys.model.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 增加租户用户
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AppendTenantUserRequest implements Serializable {


    @Schema(description = "所属租户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private long tenantId;

    @Schema(description = "租户名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantName;

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pin;

    @Schema(description = "明文密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pwdPlain;

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNo;

    @Schema(description = "是否是租户管理员", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean tenantAdmin;

}
