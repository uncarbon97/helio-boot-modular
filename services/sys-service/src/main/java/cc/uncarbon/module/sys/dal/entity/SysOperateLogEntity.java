package cc.uncarbon.module.sys.dal.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractTenantGenericEntity;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
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
import java.time.LocalDateTime;


/**
 * 系统操作日志
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "sys_operate_log")
public class SysOperateLogEntity extends AbstractTenantGenericEntity {

	@Serial
	private static final long serialVersionUID = 1L;


	@Schema(description = "主键ID")
	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	// 禁用更新
	@TableField(exist = false)
	private LocalDateTime updatedAt;
	@TableField(exist = false)
	private String updatedBy;

	@Schema(description = "主模块")
	@TableField(value = "main_module")
	private String mainModule;

	@Schema(description = "副模块")
	@TableField(value = "sub_module")
	private String subModule;

	@Schema(description = "业务号")
	@TableField(value = "biz_no")
	private String bizNo;

	@Schema(description = "操作内容")
	@TableField(value = "operation")
	private String operation;

	@Schema(description = "额外业务信息")
	@TableField(value = "biz_extra")
	private String bizExtra;

	@Schema(description = "用户ID")
	@TableField(value = "user_id")
	private Long userId;

	@Schema(description = "用户类型编码")
	@TableField(value = "user_type_code")
	private String userTypeCode;

	@Schema(description = "HTTP请求方法")
	@TableField(value = "request_method")
	private String requestMethod;

	@Schema(description = "HTTP请求路径")
	@TableField(value = "request_path")
	private String requestPath;

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
