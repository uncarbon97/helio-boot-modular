package cc.uncarbon.module.sys.model.valueobj;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 租户成员-全局找人结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TenantUserLookupVO implements Serializable {


    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "账号")
    private String pin;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phoneNo;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "是否已是本租户成员")
    private boolean memberFlag;

}
