package cc.uncarbon.module.sys.model.valueobj;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 后台管理-当前会话租户信息
 * 供前端租户切换器与视角横幅渲染
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminTenantContextVO implements Serializable {

    @Schema(description = "当前生效租户编码；null=平台视角或个人空间")
    private String tenantCode;

    @Schema(description = "当前生效租户名称；null=平台视角或个人空间")
    private String tenantName;

    @Schema(description = "是否为平台视角（超级管理员未切换租户）")
    private Boolean firstPartyView;

    @Schema(description = "当前会话是否处于切换后的租户视角")
    private Boolean switched;

}
