package cc.uncarbon.module.sys.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


/**
 * 系统用户-后台管理-新增
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysUserCreateRequest extends AdminSysUserUpdateRequest {

    @Schema(hidden = true)
    private Long id;

    @Schema(description = "初始密码明文", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 5, max = 20, message = "初始密码最短{min}位，最长{max}位")
    @NotBlank(message = "初始密码必填")
    private String initPwd;

    @Schema(description = "所属部门ID")
    private Long deptId;

}
