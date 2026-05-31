package cc.uncarbon.module.sys.model.valueobj;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 系统登录日志BO
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SysLoginLogBO implements Serializable {

    @Schema(description = "创建时刻")
    private LocalDateTime createdAt;

    @Schema(description = "登录日志类型")
    private Integer loginLogType;

    @Schema(description = "用户账号")
    private String userPin;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户类型编码")
    private String userTypeCode;

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
