package cc.uncarbon.module.tenant.facade;

import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;

/**
 * 租户配置门面
 */
public interface TenantConfigFacade {

    /**
     * 是否启用多租户隔离
     */
    boolean isTenantEnabled();

    /**
     * 当前登录模式
     */
    TenantLoginModeEnum getLoginMode();

}
