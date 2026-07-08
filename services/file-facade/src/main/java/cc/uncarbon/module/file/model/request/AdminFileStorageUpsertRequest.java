package cc.uncarbon.module.file.model.request;


import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.module.file.enums.PlatformTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Map;

/**
 * 文件存储点-后台管理-新增/修改
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminFileStorageUpsertRequest implements Serializable {

    @Schema(description = "主键ID", title = "仅修改时使用")
    private Long id;

    @Schema(description = "存储点编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100, message = "存储点编码最长{max}位")
    @NotBlank(message = "存储点编码必填")
    private String code;

    @Schema(description = "存储点名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 50, message = "存储点名称最长{max}位")
    @NotBlank(message = "存储点名称必填")
    private String name;

    @Schema(description = "存储平台类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "存储平台类型必填")
    private PlatformTypeEnum platformType;

    @Schema(description = "配置属性", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "配置属性必填")
    private Map<String, Object> settingBody;

    @Schema(description = "主存储点标识", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "主存储点标识必填")
    private YesOrNoEnum primaryFlag;

}
