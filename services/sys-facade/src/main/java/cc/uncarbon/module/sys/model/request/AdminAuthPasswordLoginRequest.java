package cc.uncarbon.module.sys.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 后台管理-密码登录
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminAuthPasswordLoginRequest implements Serializable {

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 5, max = 16, message = "账号最短{min}位，最长{max}位")
    @NotBlank(message = "账号必填")
    private String pin;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 5, max = 20, message = "密码最短{min}位，最长{max}位")
    @NotBlank(message = "密码必填")
    private String pwd;

    /**
     * 启用多租户有效
     */
    @Schema(description = "租户编码")
    private String tenantCode;

    /**
     * 启用验证码有效
     */
    @Schema(description = "验证码唯一标识")
    private String captchaId;

    /**
     * 启用验证码有效
     */
    @Schema(description = "验证码答案")
    private String captchaAnswer;

}
