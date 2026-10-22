package cc.uncarbon.module.sys.model.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 租户用户-新增
 *
 * <p>格式校验与 AdminSysUserUpdateRequest 同口径；明文/口令字段 @ToString.Exclude，不进任何日志</p>
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TenantUserCreateRequest implements Serializable {


    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private long tenantId;

    @Schema(description = "租户编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantCode;

    @Schema(description = "租户名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantName;

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pin;

    @Schema(description = "明文密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @ToString.Exclude
    private String pwdPlain;

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(message = "邮箱格式有误", regexp = "^[\\x21-\\x7E]+@[\\x21-\\x7E]+$")
    @Size(max = 254, message = "邮箱最长{max}位（仅支持 ASCII 字符）")
    @ToString.Exclude
    private String email;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(message = "手机号格式有误（仅支持中国大陆 11 位手机号）", regexp = "^1\\d{10}$")
    @Size(max = 20, message = "手机号最长{max}位")
    @ToString.Exclude
    private String phoneNo;

    @Schema(description = "是否是租户管理员", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean tenantAdmin;

}
