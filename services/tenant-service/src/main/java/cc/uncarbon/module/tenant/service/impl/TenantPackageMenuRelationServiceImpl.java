package cc.uncarbon.module.tenant.service.impl;

import cc.uncarbon.module.tenant.dal.entity.TenantPackageMenuRelationEntity;
import cc.uncarbon.module.tenant.dal.mapper.TenantPackageMenuRelationMapper;
import cc.uncarbon.module.tenant.service.TenantPackageMenuRelationService;
import cn.hutool.core.collection.CollUtil;
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


    @Override
    public List<Long> listMenuIdsByPackage(long packageId) {
        return tenantPackageMenuRelationMapper.listMenuIdsByPackage(packageId);
    }

    /**
     * 绑定套餐ID与菜单ID关联关系，增量更新
     *
     * @param packageId 套餐ID
     * @param menuIds   新菜单ID集合
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cleanAndBind(long packageId, Collection<Long> menuIds) {
        var menuIdsQuery = new LambdaQueryWrapper<TenantPackageMenuRelationEntity>()
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
            List<TenantPackageMenuRelationEntity> entityList = menuIds.stream()
                    .map(menuId -> TenantPackageMenuRelationEntity.of(packageId, menuId)).toList();
            tenantPackageMenuRelationMapper.insert(entityList);
        }
    }
}
