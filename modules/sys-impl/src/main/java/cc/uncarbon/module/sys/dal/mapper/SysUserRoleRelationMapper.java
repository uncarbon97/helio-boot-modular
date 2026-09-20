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
        return selectList(new LambdaQueryWrapper<SysUserRoleRelationEntity>()
                .select(SysUserRoleRelationEntity::getRoleId)
                .eq(SysUserRoleRelationEntity::getUserId, userId)
        ).stream().map(SysUserRoleRelationEntity::getRoleId).toList();
    }

    /**
     * 根据 (用户ID, 租户ID)，查询关联的角色IDs
     * 用户优先模式下同一用户在不同租户持有不同角色，快照需按租户限定
     * <p>本表参与行级租户隔离（不在忽略表清单）；本方法用于忽略隔离的作用域内（如无租户上下文的会话重建），故显式拼接租户条件</p>
     */
    default List<Long> listRoleIdsByUserAndTenant(Long userId, Long tenantId) {
        if (userId == null || tenantId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<SysUserRoleRelationEntity>()
                .select(SysUserRoleRelationEntity::getRoleId)
                .eq(SysUserRoleRelationEntity::getUserId, userId)
                .eq(SysUserRoleRelationEntity::getTenantId, tenantId)
        ).stream().map(SysUserRoleRelationEntity::getRoleId).toList();
    }

    /**
     * 根据角色IDs，查询关联的用户IDs
     */
    default Set<Long> listUserIdsByRoles(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Set.of();
        }

        return selectList(new LambdaQueryWrapper<SysUserRoleRelationEntity>()
                .select(SysUserRoleRelationEntity::getUserId)
                .in(SysUserRoleRelationEntity::getRoleId, roleIds)
        ).stream().map(SysUserRoleRelationEntity::getUserId).collect(Collectors.toSet());
    }
}
