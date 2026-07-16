package cc.uncarbon.module.tenant.biz;

import cc.uncarbon.framework.helium.tenant.props.HeliumTenantProperties;
import cc.uncarbon.module.tenant.errorcode.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaDTO;
import cc.uncarbon.module.tenant.model.valueobj.TenantValidateResult;
import cc.uncarbon.module.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 租户门面
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TenantFacadeImpl implements TenantFacade {

    private final TenantService tenantService;
    private final HeliumTenantProperties props;

    @Override
    public TenantValidateResult validateByCode(@Nullable String tenantCode) {
        if (!props.doesTenantEnabled()) {
            return TenantValidateResult.pass();
        }
        if (tenantCode != null) {
            TenantMetaDTO tenantMeta = tenantService.getByCode(tenantCode, false);
            if (Objects.nonNull(tenantMeta)) {
                return TenantValidateResult.pass(tenantMeta);
            }
        }
        return TenantValidateResult.fail(TenantErrorCodeEnum.A03001);
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */
}
