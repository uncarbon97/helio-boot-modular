package cc.uncarbon.module.tenant.biz;

import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import cc.uncarbon.framework.helium.tenant.props.HeliumTenantProperties;
import cc.uncarbon.module.tenant.facade.TenantConfigFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 租户配置门面
 */
@RequiredArgsConstructor
@Service
public class TenantConfigFacadeImpl implements TenantConfigFacade {

    private final HeliumTenantProperties props;


    @Override
    public boolean isTenantEnabled() {
        return props.doesTenantEnabled();
    }

    @Override
    public TenantLoginModeEnum getLoginMode() {
        return props.getLoginMode();
    }
}
