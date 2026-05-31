package cc.uncarbon.module.tenant.facade;

import cc.uncarbon.module.tenant.model.request.AdminSysTenantUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.TenantMetaKickOutUsersBO;

import java.util.Collection;

/**
 * 系统租户解耦层，用于解决循环依赖
 */
public interface SysTenantFacade {

    /**
     * 系统管理-新增
     */
    Long adminCreate(AdminSysTenantUpsertRequest request);

    /**
     * 系统管理-修改
     */
    TenantMetaKickOutUsersBO adminUpdate(AdminSysTenantUpsertRequest request);

    /**
     * 系统管理-删除
     */
    TenantMetaKickOutUsersBO adminDelete(Collection<Long> ids);

}
