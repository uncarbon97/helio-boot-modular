package cc.uncarbon.module.sys.dal.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractRelationEntity;
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
 * 系统角色-菜单关联关系
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "sys_role_menu_relation")
public class SysRoleMenuRelationEntity extends AbstractRelationEntity {

	@Serial
	private static final long serialVersionUID = 1L;


	@Schema(description = "主键ID")
	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	@Schema(description = "角色ID")
	@TableField(value = "role_id")
	private Long roleId;

	@Schema(description = "菜单ID")
	@TableField(value = "menu_id")
	private Long menuId;

	public static SysRoleMenuRelationEntity of(Long roleId, Long menuId) {
		SysRoleMenuRelationEntity ret = new SysRoleMenuRelationEntity();
		ret.roleId = roleId;
		ret.menuId = menuId;
		return ret;
	}
}
