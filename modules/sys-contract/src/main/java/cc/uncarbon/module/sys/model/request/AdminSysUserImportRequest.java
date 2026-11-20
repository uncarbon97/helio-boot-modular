package cc.uncarbon.module.sys.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;


/**
 * 系统用户-后台管理-批量导入参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AdminSysUserImportRequest implements Serializable {

    @Schema(description = "统一初始密码明文（所有行共用，不写入文件）", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 5, max = 20, message = "初始密码最短{min}位，最长{max}位")
    @NotBlank(message = "初始密码必填")
    @ToString.Exclude
    private String initPwd;

    @Schema(description = "导入后是否直接启用账号")
    private boolean autoEnable;

    @Schema(description = "统一绑定的角色IDs（可空）")
    private List<Long> defaultRoleIds;

    @Schema(description = "统一所属部门ID（可空）")
    private Long defaultDeptId;

}
