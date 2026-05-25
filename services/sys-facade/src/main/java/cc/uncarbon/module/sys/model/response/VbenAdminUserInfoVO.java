package cc.uncarbon.module.sys.model.response;

import cc.uncarbon.framework.core.enums.GenderEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 系统用户 for VbenAdmin
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class VbenAdminUserInfoVO implements Serializable {

    @Schema(description = "账号")
    private String pin;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "最后登录时刻")
    private LocalDateTime lastLoginAt;

    @Schema(description = "性别")
    private GenderEnum gender;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phoneNo;

    @Schema(description = "头像URL")
    private String avatar;

}
