package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.db.constant.SQLSegment;
import cc.uncarbon.module.sys.dal.entity.SysUserDeptRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserDeptRelationMapper;
import cc.uncarbon.module.sys.service.SysUserDeptRelationService;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 系统用户-部门关联关系
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysUserDeptRelationServiceImpl implements SysUserDeptRelationService {

    private final SysUserDeptRelationMapper sysUserDeptRelationMapper;


    @Override
    public List<Long> listDeptIdsByUser(Long userId) {
        SysUserDeptRelationEntity entity = sysUserDeptRelationMapper.selectOne(
                new LambdaQueryWrapper<SysUserDeptRelationEntity>()
                        .eq(SysUserDeptRelationEntity::getUserId, userId)
                        .last(SQLSegment.LIMIT_1)
        );

        if (entity == null) {
            return List.of();
        }
        return List.of(entity.getDeptId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cleanAndBind(Long userId, Long deptId) {
        sysUserDeptRelationMapper.delete(
                new LambdaQueryWrapper<SysUserDeptRelationEntity>()
                        .eq(SysUserDeptRelationEntity::getUserId, userId)
        );

        if (Objects.nonNull(deptId)) {
            // 需要绑定部门
            sysUserDeptRelationMapper.insert(SysUserDeptRelationEntity.of(userId, deptId));
        }
    }

    @Override
    public Set<Long> listUserIdsByDepts(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Set.of();
        }

        return sysUserDeptRelationMapper.selectList(
                new LambdaQueryWrapper<SysUserDeptRelationEntity>()
                        // 只要用户ID
                        .select(SysUserDeptRelationEntity::getUserId)
                        .in(SysUserDeptRelationEntity::getDeptId, deptIds)
        ).stream().map(SysUserDeptRelationEntity::getUserId).collect(Collectors.toSet());
    }

}
