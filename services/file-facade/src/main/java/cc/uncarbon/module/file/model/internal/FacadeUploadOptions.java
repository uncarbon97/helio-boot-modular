package cc.uncarbon.module.file.model.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 通过门面上传时的额外选项参数
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FacadeUploadOptions {


    @Schema(description = "原始文件名")
    private String originalFilename;

    @Schema(description = "MIME类型")
    private String contentType;

    @Schema(description = "SHA256")
    private String digestSha256;

    @Schema(description = "指定要上传到的平台名（null则取值默认平台）")
    private String platform;

    @Schema(description = "从对象存储服务器中下载时，以原始文件名命名（通过指定 metadata 实现）")
    private boolean useOriginalFilenameAsDownloadFileName;

}
