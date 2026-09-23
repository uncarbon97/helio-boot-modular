package cc.uncarbon.module.sys.dal.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractTenantRelationEntity;
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
 * 系统用户-租户关联关系
 *
 * <p>用户租户归属关系的<b>唯一真源</b>；sys_user.tenant_id 为投影列
 * （TENANT_FIRST=归属租户，USER_FIRST=当前激活租户），由服务层同事务双写维护。</p>
 *
 * <p>本表位于 ignored-tables（平台跨租户读写），tenant_id 由业务显式写入。</p>
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

	@Schema(description = "成员资格状态；1=启用 0=禁用")
	@TableField(value = "status")
	private EnabledStatusEnum status;


	public static SysUserTenantRelationEntity of(Long tenantId, Long userId, EnabledStatusEnum status) {
		var ret = new SysUserTenantRelationEntity()
				.setUserId(userId)
				.setStatus(status);
		ret.setTenantId(tenantId);
		return ret;
	}
}
