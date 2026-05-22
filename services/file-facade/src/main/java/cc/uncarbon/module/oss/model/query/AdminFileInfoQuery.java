package cc.uncarbon.module.oss.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 平台管理-分页列表上传文件信息 DTO
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminFileInfoQuery implements Serializable {

    @Schema(description = "原始文件名(关键词)")
    private String originalFilename;

    @Schema(description = "扩展名")
    private String extendName;

    @Schema(description = "文件主分类")
    private String category;

    @Schema(description = "时间区间起")
    private LocalDateTime beginAt;

    @Schema(description = "时间区间止")
    private LocalDateTime endAt;

}
