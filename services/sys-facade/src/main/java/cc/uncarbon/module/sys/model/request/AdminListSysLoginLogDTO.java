package cc.uncarbon.module.sys.model.request;

import cc.uncarbon.framework.core.constant.HeliumConstant;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 系统管理-分页列表系统登录日志
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminListSysLoginLogDTO implements Serializable {

    @Schema(description = "用户账号")
    private String userPin;

    @Schema(description = "登录日志类型")
    private Integer loginLogType;

    @Schema(description = "结果状态")
    private Integer resultStatus;

    @Schema(description = "时间区间起")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = HeliumConstant.Jackson.DATE_TIME_FORMAT)
    @DateTimeFormat(pattern = HeliumConstant.Jackson.DATE_TIME_FORMAT)
    private LocalDateTime beginAt;

    @Schema(description = "时间区间止")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = HeliumConstant.Jackson.DATE_TIME_FORMAT)
    @DateTimeFormat(pattern = HeliumConstant.Jackson.DATE_TIME_FORMAT)
    private LocalDateTime endAt;

}
