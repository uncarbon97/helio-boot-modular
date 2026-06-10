package cc.uncarbon.module.sys.model.request;

import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.module.commons.enums.UserTypeCodeEnum;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import cc.uncarbon.module.sys.enums.LoginLogTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 系统登录日志-新增
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SysLoginLogCreateRequest implements Serializable {

    @Schema(description = "登录日志类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private LoginLogTypeEnum loginLogType;

    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userPin;

    @Schema(description = "用户ID")
    private Long userId;

    /**
     * {@link UserTypeCodeEnum}
     */
    @Schema(description = "用户类型编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userTypeCode;

    @Schema(description = "事件发生时的 VisitorContext", requiredMode = Schema.RequiredMode.REQUIRED)
    private VisitorContext visitorContext;

    @Schema(description = "结果状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private LogResultStatusEnum resultStatus;

    @Schema(description = "失败原因文本")
    private String failedMsg;

}
