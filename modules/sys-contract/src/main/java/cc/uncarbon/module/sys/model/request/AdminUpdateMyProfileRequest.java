package cc.uncarbon.module.sys.model.request;

import cc.uncarbon.framework.helium.db.enums.GenderEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 后台管理-更新个人资料
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminUpdateMyProfileRequest implements Serializable {


    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 20, message = "昵称最长{max}位")
    @NotBlank(message = "昵称必填")
    @ToString.Exclude
    private String nickname;

    @Schema(description = "性别", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "性别必填")
    private GenderEnum gender;

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(message = "邮箱格式有误", regexp = "^[\\x21-\\x7E]+@[\\x21-\\x7E]+$")
    @Size(max = 254, message = "邮箱最长{max}位（仅支持 ASCII 字符）")
    @NotBlank(message = "邮箱必填")
    @ToString.Exclude
    private String email;

    @Schema(description = "手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(message = "手机号格式有误（仅支持中国大陆 11 位手机号）", regexp = "^1\\d{10}$")
    @Size(max = 20, message = "手机号最长{max}位")
    @NotBlank(message = "手机号必填")
    @ToString.Exclude
    private String phoneNo;

}
