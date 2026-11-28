package cc.uncarbon.module.tenant.util;

import cc.uncarbon.module.tenant.constant.TenantConstant;
import lombok.experimental.UtilityClass;

import java.util.Objects;

@UtilityClass
public class TenantUtil {

    /**
     * 是否平台自营域口径（含无租户上下文的场景）
     */
    public static boolean isPlatformDomain(Long tenantId) {
        return tenantId == null || Objects.equals(tenantId, TenantConstant.PLATFORM_TENANT_ID);
    }

}
