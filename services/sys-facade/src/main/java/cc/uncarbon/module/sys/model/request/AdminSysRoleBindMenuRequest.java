package cc.uncarbon.module.sys.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Set;


/**
 * 系统角色-绑定菜单
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysRoleBindMenuRequest implements Serializable {

    @Schema(description = "菜单IDs；如果为空则视为解除所有绑定")
    private Set<Long> menuIds;

    @Schema(description = "角色ID", hidden = true)
    private Long roleId;

}
