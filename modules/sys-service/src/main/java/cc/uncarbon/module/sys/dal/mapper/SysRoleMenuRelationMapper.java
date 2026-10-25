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

    /**
     * 根据角色ID，删除关联
     */
    default int deleteByRoleId(Long roleId) {
        if (roleId == null) {
            return 0;
        }
        return delete(new LambdaQueryWrapper<SysRoleMenuRelationEntity>()
                .eq(SysRoleMenuRelationEntity::getRoleId, roleId));
    }

    /**
     * 根据角色ID，删除不在指定菜单ID集合内的关联
     */
    default int deleteByRoleIdAndMenuIdNotIn(Long roleId, Collection<Long> menuIds) {
        return delete(new LambdaQueryWrapper<SysRoleMenuRelationEntity>()
                .eq(SysRoleMenuRelationEntity::getRoleId, roleId)
                .notIn(SysRoleMenuRelationEntity::getMenuId, menuIds));
    }

    /**
     * 根据菜单IDs，删除关联
     */
    default int deleteByMenuIds(Collection<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return 0;
        }
        return delete(new LambdaQueryWrapper<SysRoleMenuRelationEntity>()
                .in(SysRoleMenuRelationEntity::getMenuId, menuIds));
    }
}
