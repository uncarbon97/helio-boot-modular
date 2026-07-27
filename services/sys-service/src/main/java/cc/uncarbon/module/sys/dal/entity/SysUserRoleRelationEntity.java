package cc.uncarbon.module.sys.dal.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractTenantRelationEntity;
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
 * 系统用户-角色关联关系
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "sys_user_role_relation")
public class SysUserRoleRelationEntity extends AbstractTenantRelationEntity {

	@Serial
	private static final long serialVersionUID = 1L;


	@Schema(description = "主键ID")
	@TableId(type = IdType.AUTO)
	private Long id;

	@Schema(description = "用户ID")
	@TableField(value = "user_id")
	private Long userId;

	@Schema(description = "角色ID")
	@TableField(value = "role_id")
	private Long roleId;


	public static SysUserRoleRelationEntity of(Long userId, Long roleId) {
		SysUserRoleRelationEntity ret = new SysUserRoleRelationEntity();
		ret.userId = userId;
		ret.roleId = roleId;
		return ret;
	}
}
