package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.module.sys.dal.entity.SysUserRoleRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserRoleRelationMapper;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;


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
        SysUserRoleRelationEntity entity = new SysUserRoleRelationEntity()
                .setUserId(userId).setRoleId(roleId);
        entity.setTenantId(tenantId);

        sysUserRoleRelationMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
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
        return sysUserRoleRelationMapper.listRoleIdsByUser(userId);
    }
}
