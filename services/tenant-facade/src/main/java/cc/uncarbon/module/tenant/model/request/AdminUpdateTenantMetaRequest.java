package cc.uncarbon.module.tenant.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 租户-修改
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminUpdateTenantMetaRequest implements Serializable {


    @Schema(description = "主键ID", title = "仅修改时使用")
    private Long id;

    @Schema(description = "租户名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 50, message = "【租户名称】最长50位")
    @NotBlank(message = "租户名称必填")
    private String name;

    @Schema(description = "所属租户套餐ID")
    private Long packageId;

}
