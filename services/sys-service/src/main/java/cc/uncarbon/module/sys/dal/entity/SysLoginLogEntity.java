package cc.uncarbon.module.sys.dal.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractTenantGenericEntity;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import cc.uncarbon.module.sys.enums.LoginLogTypeEnum;
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
 * 系统登录日志
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "sys_login_log")
public class SysLoginLogEntity extends AbstractTenantGenericEntity {

	@Serial
	private static final long serialVersionUID = 1L;


	@Schema(description = "主键ID")
	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	@Schema(description = "登录日志类型")
	@TableField(value = "login_log_type")
	private LoginLogTypeEnum loginLogType;

	@Schema(description = "用户账号")
	@TableField(value = "user_pin")
	private String userPin;

	@Schema(description = "用户ID")
	@TableField(value = "user_id")
	private Long userId;

	@Schema(description = "用户类型编码")
	@TableField(value = "user_type_code")
	private String userTypeCode;

	@Schema(description = "来源IP地址")
	@TableField(value = "visitor_ip")
	private String visitorIp;

	@Schema(description = "来源UA")
	@TableField(value = "visitor_user_agent")
	private String visitorUserAgent;

	@Schema(description = "IP地址归属地")
	@TableField(value = "visitor_ip_location")
	private String visitorIpLocation;

	@Schema(description = "结果状态")
	@TableField(value = "result_status")
	private LogResultStatusEnum resultStatus;

	@Schema(description = "失败原因文本")
	@TableField(value = "failed_msg")
	private String failedMsg;

}
