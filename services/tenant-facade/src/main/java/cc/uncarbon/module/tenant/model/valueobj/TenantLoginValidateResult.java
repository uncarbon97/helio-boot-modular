package cc.uncarbon.module.tenant.model.valueobj;

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
public class TenantLoginValidateResult implements Serializable {

    @Schema(description = "租户编码")
    private String code;

    @Schema(description = "租户名称")
    private String name;

    /**
     * 租户是否有效
     * 如果未使用多租户特性，会直接返回 true
     */
    private boolean validated;

    /**
     * 是否忽略
     * 如果未使用多租户特性，会直接返回 true
     */
    private boolean tenantIgnored;

}
