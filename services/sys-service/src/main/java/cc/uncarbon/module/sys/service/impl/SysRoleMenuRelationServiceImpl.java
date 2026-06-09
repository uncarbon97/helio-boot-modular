package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.module.sys.dal.entity.SysRoleMenuRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMenuRelationMapper;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 系统角色-可见菜单关联
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysRoleMenuRelationServiceImpl implements SysRoleMenuRelationService {

    private final SysRoleMenuRelationMapper sysRoleMenuRelationMapper;


    @Override
    public Set<Long> listMenuIdsByRoles(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Set.of();
        }

        // aka * 16
        Set<Long> ret = new HashSet<>(roleIds.size() << 4);
        for (Long roleId : roleIds) {
            var menuIdsQuery =
                    new LambdaQueryWrapper<SysRoleMenuRelationEntity>()
                            .select(SysRoleMenuRelationEntity::getMenuId)
                            .eq(SysRoleMenuRelationEntity::getRoleId, roleId);
            ret.addAll(sysRoleMenuRelationMapper.selectList(menuIdsQuery).stream()
                            .map(SysRoleMenuRelationEntity::getMenuId).collect(Collectors.toSet()));
        }

        return ret;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cleanAndBind(Long roleId, Collection<Long> menuIds) {
        var menuIdsQuery =
                new LambdaQueryWrapper<SysRoleMenuRelationEntity>()
                        .select(SysRoleMenuRelationEntity::getMenuId)
                        .eq(SysRoleMenuRelationEntity::getRoleId, roleId);

        if (CollUtil.isEmpty(menuIds)) {
            // 清除绑定，直接删除所有关联关系就行
            sysRoleMenuRelationMapper.delete(menuIdsQuery);
            return;
        }

        // 先删除不再需要的关联关系
        sysRoleMenuRelationMapper.delete(
                new QueryWrapper<SysRoleMenuRelationEntity>()
                        .lambda()
                        .eq(SysRoleMenuRelationEntity::getRoleId, roleId)
                        .notIn(SysRoleMenuRelationEntity::getMenuId, menuIds)
        );

        // 取出需要增量更新的部分
        Set<Long> existingMenuIds = sysRoleMenuRelationMapper.selectList(menuIdsQuery)
                .stream().map(SysRoleMenuRelationEntity::getMenuId)
                .collect(Collectors.toSet());
        menuIds.removeAll(existingMenuIds);

        if (CollUtil.isNotEmpty(menuIds)) {
            // 批量插入需要增量更新的部分
            List<SysRoleMenuRelationEntity> entityList = menuIds.stream()
                    .map(menuId -> SysRoleMenuRelationEntity.of(roleId, menuId)).toList();
            sysRoleMenuRelationMapper.insert(entityList);
        }
    }
}
