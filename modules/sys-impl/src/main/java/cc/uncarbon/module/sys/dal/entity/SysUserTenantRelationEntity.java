package cc.uncarbon.module.sys.dal.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractTenantRelationEntity;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
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
 * 系统用户-租户关联关系（用户优先模式 USER_FIRST 专用）
 * 租户优先模式 TENANT_FIRST 下不写此表，用户归属以 sys_user.tenant_id 单值为准
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "sys_user_tenant_relation")
public class SysUserTenantRelationEntity extends AbstractTenantRelationEntity {

	@Serial
	private static final long serialVersionUID = 1L;


	@Schema(description = "主键ID")
	@TableId(type = IdType.AUTO)
	private Long id;

	@Schema(description = "用户ID")
	@TableField(value = "user_id")
	private Long userId;

	@Schema(description = "是否默认租户")
	@TableField(value = "default_flag")
	private YesOrNoEnum defaultFlag;

	@Schema(description = "状态")
	@TableField(value = "status")
	private EnabledStatusEnum status;

}
