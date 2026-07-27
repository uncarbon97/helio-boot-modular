package cc.uncarbon.module.sys.dal.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractTenantGenericEntity;
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
 * 部门
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "sys_dept")
public class SysDeptEntity extends AbstractTenantGenericEntity {

	@Serial
	private static final long serialVersionUID = 1L;


	@Schema(description = "主键ID")
	@TableId(type = IdType.AUTO)
	private Long id;

	@Schema(description = "部门名称")
	@TableField(value = "name")
	private String name;

	@Schema(description = "上级部门ID(根部门设置为0)")
	@TableField(value = "parent_id")
	private Long parentId;

	@Schema(description = "排序")
	@TableField(value = "sort")
	private Integer sort;

	@Schema(description = "状态")
	@TableField(value = "status")
	private EnabledStatusEnum status;

}
