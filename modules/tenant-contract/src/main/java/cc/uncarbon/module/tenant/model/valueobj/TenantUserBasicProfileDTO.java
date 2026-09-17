package cc.uncarbon.module.tenant.model.valueobj;

import cc.uncarbon.framework.helium.db.enums.GenderEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 租户用户-基本资料
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TenantUserBasicProfileDTO implements Serializable {

    @Schema(description = "账号")
    private String pin;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "性别")
    private GenderEnum gender;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phoneNo;

}
