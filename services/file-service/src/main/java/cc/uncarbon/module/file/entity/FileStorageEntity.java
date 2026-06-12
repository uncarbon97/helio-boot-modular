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


/**
 * 文件存储点
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "file_storage")
public class FileStorageEntity extends AbstractTenantGenericEntity {

    private static final long serialVersionUID = 1L;


    @Schema(description = "存储点编码")
    @TableField(value = "code")
    private String code;

    @Schema(description = "存储点名称")
    @TableField(value = "name")
    private String name;

    @Schema(description = "存储点类型")
    @TableField(value = "type")
    private Integer type;

    @Schema(description = "配置属性")
    @TableField(value = "config_json")
    private String configJson;

    @Schema(description = "主存储点标识")
    @TableField(value = "primary_flag")
    private Integer primaryFlag;

}
