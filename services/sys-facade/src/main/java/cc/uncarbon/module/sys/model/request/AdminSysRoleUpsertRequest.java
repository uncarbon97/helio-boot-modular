package cc.uncarbon.module.sys.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 系统角色-后台管理-新增/修改
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysRoleUpsertRequest implements Serializable {

    @Schema(description = "主键ID", title = "仅修改时使用")
    private Long id;

    @Schema(description = "角色编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100, message = "角色编码最长{max}位")
    @NotBlank(message = "角色编码必填")
    private String code;

    @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100, message = "角色名称最长{max}位")
    @NotBlank(message = "角色名称必填")
    private String name;

    @Schema(description = "角色描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 255, message = "角色描述最长{max}位")
    private String description;

    public boolean inCreating() {
        return id == null;
    }

}
