package cc.uncarbon.module.sys.model.query;

import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.base.page.PageQuery;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 系统登录日志-分页查询
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysLoginLogListQuery implements Serializable, PageQuery {

    @Schema(description = "分页查询参数")
    private PageParam pageParam;

    @Schema(description = "用户账号")
    private String userPin;

    @Schema(description = "结果状态")
    private LogResultStatusEnum resultStatus;

    @Schema(description = "时间区间起")
    private LocalDateTime beginAt;

    @Schema(description = "时间区间止")
    private LocalDateTime endAt;

}
