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
 * 字典项-新增/修改
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysDictItemUpsertRequest implements Serializable {

    @Schema(description = "主键ID", title = "仅修改时使用")
    private Long id;

    @Schema(description = "所属分类ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "所属分类ID必填")
    private Long categoryId;

    @Schema(description = "字典项编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100, message = "字典项编码最长{max}位")
    @NotBlank(message = "字典项编码必填")
    private String code;

    @Schema(description = "字典项标签", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100, message = "字典项标签最长{max}位")
    @NotBlank(message = "字典项标签必填")
    private String label;

    @Schema(description = "字典项值", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 10000, message = "字典项值最长{max}位")
    @NotBlank(message = "字典项值必填")
    private String value;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态必填")
    private EnabledStatusEnum status;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排序必填")
    private Integer sort;

    @Schema(description = "字典项描述")
    @Size(max = 255, message = "字典项描述最长{max}位")
    private String description;

}
