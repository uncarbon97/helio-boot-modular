package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.db.constant.SQLSegment;
import cc.uncarbon.module.sys.dal.entity.SysUserDeptRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserDeptRelationMapper;
import cc.uncarbon.module.sys.service.SysUserDeptRelationService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 后台用户-部门关联
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysUserDeptRelationServiceImpl implements SysUserDeptRelationService {

    private final SysUserDeptRelationMapper sysUserDeptRelationMapper;


    /**
     * 列举用户ID关联的部门IDs
     *
     * @return 关联的部门IDs；目前最多只有1个元素
     */
    @Override
    public List<Long> getUserDeptIds(Long userId) {
        SysUserDeptRelationEntity entity = sysUserDeptRelationMapper.selectOne(
                new QueryWrapper<SysUserDeptRelationEntity>()
                        .lambda()
                        .eq(SysUserDeptRelationEntity::getUserId, userId)
                        .last(SQLSegment.LIMIT_1)
        );

        if (entity == null) {
            return List.of();
        }
        return Collections.singletonList(entity.getDeptId());
    }

    /**
     * 先清理用户ID所有关联关系, 再绑定用户ID与部门ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cleanAndBind(Long userId, Long deptId) {
        sysUserDeptRelationMapper.delete(
                new QueryWrapper<SysUserDeptRelationEntity>()
                        .lambda()
                        .eq(SysUserDeptRelationEntity::getUserId, userId)
        );

        if (ObjectUtil.isNotNull(deptId)) {
            // 需要绑定部门
            sysUserDeptRelationMapper.insert(SysUserDeptRelationEntity.of(userId, deptId));
        }

    }

    /**
     * 列举部门IDs关联的用户IDs
     */
    @Override
    public Set<Long> listUserIdsByDeptIds(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptySet();
        }

        return sysUserDeptRelationMapper.selectList(
                new QueryWrapper<SysUserDeptRelationEntity>()
                        .lambda()
                        // 只要用户ID
                        .select(SysUserDeptRelationEntity::getUserId)
                        .in(SysUserDeptRelationEntity::getDeptId, deptIds)
        ).stream().map(SysUserDeptRelationEntity::getUserId).collect(Collectors.toSet());
    }

}
