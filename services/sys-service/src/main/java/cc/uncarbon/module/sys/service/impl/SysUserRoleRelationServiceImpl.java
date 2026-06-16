package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.sys.dal.entity.SysUserRoleRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserRoleRelationMapper;
import cc.uncarbon.module.sys.model.request.TenantUserBindRoleRequest;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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


    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreate(Long tenantId, Long userId, Long roleId) {
        var entity = new SysUserRoleRelationEntity()
                .setUserId(userId).setRoleId(roleId);
        entity.setTenantId(tenantId);

        sysUserRoleRelationMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cleanAndBind(Long userId, Collection<Long> roleIds) {
        var roleIdsQuery = new LambdaQueryWrapper<SysUserRoleRelationEntity>()
                .select(SysUserRoleRelationEntity::getRoleId)
                .eq(SysUserRoleRelationEntity::getUserId, userId);

        if (CollUtil.isEmpty(roleIds)) {
            // 清除绑定，直接删除所有关联关系就行
            sysUserRoleRelationMapper.delete(roleIdsQuery);
            return;
        }

        // 先删除不再需要的关联关系
        sysUserRoleRelationMapper.delete(new LambdaQueryWrapper<SysUserRoleRelationEntity>()
                .eq(SysUserRoleRelationEntity::getUserId, userId)
                .notIn(SysUserRoleRelationEntity::getRoleId, roleIds)
        );

        // 深拷贝，取出需要增量更新的部分
        List<Long> needAppendedIds = new ArrayList<>(roleIds);
        Set<Long> existingRoleIds = sysUserRoleRelationMapper.selectList(roleIdsQuery)
                .stream().map(SysUserRoleRelationEntity::getRoleId)
                .collect(Collectors.toSet());
        needAppendedIds.removeAll(existingRoleIds);

        if (CollUtil.isNotEmpty(needAppendedIds)) {
            // 批量插入需要增量更新的部分
            List<SysUserRoleRelationEntity> entityList = needAppendedIds.stream()
                    .map(roleId -> SysUserRoleRelationEntity.of(userId, roleId)).toList();
            sysUserRoleRelationMapper.insert(entityList);
        }
    }

    @Override
    public List<Long> listRoleIdsByUser(Long userId) {
        return sysUserRoleRelationMapper.listRoleIdsByUser(userId);
    }

    @Override
    public void tenantUserBindRole(TenantUserBindRoleRequest request) {
        TenantContextHolder.runWithContext(
                new SimpleTenantContext(request.getTenantId(), request.getTenantCode(), null),
                () -> {
                    var self = SpringUtil.getBean(getClass());
                    self.cleanAndBind(request.getUserId(), request.getRoleIds());
                });
    }
}
