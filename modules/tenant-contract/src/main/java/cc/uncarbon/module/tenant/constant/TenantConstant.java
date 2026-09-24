package cc.uncarbon.module.tenant.constant;

import cc.uncarbon.framework.helium.tenant.constant.HeliumTenantConstant;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TenantConstant {

    /**
     * 平台自营域租户ID（第一方）
     * 沿用脚手架中的常量
     */
    public static final long FIRST_PARTY_TENANT_ID = HeliumTenantConstant.FALLBACK_TENANT_ID;

}
