package cc.uncarbon.module.sys.model.valueobj;

import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;


/**
 * 后台管理-当前会话租户信息
 * 供前端租户切换器与视角横幅渲染
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminTenantContextVO implements Serializable {

    @Schema(description = "当前生效租户ID；null=平台视角或个人空间")
    private Long tenantId;

    @Schema(description = "当前生效租户编码；null=平台视角或个人空间")
    private String tenantCode;

    @Schema(description = "当前生效租户名称；null=平台视角或个人空间")
    private String tenantName;

    @Schema(description = "是否为平台视角（超级管理员未切换租户）")
    private Boolean platformView;

    @Schema(description = "当前会话是否处于切换后的租户视角")
    private Boolean switched;

    @Schema(description = "切换前的租户上下文；null=切换前为平台视角或个人空间")
    private TenantContext originalTenantContext;

    @Schema(description = "切换时刻")
    private Instant switchedAt;

}
