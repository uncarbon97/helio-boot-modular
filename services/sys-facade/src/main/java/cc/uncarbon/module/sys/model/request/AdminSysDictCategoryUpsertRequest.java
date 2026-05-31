package cc.uncarbon.module.sys.model.request;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 系统管理-字典分类-新增/修改
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysDictCategoryUpsertRequest implements Serializable {

    @Schema(description = "主键ID", title = "仅修改时使用")
    private Long id;

    @Schema(description = "字典编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 255, message = "【字典编码】最长255位")
    @NotBlank(message = "字典编码必填")
    private String code;

    @Schema(description = "字典名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 255, message = "【字典名称】最长255位")
    @NotBlank(message = "字典名称必填")
    private String name;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态必填")
    private EnabledStatusEnum status;

    @Schema(description = "字典描述")
    @Size(max = 255, message = "【字典描述】最长255位")
    private String description;

}
