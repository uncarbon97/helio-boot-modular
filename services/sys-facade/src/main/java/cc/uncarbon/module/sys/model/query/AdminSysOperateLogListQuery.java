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
 * 系统操作日志-后台管理-分页查询
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysOperateLogListQuery implements Serializable, PageQuery {

    @Schema(description = "分页查询参数")
    private PageParam pageParam;

    @Schema(description = "时间区间起")
    private LocalDateTime beginAt;

    @Schema(description = "时间区间止")
    private LocalDateTime endAt;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "行为")
    private String behavior;

    @Schema(description = "业务号")
    private String bizNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "结果状态")
    private LogResultStatusEnum resultStatus;

}
