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
 * 后台管理-修改当前用户密码
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminUpdateMyPwdRequest implements Serializable {

    @Schema(description = "原密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "原密码必填")
    private String old;

    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 8, max = 20, message = "新密码最短{min}位，最长{max}位")
    @NotBlank(message = "新密码必填")
    private String neo;

    @Schema(description = "确认密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 8, max = 20, message = "确认密码最短{min}位，最长{max}位")
    @NotBlank(message = "确认密码必填")
    private String confirmNeo;

}
