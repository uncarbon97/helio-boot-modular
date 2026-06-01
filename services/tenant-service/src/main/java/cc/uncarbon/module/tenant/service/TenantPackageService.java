package cc.uncarbon.module.tenant.service;

import cc.uncarbon.module.tenant.dal.entity.TenantPackageEntity;

import java.util.Collection;
import java.util.List;

/**
 * 租户套餐
 */
public interface TenantPackageService {

    /**
     * 根据ID获取
     */
    TenantPackageEntity getById(Long id);

    /**
     * 根据ID集合批量获取
     */
    List<TenantPackageEntity> listByIds(Collection<Long> ids);

    /**
     * 根据ID删除
     */
    void deleteByIds(Collection<Long> ids);
}
