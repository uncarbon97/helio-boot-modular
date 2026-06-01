package cc.uncarbon.module.tenant.service.impl;

import cc.uncarbon.module.tenant.dal.entity.TenantPackageMenuRelationEntity;
import cc.uncarbon.module.tenant.dal.mapper.TenantPackageMenuRelationMapper;
import cc.uncarbon.module.tenant.service.TenantPackageMenuRelationService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 租户套餐-菜单关联
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TenantPackageMenuRelationServiceImpl implements TenantPackageMenuRelationService {

    private final TenantPackageMenuRelationMapper tenantPackageMenuRelationMapper;


    /**
     * 根据套餐Ids取菜单Ids
     *
     * @param packageIds 套餐Ids
     * @return 菜单Ids
     */
    @Override
    public Set<Long> listMenuIdsByPackageIds(Collection<Long> packageIds) throws IllegalArgumentException {
        Assert.notEmpty(packageIds);

        Set<Long> ret = new HashSet<>(packageIds.size() << 4);
        for (Long packageId : packageIds) {
            ret.addAll(
                    tenantPackageMenuRelationMapper.selectList(
                            new QueryWrapper<TenantPackageMenuRelationEntity>()
                                    .lambda()
                                    .select(TenantPackageMenuRelationEntity::getMenuId)
                                    .eq(TenantPackageMenuRelationEntity::getPackageId, packageId)
                    ).stream().map(TenantPackageMenuRelationEntity::getMenuId).collect(Collectors.toSet()));
        }

        return ret;
    }

    /**
     * 绑定套餐ID与菜单ID关联关系，增量更新
     *
     * @param packageId 套餐ID
     * @param menuIds   新菜单ID集合
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanAndBind(Long packageId, Collection<Long> menuIds) {
        LambdaQueryWrapper<TenantPackageMenuRelationEntity> menuIdsQuery =
                new QueryWrapper<TenantPackageMenuRelationEntity>()
                        .lambda()
                        .select(TenantPackageMenuRelationEntity::getMenuId)
                        .eq(TenantPackageMenuRelationEntity::getPackageId, packageId);

        if (CollUtil.isEmpty(menuIds)) {
            tenantPackageMenuRelationMapper.delete(menuIdsQuery);
            return;
        }

        // 先删除不再需要的关联关系
        tenantPackageMenuRelationMapper.delete(
                new QueryWrapper<TenantPackageMenuRelationEntity>()
                        .lambda()
                        .eq(TenantPackageMenuRelationEntity::getPackageId, packageId)
                        .notIn(TenantPackageMenuRelationEntity::getMenuId, menuIds)
        );

        // 取出需要增量更新的部分
        Set<Long> existingMenuIds = tenantPackageMenuRelationMapper.selectList(menuIdsQuery)
                .stream().map(TenantPackageMenuRelationEntity::getMenuId)
                .collect(Collectors.toSet());
        menuIds.removeAll(existingMenuIds);

        if (CollUtil.isNotEmpty(menuIds)) {
            List<TenantPackageMenuRelationEntity> entityList = new ArrayList<>(menuIds.size());
            for (Long menuId : menuIds) {
                entityList.add(TenantPackageMenuRelationEntity.of(packageId, menuId));
            }
            tenantPackageMenuRelationMapper.insert(entityList);
        }
    }
}
