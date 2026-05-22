package cc.uncarbon.module.sys.entity;

import cc.uncarbon.framework.core.enums.EnabledStatusEnum;
import cc.uncarbon.framework.crud.entity.HelioBaseEntity;
import cc.uncarbon.module.sys.constant.SysConstant;
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
 * 系统角色
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "sys_role")
public class SysRoleEntity extends HelioBaseEntity<Long> {

	@Schema(description = "角色编码")
	@TableField(value = "code")
	private String code;

	@Schema(description = "角色名称")
	@TableField(value = "name")
	private String name;

	@Schema(description = "角色描述")
	@TableField(value = "description")
	private String description;

	@Schema(description = "状态")
	@TableField(value = "status")
	private EnabledStatusEnum status;

	@Schema(description = "角色特殊标记")
	@TableField(value = "flags")
	private String flags;

	/**
	 * 角色实例可被视为超级管理员
	 */
	public boolean isSuperAdmin() {
		return SysConstant.SUPER_ADMIN_ROLE_ID.equals(getId()) || SysConstant.SUPER_ADMIN_ROLE_CODE.equalsIgnoreCase(getCode());
	}

	/**
	 * 角色实例可被视为租户管理员
	 */
	public boolean isTenantAdmin() {
		return SysConstant.TENANT_ADMIN_ROLE_CODE.equalsIgnoreCase(getCode());
	}

}
