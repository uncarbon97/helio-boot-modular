package cc.uncarbon.module.tenant.model.request;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Collection;


/**
 * 租户套餐-后台管理-新增/修改
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminTenantPackageUpsertRequest implements Serializable {

    @Schema(description = "主键ID", title = "仅修改时使用")
    private Long id;

    @Schema(description = "套餐编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100, message = "套餐编码最长{max}位")
    @NotBlank(message = "套餐编码必填")
    private String code;

    @Schema(description = "套餐名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100, message = "套餐名称最长{max}位")
    @NotBlank(message = "套餐名称必填")
    private String name;

    @Schema(description = "状态")
    private EnabledStatusEnum status;

    @Schema(description = "套餐描述")
    @Size(max = 255, message = "套餐描述最长{max}位")
    private String description;

    @Schema(description = "菜单ID数组")
    private Collection<Long> menuIds;

}
