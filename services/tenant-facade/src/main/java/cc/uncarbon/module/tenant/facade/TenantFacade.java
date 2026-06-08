package cc.uncarbon.module.tenant.facade;

import cc.uncarbon.module.tenant.model.valueobj.TenantLoginValidateResult;

/**
 * 系统租户解耦层，用于解决循环依赖
 */
public interface TenantFacade {

    /**
     * 根据租户编码查询租户信息
     */
    TenantLoginValidateResult validateLoginByCode(String tenantCode);

}
