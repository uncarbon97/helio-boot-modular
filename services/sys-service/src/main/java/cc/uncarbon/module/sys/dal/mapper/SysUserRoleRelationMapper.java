package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.module.sys.dal.entity.SysUserRoleRelationEntity;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统用户-角色关联关系
 */
@Mapper
public interface SysUserRoleRelationMapper extends BaseMapper<SysUserRoleRelationEntity> {

    /**
     * 根据用户ID，查询关联的角色IDs
     */
    default List<Long> listRoleIdsByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return selectList(
                new LambdaQueryWrapper<SysUserRoleRelationEntity>()
                        .select(SysUserRoleRelationEntity::getRoleId)
                        .eq(SysUserRoleRelationEntity::getUserId, userId)
        ).stream().map(SysUserRoleRelationEntity::getRoleId).toList();
    }

    /**
     * 根据角色IDs，查询关联的用户IDs
     */
    default Set<Long> listUserIdsByRoles(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Set.of();
        }

        return selectList(
                new LambdaQueryWrapper<SysUserRoleRelationEntity>()
                        .select(SysUserRoleRelationEntity::getUserId)
                        .in(SysUserRoleRelationEntity::getRoleId, roleIds)
        ).stream().map(SysUserRoleRelationEntity::getUserId).collect(Collectors.toSet());
    }
}
