package cc.uncarbon.module.file.model.query;


import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.base.page.PageQuery;
import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件存储点-后台管理-分页查询
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminFileStorageListQuery implements Serializable, PageQuery {

    @Schema(description = "分页查询参数")
    private PageParam pageParam;

    @Schema(description = "时间区间起")
    private LocalDateTime beginAt;

    @Schema(description = "时间区间止")
    private LocalDateTime endAt;

    @Schema(description = "存储点编码")
    private String code;

    @Schema(description = "存储点名称")
    private String name;

    @Schema(description = "存储点类型")
    private Integer type;

    @Schema(description = "主存储点标识")
    private YesOrNoEnum primaryFlag;

}
