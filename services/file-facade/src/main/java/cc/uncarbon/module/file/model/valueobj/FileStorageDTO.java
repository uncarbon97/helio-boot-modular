package cc.uncarbon.module.file.model.valueobj;


import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.module.file.enums.PlatformTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件存储点
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FileStorageDTO implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "创建时刻")
    private LocalDateTime createdAt;

    @Schema(description = "更新时刻")
    private LocalDateTime updatedAt;

    @Schema(description = "存储点编码")
    private String code;

    @Schema(description = "存储点名称")
    private String name;

    @Schema(description = "存储平台类型")
    private PlatformTypeEnum platformType;

    @Schema(description = "配置属性")
    private String settingJson;

    @Schema(description = "主存储点标识")
    private YesOrNoEnum primaryFlag;

}
