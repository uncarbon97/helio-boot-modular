package cc.uncarbon.module.sys.model.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Collection;

/**
 * 租户成员-拉入本租户（USER_FIRST）
 *
 * <p>目标租户恒取当前会话租户上下文，不接受显式指定</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TenantUserJoinRequest implements Serializable {


    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账号不能为空")
    private String pin;

    @Schema(description = "初始角色IDs；为空则不绑定角色")
    private Collection<Long> roleIds;

}
