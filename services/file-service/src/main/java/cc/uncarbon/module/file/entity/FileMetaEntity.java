package cc.uncarbon.module.file.entity;

import cc.uncarbon.framework.helium.db.entity.AbstractTenantGenericEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;


/**
 * 文件元数据
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "file_meta")
public class FileMetaEntity extends AbstractTenantGenericEntity {

    @Serial
    private static final long serialVersionUID = 1L;


    @Schema(description = "原始存储点ID")
    @TableField(value = "storage_id")
    private Long storageId;

    @Schema(description = "存储点编码")
    @TableField(value = "storage_code")
    private String storageCode;

    @Schema(description = "基础存储路径")
    @TableField(value = "storage_base_path")
    private String storageBasePath;

    @Schema(description = "存储路径")
    @TableField(value = "storage_path")
    private String storagePath;

    @Schema(description = "存储文件名")
    @TableField(value = "storage_filename")
    private String storageFilename;

    @Schema(description = "原始文件名")
    @TableField(value = "original_filename")
    private String originalFilename;

    @Schema(description = "扩展名")
    @TableField(value = "extend_name")
    private String extendName;

    @Schema(description = "文件大小")
    @TableField(value = "file_size")
    private Long fileSize;

    @Schema(description = "SHA256")
    @TableField(value = "digest_sha256")
    private String digestSha256;

    @Schema(description = "文件主分类")
    @TableField(value = "category")
    private String category;

    @Schema(description = "对象存储直链")
    @TableField(value = "direct_url")
    private String directUrl;


    /**
     * 取完整的存储文件名（带扩展名）
     */
    public String getStorageFilenameFull() {
        return "%s.%s".formatted(getStorageFilename(), getExtendName());
    }

    /**
     * 取完整的原始文件名（带扩展名）
     */
    public String getOriginalFilenameFull() {
        return "%s.%s".formatted(getOriginalFilename(), getExtendName());
    }
}
