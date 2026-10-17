package cc.uncarbon.module.file.dal.entity;


import cc.uncarbon.framework.helium.db.constant.EntityField;
import cc.uncarbon.framework.helium.db.entity.AbstractGenericEntity;
import cc.uncarbon.framework.helium.db.entity.TenantEntity;
import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.module.file.enums.StoragePlatformTypeEnum;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 文件存储点
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName(value = "file_storage")
public class FileStorageEntity extends AbstractGenericEntity implements TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;


    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 兼容多租户特性
     */
    @Schema(description = "租户ID")
    @TableField(value = EntityField.TENANT_ID_COLUMN)
    private Long tenantId;

    @Schema(description = "存储点编码")
    @TableField(value = "code")
    private String code;

    @Schema(description = "存储点名称")
    @TableField(value = "name")
    private String name;

    @Schema(description = "存储平台类型")
    @TableField(value = "platform_type")
    private StoragePlatformTypeEnum platformType;

    @Schema(description = "配置属性")
    @TableField(value = "setting_json")
    private String settingJson;

    @Schema(description = "主存储点标识")
    @TableField(value = "primary_flag")
    private YesOrNoEnum primaryFlag;

}
