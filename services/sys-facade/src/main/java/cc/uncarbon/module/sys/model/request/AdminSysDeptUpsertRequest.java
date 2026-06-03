package cc.uncarbon.module.sys.model.request;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 部门-新增/修改
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysDeptUpsertRequest implements Serializable {

    @Schema(description = "主键ID", title = "仅修改时使用")
    private Long id;

    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 50, message = "部门名称最长50位")
    @NotBlank(message = "部门名称必填")
    private String name;

    @Schema(description = "上级部门ID(根部门设置为0)")
    private Long parentId;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态")
    private EnabledStatusEnum status;

}
