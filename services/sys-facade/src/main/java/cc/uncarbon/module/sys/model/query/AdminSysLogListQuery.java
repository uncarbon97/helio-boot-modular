package cc.uncarbon.module.sys.model.query;

import cc.uncarbon.framework.core.constant.HeliumConstant;
import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.base.page.PageQuery;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 系统管理-分页查询系统日志
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysLogListQuery implements Serializable, PageQuery {

    @Schema(description = "分页查询参数")
    private PageParam pageParam;

    @Schema(description = "用户账号")
    private String username;

    @Schema(description = "操作内容")
    private String operation;

    @Schema(description = "结果状态")
    private Integer status;

    @Schema(description = "时间区间起")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = HeliumConstant.Jackson.DATE_TIME_FORMAT)
    @DateTimeFormat(pattern = HeliumConstant.Jackson.DATE_TIME_FORMAT)
    private LocalDateTime beginAt;

    @Schema(description = "时间区间止")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = HeliumConstant.Jackson.DATE_TIME_FORMAT)
    @DateTimeFormat(pattern = HeliumConstant.Jackson.DATE_TIME_FORMAT)
    private LocalDateTime endAt;

}
