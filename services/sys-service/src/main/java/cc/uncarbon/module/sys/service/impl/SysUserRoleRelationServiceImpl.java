package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.module.sys.dal.entity.SysUserRoleRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserRoleRelationMapper;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 系统用户-角色关联关系
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysUserRoleRelationServiceImpl implements SysUserRoleRelationService {

    private final SysUserRoleRelationMapper sysUserRoleRelationMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long adminCreate(Long tenantId, Long userId, Long roleId) {
        SysUserRoleRelationEntity entity = new SysUserRoleRelationEntity()
                .setUserId(userId).setRoleId(roleId);
        entity.setTenantId(tenantId);

        sysUserRoleRelationMapper.insert(entity);

        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanAndBind(Long userId, Collection<Long> roleIds) {
        sysUserRoleRelationMapper.delete(
                new QueryWrapper<SysUserRoleRelationEntity>()
                        .lambda()
                        .eq(SysUserRoleRelationEntity::getUserId, userId)
        );

        if (CollUtil.isNotEmpty(roleIds)) {
            // 需要绑定角色
            List<SysUserRoleRelationEntity> entityList = roleIds.stream()
                    .map(roleId -> SysUserRoleRelationEntity.of(userId, roleId)).toList();
            sysUserRoleRelationMapper.insert(entityList);
        }
    }

    @Override
    public List<Long> listRoleIdsByUser(Long userId) {
        if (Objects.isNull(userId)) {
            return List.of();
        }

        return sysUserRoleRelationMapper.selectList(
                new LambdaQueryWrapper<SysUserRoleRelationEntity>()
                        .select(SysUserRoleRelationEntity::getRoleId)
                        .eq(SysUserRoleRelationEntity::getUserId, userId)
        ).stream().map(SysUserRoleRelationEntity::getRoleId).toList();
    }

    @Override
    public Set<Long> listUserIdsByRoles(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Set.of();
        }

        return sysUserRoleRelationMapper.selectList(
                new LambdaQueryWrapper<SysUserRoleRelationEntity>()
                        .select(SysUserRoleRelationEntity::getUserId)
                        .in(SysUserRoleRelationEntity::getRoleId, roleIds)
        ).stream().map(SysUserRoleRelationEntity::getUserId).collect(Collectors.toSet());
    }
}
