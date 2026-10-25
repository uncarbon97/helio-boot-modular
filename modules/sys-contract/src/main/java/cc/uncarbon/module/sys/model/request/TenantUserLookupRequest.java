package cc.uncarbon.module.sys.model.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 租户成员-全局找人（USER_FIRST 模式下，拉入本租户前置查询）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TenantUserLookupRequest implements Serializable {


    @Schema(description = "账号")
    private String pin;

    @Schema(description = "完整手机号")
    @Pattern(message = "手机号格式有误（仅支持中国大陆 11 位手机号）", regexp = "^1\\d{10}$")
    @Size(max = 20, message = "手机号最长{max}位")
    @ToString.Exclude
    private String phoneNo;

}
