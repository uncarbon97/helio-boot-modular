package cc.uncarbon.module.sys.facade;

import cc.uncarbon.module.sys.model.request.AdminInsertTenantMetaDTO;
import cc.uncarbon.module.sys.model.request.AdminUpdateTenantMetaDTO;
import cc.uncarbon.module.sys.model.response.TenantMetaKickOutUsersBO;

import java.util.Collection;

/**
 * 系统租户解耦层，用于解决循环依赖
 */
public interface SysTenantFacade {

    /**
     * 系统管理-新增
     */
    Long adminInsert(AdminInsertTenantMetaDTO dto);

    /**
     * 系统管理-编辑
     */
    TenantMetaKickOutUsersBO adminUpdate(AdminUpdateTenantMetaDTO dto);

    /**
     * 系统管理-删除
     */
    TenantMetaKickOutUsersBO adminDelete(Collection<Long> ids);

}
