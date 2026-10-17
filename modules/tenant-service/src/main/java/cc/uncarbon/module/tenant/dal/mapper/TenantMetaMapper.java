package cc.uncarbon.module.tenant.dal.mapper;

import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.tenant.dal.entity.TenantMetaEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jspecify.annotations.NonNull;

/**
 * 租户主数据
 */
@Mapper
public interface TenantMetaMapper extends BaseMapper<TenantMetaEntity> {

    default void updateAdminUserId(long id, long userId) {
        updateById(new TenantMetaEntity().setId(id)
                .setAdminUserId(userId)
        );
    }

    default TenantMetaEntity selectByCode(@NonNull String code) {
        return selectOne(new LambdaQueryWrapper<TenantMetaEntity>()
                .eq(TenantMetaEntity::getCode, code)
                .last(SQLSegment.LIMIT_1)
        );
    }
}
