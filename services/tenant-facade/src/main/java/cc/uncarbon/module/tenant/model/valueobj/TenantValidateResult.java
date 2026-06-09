package cc.uncarbon.module.tenant.model.valueobj;

import cc.uncarbon.framework.helium.base.enums.ErrorCodeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 租户校验结果
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TenantValidateResult implements Serializable {

    /**
     * 租户是否有效
     * 如果未使用多租户特性，会直接返回 true
     */
    private boolean valid;

    /**
     * 校验失败错误枚举
     */
    private ErrorCodeEnum errorCode;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "租户编码")
    private String tenantCode;

    @Schema(description = "租户名称")
    private String tenantName;

}
