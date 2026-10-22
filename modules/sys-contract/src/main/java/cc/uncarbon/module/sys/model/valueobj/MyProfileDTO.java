package cc.uncarbon.module.sys.model.valueobj;

import cc.uncarbon.framework.helium.db.enums.GenderEnum;
import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;


/**
 * 用户资料（本人视角，含联系方式明文）
 *
 * <p>明文字段 @ToString.Exclude 不进日志</p>
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MyProfileDTO implements Serializable {

    @Schema(description = "账号")
    private String pin;

    @Schema(description = "昵称")
    @ToString.Exclude
    private String nickname;

    @Schema(description = "最后登录时刻")
    private Instant lastLoginAt;

    @Schema(description = "性别")
    private GenderEnum gender;

    @Schema(description = "邮箱")
    @ToString.Exclude
    private String email;

    @Schema(description = "手机号")
    @ToString.Exclude
    private String phoneNo;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "要求用户下次登录时修改密码")
    private YesOrNoEnum mustChangePassword;

}
