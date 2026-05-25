package cc.uncarbon.module.sys.entity;

import cc.uncarbon.framework.crud.entity.HeliumBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;


/**
 * 系统用户-角色关联关系
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "sys_user_role_relation")
public class SysUserRoleRelationEntity extends HeliumBaseEntity<Long> {

	@Schema(description = "用户ID")
	@TableField(value = "user_id")
	private Long userId;

	@Schema(description = "角色ID")
	@TableField(value = "role_id")
	private Long roleId;

}
