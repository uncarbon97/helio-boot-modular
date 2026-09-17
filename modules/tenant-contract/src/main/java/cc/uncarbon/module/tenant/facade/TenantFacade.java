package cc.uncarbon.module.tenant.facade;

import cc.uncarbon.module.tenant.model.valueobj.TenantValidateResult;
import org.jspecify.annotations.Nullable;

/**
 * 租户门面
 */
public interface TenantFacade {

    /**
     * 校验传入的租户编码
     */
    TenantValidateResult validateByCode(@Nullable String tenantCode);

}
