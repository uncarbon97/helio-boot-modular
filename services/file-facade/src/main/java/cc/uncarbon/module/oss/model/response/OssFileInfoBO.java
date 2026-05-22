package cc.uncarbon.module.oss.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 上传文件信息 BO
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OssFileInfoBO implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "创建时刻")
    private LocalDateTime createdAt;

    @Schema(description = "更新时刻")
    private LocalDateTime updatedAt;

    @Schema(description = "存储平台")
    private String storagePlatform;

    @Schema(description = "基础存储路径")
    private String storageBasePath;

    @Schema(description = "存储路径")
    private String storagePath;

    @Schema(description = "存储文件名")
    private String storageFilename;

    @Schema(description = "原始文件名")
    private String originalFilename;

    @Schema(description = "扩展名")
    private String extendName;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "MD5")
    private String md5;

    @Schema(description = "类别编号")
    private String classified;

    @Schema(description = "对象存储直链")
    private String directUrl;

    /**
     * 取完整的存储文件名（带扩展名）
     */
    public String getStorageFilenameFull() {
        return String.format("%s.%s", this.getStorageFilename(), this.getExtendName());
    }

    /**
     * 取完整的原始文件名（带扩展名）
     */
    public String getOriginalFilenameFull() {
        return String.format("%s.%s", this.getOriginalFilename(), this.getExtendName());
    }

}
