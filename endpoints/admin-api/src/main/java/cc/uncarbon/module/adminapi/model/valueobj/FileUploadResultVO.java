package cc.uncarbon.module.adminapi.model.valueobj;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 上传文件结果 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FileUploadResultVO implements Serializable {

    @Schema(description = "外显文件ID")
    private String outFileId;

    @Schema(description = "存储文件名")
    private String filename;

    @Schema(description = "完整外链")
    private String url;

    @Schema(description = "原始文件名")
    private String originalFilename;

}
