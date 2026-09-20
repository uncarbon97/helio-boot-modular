package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.dal.entity.SysUserTenantRelationEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 系统用户-租户关联关系（用户优先模式 USER_FIRST 专用）
 */
@Mapper
public interface SysUserTenantRelationMapper extends BaseMapper<SysUserTenantRelationEntity> {

    /**
     * 列举用户启用状态的租户关联；默认租户排最前
     */
    default List<SysUserTenantRelationEntity> listEnabledByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<SysUserTenantRelationEntity>()
                .eq(SysUserTenantRelationEntity::getUserId, userId)
                .eq(SysUserTenantRelationEntity::getStatus, EnabledStatusEnum.ENABLED)
                .orderByDesc(SysUserTenantRelationEntity::getDefaultFlag)
                .orderByAsc(SysUserTenantRelationEntity::getId)
        );
    }

}
