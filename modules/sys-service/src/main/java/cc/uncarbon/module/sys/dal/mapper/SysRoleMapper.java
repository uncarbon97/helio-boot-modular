package cc.uncarbon.module.sys.dal.mapper;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统角色
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRoleEntity> {

    /**
     * 根据角色IDs，过滤出处于启用状态的角色IDs
     *
     * @param roleIds 角色IDs
     * @return 处于启用状态的角色IDs
     */
    default Set<Long> listEnabledRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Set.of();
        }
        return selectList(new LambdaQueryWrapper<SysRoleEntity>()
                        // 仅取主键ID
                        .select(SysRoleEntity::getId)
                        // 值相符
                        .in(SysRoleEntity::getId, roleIds)
                        .eq(SysRoleEntity::getStatus, EnabledStatusEnum.ENABLED)
                ).stream().map(SysRoleEntity::getId).collect(Collectors.toSet());
    }
}
