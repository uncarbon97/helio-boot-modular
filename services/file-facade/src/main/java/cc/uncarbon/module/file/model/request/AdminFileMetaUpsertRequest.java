package cc.uncarbon.module.file.model.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;

/**
 * 文件元数据-后台管理-新增/修改
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminFileMetaUpsertRequest implements Serializable {

    @Schema(description = "主键ID", title = "仅修改时使用")
    private Long id;

    @Schema(description = "原始存储点ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "原始存储点ID必填")
    private Long storageId;

    @Schema(description = "存储点编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100, message = "存储点编码最长{max}位")
    @NotBlank(message = "存储点编码必填")
    private String storageCode;

    @Schema(description = "存储点主目录路径", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 255, message = "存储点主目录路径最长{max}位")
    @NotBlank(message = "存储点主目录路径必填")
    private String storageBasePath;

    @Schema(description = "子目录路径", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 512, message = "存储路径最长{max}位")
    @NotBlank(message = "存储路径必填")
    private String subDirPath;

    @Schema(description = "存储文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 255, message = "存储文件名最长{max}位")
    @NotBlank(message = "存储文件名必填")
    private String storageFilename;

    @Schema(description = "原始文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 255, message = "原始文件名最长{max}位")
    @NotBlank(message = "原始文件名必填")
    private String originalFilename;

    @Schema(description = "扩展名", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 16, message = "扩展名最长{max}位")
    @NotBlank(message = "扩展名必填")
    private String extendName;

    @Schema(description = "文件大小", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文件大小必填")
    private Long fileSize;

    @Schema(description = "SHA256")
    @Size(max = 64, message = "SHA256最长{max}位")
    private String digestSha256;

    @Schema(description = "文件主分类")
    @Size(max = 255, message = "文件主分类最长{max}位")
    private String category;

    @Schema(description = "对象存储直链")
    @Size(max = 510, message = "对象存储直链最长{max}位")
    private String directUrl;

}
