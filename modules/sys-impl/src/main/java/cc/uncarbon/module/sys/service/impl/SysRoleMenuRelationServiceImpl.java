package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.module.sys.dal.entity.SysRoleMenuRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMenuRelationMapper;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    @Override
    public Set<Long> listRoleIdsByMenus(Collection<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return Set.of();
        }
        return sysRoleMenuRelationMapper.selectList(new LambdaQueryWrapper<SysRoleMenuRelationEntity>()
                        .select(SysRoleMenuRelationEntity::getRoleId)
                        .in(SysRoleMenuRelationEntity::getMenuId, menuIds))
                .stream().map(SysRoleMenuRelationEntity::getRoleId).collect(Collectors.toSet());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cleanAndBind(Long roleId, Collection<Long> menuIds) {
        // 入参去重，避免插入重复关系行
        if (CollUtil.isNotEmpty(menuIds)) {
            menuIds = new LinkedHashSet<>(menuIds);
        }
        var menuIdsQuery = new LambdaQueryWrapper<SysRoleMenuRelationEntity>()
                .select(SysRoleMenuRelationEntity::getMenuId)
                .eq(SysRoleMenuRelationEntity::getRoleId, roleId);

        if (CollUtil.isEmpty(menuIds)) {
            // 清除绑定，直接删除所有关联关系就行
            sysRoleMenuRelationMapper.delete(menuIdsQuery);
            return;
        }

        // 先删除不再需要的关联关系
        sysRoleMenuRelationMapper.delete(new LambdaQueryWrapper<SysRoleMenuRelationEntity>()
                .eq(SysRoleMenuRelationEntity::getRoleId, roleId)
                .notIn(SysRoleMenuRelationEntity::getMenuId, menuIds)
        );

        // 深拷贝，取出需要增量更新的部分
        List<Long> needAppendedIds = new ArrayList<>(menuIds);
        Set<Long> existingMenuIds = sysRoleMenuRelationMapper.selectList(menuIdsQuery)
                .stream().map(SysRoleMenuRelationEntity::getMenuId)
                .collect(Collectors.toSet());
        needAppendedIds.removeAll(existingMenuIds);

        if (CollUtil.isNotEmpty(needAppendedIds)) {
            // 批量插入需要增量更新的部分
            List<SysRoleMenuRelationEntity> entityList = needAppendedIds.stream()
                    .map(menuId -> SysRoleMenuRelationEntity.of(roleId, menuId)).toList();
            sysRoleMenuRelationMapper.insert(entityList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByRoleIds(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        sysRoleMenuRelationMapper.delete(new LambdaQueryWrapper<SysRoleMenuRelationEntity>()
                .in(SysRoleMenuRelationEntity::getRoleId, roleIds));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByMenuIds(Collection<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            return;
        }
        sysRoleMenuRelationMapper.delete(new LambdaQueryWrapper<SysRoleMenuRelationEntity>()
                .in(SysRoleMenuRelationEntity::getMenuId, menuIds));
    }
}
