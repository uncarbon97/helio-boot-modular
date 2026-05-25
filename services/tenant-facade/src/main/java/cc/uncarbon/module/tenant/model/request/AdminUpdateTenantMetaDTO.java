package cc.uncarbon.module.tenant.model.request;

import cc.uncarbon.framework.core.enums.EnabledStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;


/**
 * 系统管理-修改租户主数据
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminUpdateTenantMetaDTO implements Serializable {

    @Schema(description = "主键ID", hidden = true, title = "仅修改时使用")
    private Long id;

    @Schema(description = "租户编码")
    @Size(max = 100, message = "【租户编码】最长100位")
    private String code;

    @Schema(description = "租户名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 50, message = "【租户名称】最长50位")
    @NotBlank(message = "租户名称必填")
    private String name;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态必填")
    private EnabledStatusEnum status;

    @Schema(description = "所属租户套餐ID")
    private Long packageId;

}
