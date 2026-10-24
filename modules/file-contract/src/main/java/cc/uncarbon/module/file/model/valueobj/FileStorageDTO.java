package cc.uncarbon.module.file.model.valueobj;


import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.module.commons.model.valueobj.AuditUserDisplayAware;
import cc.uncarbon.module.file.enums.StoragePlatformTypeEnum;
import cc.uncarbon.module.file.model.setting.storage.FileStorageSetting;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;

/**
 * 文件存储点
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FileStorageDTO implements Serializable, AuditUserDisplayAware {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "创建时刻")
    private Instant createdAt;

    @Schema(description = "创建者")
    private String createdBy;

    @Schema(description = "更新时刻")
    private Instant updatedAt;

    @Schema(description = "更新者")
    private String updatedBy;

    @Schema(description = "存储点编码")
    private String code;

    @Schema(description = "存储点名称")
    private String name;

    @Schema(description = "存储平台类型")
    private StoragePlatformTypeEnum platformType;

    @Schema(description = "配置属性")
    private FileStorageSetting settingBody;

    @Schema(description = "主存储点标识")
    private YesOrNoEnum primaryFlag;

}
