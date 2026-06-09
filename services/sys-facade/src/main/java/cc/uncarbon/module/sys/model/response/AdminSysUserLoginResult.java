package cc.uncarbon.module.sys.model.response;

import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 后台管理-登录
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysUserLoginResult implements Serializable {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "账号")
    private String pin;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phoneNo;

    @Schema(description = "关联角色IDs")
    private List<Long> roleIds;

    @Schema(description = "关联角色编码")
    private List<String> roleCodes;

    @Schema(description = "关联菜单权限")
    private Set<String> permissions;

    @Schema(description = "角色-权限 Map，用于更新缓存")
    private Map<Long, Set<String>> rolePermissionMap;

    @Schema(description = "租户上下文")
    private TenantContext tenantContext;

}
