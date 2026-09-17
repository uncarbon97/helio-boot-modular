package cc.uncarbon.module.sys.model.request;

import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 系统用户-后台管理-重置密码
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysUserResetPasswordRequest implements Serializable {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "随机新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 16, max = 64, message = "随机新密码最短16位，最长64位")
    @NotBlank(message = "随机新密码必填")
    private String randomPassword;

    @Schema(description = "要求用户下次登录时修改密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "要求用户下次登录时修改密码必填")
    private YesOrNoEnum mustChangePassword;

}
