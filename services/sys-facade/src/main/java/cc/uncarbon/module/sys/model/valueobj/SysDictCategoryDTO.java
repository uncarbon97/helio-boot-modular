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
 * 字典分类
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SysDictCategoryDTO implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "创建时刻")
    private LocalDateTime createdAt;

    @Schema(description = "更新时刻")
    private LocalDateTime updatedAt;

    @Schema(description = "字典编码")
    private String code;

    @Schema(description = "字典名称")
    private String name;

    @Schema(description = "状态")
    private EnabledStatusEnum status;

    @Schema(description = "字典描述")
    private String description;

}
