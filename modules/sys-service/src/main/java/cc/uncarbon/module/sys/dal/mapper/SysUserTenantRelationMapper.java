package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.module.sys.dal.entity.SysUserTenantRelationEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户-租户关联关系
 */
@Mapper
public interface SysUserTenantRelationMapper extends BaseMapper<SysUserTenantRelationEntity> {

    /**
     * 删除指定租户内的用户成员关系
     */
    default void deleteByTenantIdAndUserId(long tenantId, long userId) {
        delete(new LambdaQueryWrapper<SysUserTenantRelationEntity>()
                .eq(SysUserTenantRelationEntity::getTenantId, tenantId)
                .eq(SysUserTenantRelationEntity::getUserId, userId)
        );
    }

    /**
     * 删除用户在所有租户内的成员关系
     */
    default void deleteByUserId(long userId) {
        delete(new LambdaQueryWrapper<SysUserTenantRelationEntity>()
                .eq(SysUserTenantRelationEntity::getUserId, userId)
        );
    }
}
