package cc.uncarbon.module.sys.dal.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractTenantGenericEntity;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serial;


/**
 * 字典项
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "sys_dict_item")
public class SysDictItemEntity extends AbstractTenantGenericEntity {

    @Serial
    private static final long serialVersionUID = 1L;


    @Schema(description = "主键ID")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "所属分类ID")
    @TableField(value = "category_id")
    private Long categoryId;

    @Schema(description = "字典项编码")
    @TableField(value = "code")
    private String code;

    @Schema(description = "字典项值")
    @TableField(value = "value")
    private String value;

    @Schema(description = "字典项标签")
    @TableField(value = "label")
    private String label;

    @Schema(description = "状态")
    @TableField(value = "status")
    private EnabledStatusEnum status;

    @Schema(description = "排序")
    @TableField(value = "sort")
    private Integer sort;

    @Schema(description = "字典项描述")
    @TableField(value = "description")
    private String description;

}
