package cc.uncarbon.module.tenant.dal.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractGenericEntity;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;


/**
 * 租户套餐
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "tenant_package")
public class TenantPackageEntity extends AbstractGenericEntity {

    @Serial
    private static final long serialVersionUID = 1L;


    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "套餐编码")
    @TableField(value = "code")
    private String code;

    @Schema(description = "套餐名称")
    @TableField(value = "name")
    private String name;

    @Schema(description = "状态")
    @TableField(value = "status")
    private EnabledStatusEnum status;

    @Schema(description = "套餐描述")
    @TableField(value = "description")
    private String description;

}
