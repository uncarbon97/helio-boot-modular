package cc.uncarbon.module.sys.facade;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;

import java.util.Collection;
import java.util.List;

/**
 * 租户用户门面
 */
public interface TenantUserFacade {


    /**
     * 系统管理 - 取租户用户IDs
     *
     * @param tenantId    租户ID，非主键ID
     * @param statusEnums 仅保留符合指定状态的，可以为null
     */
    List<Long> listUserIdsByTenantId(Long tenantId, Collection<EnabledStatusEnum> statusEnums);

}
