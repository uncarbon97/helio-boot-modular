package cc.uncarbon.module.sys.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 系统操作日志-新增
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SysOperateLogCreateRequest implements Serializable {

    @Schema(description = "主模块")
    private String mainModule;

    @Schema(description = "副模块")
    private String subModule;

    @Schema(description = "业务号")
    private String bizNo;

    @Schema(description = "操作内容")
    private String operation;

    @Schema(description = "额外业务信息")
    private String bizExtra;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户类型编码")
    private String userTypeCode;

    @Schema(description = "HTTP请求方法")
    private String requestMethod;

    @Schema(description = "HTTP请求路径")
    private String requestPath;

    @Schema(description = "来源IP地址")
    private String visitorIp;

    @Schema(description = "来源UA")
    private String visitorUserAgent;

    @Schema(description = "IP地址归属地")
    private String visitorIpLocation;

    @Schema(description = "结果状态")
    private Integer resultStatus;

    @Schema(description = "失败原因文本")
    private String failedMsg;

}
