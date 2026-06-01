package cc.uncarbon.module.tenant.service;

import java.util.Collection;
import java.util.Set;

/**
 * 租户套餐-菜单关联
 */
public interface TenantPackageMenuRelationService {

    /**
     * 根据套餐Ids取菜单Ids
     */
    Set<Long> listMenuIdsByPackageIds(Collection<Long> packageIds) throws IllegalArgumentException;

    /**
     * 绑定套餐ID与菜单ID关联关系，增量更新
     */
    void cleanAndBind(Long packageId, Collection<Long> menuIds);
}
