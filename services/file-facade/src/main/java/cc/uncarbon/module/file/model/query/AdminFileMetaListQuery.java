package cc.uncarbon.module.file.model.query;


import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.base.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件元数据-后台管理-分页查询
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminFileMetaListQuery implements Serializable, PageQuery {

    @Schema(description = "分页查询参数")
    private PageParam pageParam;

    @Schema(description = "时间区间起")
    private LocalDateTime beginAt;

    @Schema(description = "时间区间止")
    private LocalDateTime endAt;

    @Schema(description = "原始存储点ID")
    private Long storageId;

    @Schema(description = "存储点编码")
    private String storageCode;

    @Schema(description = "存储文件名")
    private String storageFilename;

    @Schema(description = "原始文件名")
    private String originalFilename;

    @Schema(description = "扩展名")
    private String extendName;

    @Schema(description = "文件主分类")
    private String category;

}
