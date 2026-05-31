package cc.uncarbon.module.sys.model.valueobj;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 字典项信息
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SysDictItemDTO implements Serializable {

    @Schema(description = "主键ID", title = "仅修改时使用")
    private Long id;

    @Schema(description = "创建时刻")
    private LocalDateTime createdAt;

    @Schema(description = "更新时刻")
    private LocalDateTime updatedAt;

    @Schema(description = "所属分类ID")
    private Long categoryId;

    @Schema(description = "字典项编码")
    private String code;

    @Schema(description = "字典项标签")
    private String label;

    @Schema(description = "字典项值")
    private String value;

    @Schema(description = "状态")
    private EnabledStatusEnum status;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "字典项描述")
    private String description;

}
