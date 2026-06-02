package cc.uncarbon.module.tenant.dal.mapper;

import cc.uncarbon.module.tenant.dal.entity.TenantPackageMenuRelationEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 租户套餐-菜单关联关系
 */
@Mapper
public interface TenantPackageMenuRelationMapper extends BaseMapper<TenantPackageMenuRelationEntity> {

    default List<Long> listMenuIdsByPackage(long packageId) {
        return selectList(
                new LambdaQueryWrapper<TenantPackageMenuRelationEntity>()
                        .select(TenantPackageMenuRelationEntity::getMenuId)
                        .eq(TenantPackageMenuRelationEntity::getPackageId, packageId)
        ).stream().map(TenantPackageMenuRelationEntity::getMenuId).toList();
    }
}
