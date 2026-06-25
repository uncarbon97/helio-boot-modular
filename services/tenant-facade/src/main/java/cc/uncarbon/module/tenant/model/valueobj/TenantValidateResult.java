package cc.uncarbon.module.tenant.model.valueobj;

import cc.uncarbon.framework.helium.base.errorcode.BuiltinErrorCodeEnum;
import cc.uncarbon.framework.helium.base.errorcode.StructuredErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.NonNull;

import java.io.Serializable;


/**
 * 租户校验结果
 */
@Accessors(chain = true)
@RequiredArgsConstructor
@Data
public class TenantValidateResult implements Serializable {

    /**
     * 租户是否有效
     * 如果未使用多租户特性，也是 true
     */
    private final boolean valid;

    /**
     * 校验失败错误枚举
     */
    private final StructuredErrorCode errorCode;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "租户编码")
    private String tenantCode;

    @Schema(description = "租户名称")
    private String tenantName;

    public static TenantValidateResult pass() {
        return new TenantValidateResult(true, BuiltinErrorCodeEnum.OK);
    }

    public static TenantValidateResult pass(@NonNull TenantMetaDTO meta) {
        return pass()
                .setTenantId(meta.getId())
                .setTenantName(meta.getName())
                .setTenantCode(meta.getCode());
    }

    public static TenantValidateResult fail(StructuredErrorCode errorCode) {
        return new TenantValidateResult(false, errorCode);
    }

}
