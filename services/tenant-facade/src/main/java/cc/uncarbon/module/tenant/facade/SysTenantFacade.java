package cc.uncarbon.module.tenant.facade;

import cc.uncarbon.module.tenant.model.request.AdminCreateTenantRequest;
import cc.uncarbon.module.tenant.model.request.AdminUpdateTenantMetaRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaKickOutUsersBO;

import java.util.Collection;

/**
 * 系统租户解耦层，用于解决循环依赖
 */
public interface SysTenantFacade {

    /**
     * 系统管理-新增
     */
    Long adminCreate(AdminCreateTenantRequest request);

    /**
     * 系统管理-修改
     */
    TenantMetaKickOutUsersBO adminUpdate(AdminUpdateTenantMetaRequest request);

    /**
     * 系统管理-删除
     */
    TenantMetaKickOutUsersBO adminDelete(Collection<Long> ids);

}
