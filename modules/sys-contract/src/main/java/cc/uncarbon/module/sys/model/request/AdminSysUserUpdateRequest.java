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
 * 系统用户-后台管理-修改
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class AdminSysUserUpdateRequest extends AdminUpdateMyProfileRequest {

    @Schema(description = "主键ID", title = "仅修改时使用")
    private Long id;

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 5, max = 16, message = "账号最短{min}位，最长{max}位")
    @NotBlank(message = "账号必填")
    private String pin;

}
