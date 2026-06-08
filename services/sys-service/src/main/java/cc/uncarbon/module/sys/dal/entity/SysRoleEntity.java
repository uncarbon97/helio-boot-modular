package cc.uncarbon.module.sys.dal.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractTenantGenericEntity;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.enums.SysRoleFlagEnum;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
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
import java.util.List;


/**
 * 系统角色
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "sys_role")
public class SysRoleEntity extends AbstractTenantGenericEntity {

	@Serial
	private static final long serialVersionUID = 1L;


	@Schema(description = "主键ID")
	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

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
	 * 解析角色特殊标记为 {@link SysRoleFlagEnum} 集合
	 */
	public List<SysRoleFlagEnum> resolveFlags() {
		if (CharSequenceUtil.isEmpty(flags)) {
			return List.of();
		}
		CharSequenceUtil.split(flags, StrPool.COMMA).stream().map()
	}

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
