package cc.uncarbon.module.tenant.model.request;

import cn.hutool.core.lang.RegexPool;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 租户-新增
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminTenantCreateRequest implements Serializable {


    @Schema(description = "租户编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100, message = "租户编码最长{max}位")
    @NotBlank(message = "租户编码必填")
    private String code;

    @Schema(description = "租户名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100, message = "租户名称最长{max}位")
    @NotBlank(message = "租户名称必填")
    private String name;

    @Schema(description = "租户管理员账号", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 5, max = 16, message = "租户管理员账号最短{min}位，最长{max}位")
    @NotBlank(message = "租户管理员账号必填")
    private String tenantAdminPin;

    @Schema(description = "租户管理员初始密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 8, max = 20, message = "租户管理员初始密码最短{min}位，最长{max}位")
    @NotBlank(message = "租户管理员初始密码必填")
    private String tenantAdminPwd;

    @Schema(description = "租户管理员邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(message = "租户管理员邮箱格式不正确", regexp = RegexPool.EMAIL)
    @Size(max = 255, message = "租户管理员邮箱最长{max}位")
    @NotBlank(message = "租户管理员邮箱必填")
    private String tenantAdminEmail;

    @Schema(description = "租户管理员手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(message = "租户管理员手机号格式不正确", regexp = RegexPool.MOBILE)
    @Size(max = 20, message = "租户管理员手机号最长{max}位")
    @NotBlank(message = "租户管理员手机号必填")
    private String tenantAdminPhoneNo;

    @Schema(description = "所属租户套餐ID")
    private Long packageId;

}
