package cc.uncarbon.module.sys.model.request;

import cc.uncarbon.module.sys.model.query.AdminSysOperateLogListQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


/**
 * 系统操作日志-后台管理-批量导出
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class AdminSysOperateLogExportRequest extends AdminSysOperateLogListQuery {

    @Schema(description = "是否包含来源UA列；默认不包含")
    private Boolean includeUserAgent;

}
