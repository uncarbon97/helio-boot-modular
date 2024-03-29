package cc.uncarbon.module.sys.entity;

import cc.uncarbon.framework.crud.entity.HelioBaseEntity;
import cc.uncarbon.module.sys.enums.SysLogStatusEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;


/**
 * 系统日志
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "sys_log")
public class SysLogEntity extends HelioBaseEntity<Long> {

	@ApiModelProperty(value = "用户ID")
	@TableField(value = "user_id")
	private Long userId;

	@ApiModelProperty(value = "用户账号")
	@TableField(value = "username")
	private String username;

	@ApiModelProperty(value = "操作内容")
	@TableField(value = "operation")
	private String operation;

	@ApiModelProperty(value = "请求方法")
	@TableField(value = "method")
	private String method;

	@ApiModelProperty(value = "请求参数")
	@TableField(value = "params")
	private String params;

	@ApiModelProperty(value = "IP地址")
	@TableField(value = "ip")
	private String ip;

	@ApiModelProperty(value = "状态")
	@TableField(value = "status")
	private SysLogStatusEnum status;

	@ApiModelProperty(value = "错误原因堆栈")
	@TableField(value = "error_stacktrace")
	private String errorStacktrace;

	@ApiModelProperty(value = "用户UA")
	@TableField(value = "user_agent")
	private String userAgent;

	@ApiModelProperty(value = "IP地址属地-国家或地区名")
	@TableField(value = "ip_location_region_name")
	private String ipLocationRegionName;

	@ApiModelProperty(value = "IP地址属地-省级行政区名")
	@TableField(value = "ip_location_province_name")
	private String ipLocationProvinceName;

	@ApiModelProperty(value = "IP地址属地-市级行政区名")
	@TableField(value = "ip_location_city_name")
	private String ipLocationCityName;

	@ApiModelProperty(value = "IP地址属地-县级行政区名")
	@TableField(value = "ip_location_district_name")
	private String ipLocationDistrictName;

}
