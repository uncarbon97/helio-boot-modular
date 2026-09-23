package cc.uncarbon.module.sys.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 后台管理-切换至租户
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminTenantSwitchEnterRequest implements Serializable {

    @Schema(description = "目标租户编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "租户编码必填")
    private String tenantCode;

}
