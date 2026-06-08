package cc.uncarbon.module.tenant.service;

import java.util.Collection;
import java.util.List;

/**
 * 租户套餐-菜单关联
 */
public interface TenantPackageMenuRelationService {

    /**
     * 根据套餐 ID 列举菜单 IDs
     */
    List<Long> listMenuIdsByPackage(long packageId);

    /**
     * 绑定套餐ID与菜单ID关联关系，增量更新
     */
    void cleanAndBind(long packageId, Collection<Long> menuIds);

}
