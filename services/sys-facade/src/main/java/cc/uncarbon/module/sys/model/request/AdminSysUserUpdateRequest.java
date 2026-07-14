package cc.uncarbon.module.sys.model.request;

import cc.uncarbon.framework.helium.db.enums.GenderEnum;
import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cn.hutool.core.lang.RegexPool;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 系统用户-后台管理-修改
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysUserUpdateRequest implements Serializable {

    @Schema(description = "主键ID", title = "仅修改时使用")
    private Long id;

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 5, max = 16, message = "账号最短{min}位，最长{max}位")
    @NotBlank(message = "账号必填")
    private String pin;

    @Schema(description = "要求用户下次登录时修改密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "要求用户下次登录时修改密码必填")
    private YesOrNoEnum requireNewPwdFlag;

    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 20, message = "昵称最长{max}位")
    @NotBlank(message = "昵称必填")
    private String nickname;

    @Schema(description = "性别", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "性别必填")
    private GenderEnum gender;

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(message = "邮箱格式有误", regexp = RegexPool.EMAIL)
    @Size(max = 255, message = "邮箱最长{max}位")
    @NotBlank(message = "邮箱必填")
    private String email;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(message = "手机号格式有误", regexp = RegexPool.MOBILE)
    @Size(max = 20, message = "手机号最长{max}位")
    @NotBlank(message = "手机号必填")
    private String phoneNo;

}
