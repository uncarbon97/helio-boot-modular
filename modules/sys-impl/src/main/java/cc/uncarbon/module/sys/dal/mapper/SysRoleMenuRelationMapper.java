package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.module.sys.dal.entity.SysRoleMenuRelationEntity;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

/**
 * 系统角色-可见菜单关联
 */
@Mapper
public interface SysRoleMenuRelationMapper extends BaseMapper<SysRoleMenuRelationEntity> {

    /**
     * 根据角色IDs，删除关联
     */
    default int deleteByRoleIds(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return 0;
        }
        return delete(new LambdaQueryWrapper<SysRoleMenuRelationEntity>()
                .in(SysRoleMenuRelationEntity::getRoleId, roleIds));
    }
}
