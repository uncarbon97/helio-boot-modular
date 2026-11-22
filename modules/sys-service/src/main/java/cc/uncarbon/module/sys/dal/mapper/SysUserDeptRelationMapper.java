package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.framework.helium.db.mybatisplus.conditions.query.LambdaQueryWrapperX;
import cc.uncarbon.module.sys.dal.entity.SysUserDeptRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户-部门关联关系
 */
@Mapper
public interface SysUserDeptRelationMapper extends BaseMapper<SysUserDeptRelationEntity> {

    /**
     * 根据用户ID，删除关联
     */
    default int deleteByUserId(Long userId) {
        if (userId == null) {
            return 0;
        }
        return delete(new LambdaQueryWrapperX<SysUserDeptRelationEntity>()
                .eq(SysUserDeptRelationEntity::getUserId, userId));
    }

    /**
     * 根据部门ID，删除关联
     */
    default int deleteByDeptId(long deptId) {
        return delete(new LambdaQueryWrapperX<SysUserDeptRelationEntity>()
                .eq(SysUserDeptRelationEntity::getDeptId, deptId));
    }
}
