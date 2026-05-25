package cc.uncarbon.module.sys.model.request;

import cc.uncarbon.module.sys.constant.SysConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Objects;


/**
 * 系统管理-新增/修改系统角色
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminInsertOrUpdateSysRoleDTO implements Serializable {

    @Schema(description = "主键ID", hidden = true, title = "仅修改时使用")
    private Long id;

    @Schema(description = "所属租户ID", hidden = true, title = "仅新增时使用")
    private Long tenantId;

    @Schema(description = "角色编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100, message = "【角色编码】最长100位")
    @NotBlank(message = "角色编码必填")
    private String code;

    @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 50, message = "【角色名称】最长50位")
    @NotBlank(message = "角色名称必填")
    private String name;

    /**
     * 是否用于创建新租户管理员角色
     */
    public boolean creatingNewTenantAdmin() {
        return Objects.nonNull(tenantId) && SysConstant.TENANT_ADMIN_ROLE_CODE.equalsIgnoreCase(code);
    }

}
