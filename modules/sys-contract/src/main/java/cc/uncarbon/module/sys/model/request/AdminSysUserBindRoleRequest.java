package cc.uncarbon.module.sys.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Set;


/**
 * 系统用户-后台管理-绑定用户角色
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysUserBindRoleRequest implements Serializable {

    @Schema(description = "角色IDs；如果为空则视为解除所有绑定")
    private Set<Long> roleIds;

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户ID必填")
    private Long userId;

}
