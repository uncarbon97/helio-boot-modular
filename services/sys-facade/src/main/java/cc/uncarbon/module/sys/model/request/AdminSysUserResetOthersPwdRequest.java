package cc.uncarbon.module.sys.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;


/**
 * 系统用户-后台管理-重置其他用户密码
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysUserResetOthersPwdRequest implements Serializable {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "随机新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 16, max = 64, message = "随机新密码最短16位，最长64位")
    @NotBlank(message = "随机新密码必填")
    private String randomPassword;

}
