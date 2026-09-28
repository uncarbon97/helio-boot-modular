package cc.uncarbon.module.sys.biz;

import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserTenantRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserTenantRelationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户租户归属一致性对账任务（B1 / M4）
 *
 * <p>relation 表（sys_user_tenant_relation）为唯一真源，sys_user.tenant_id 为投影列；
 * 服务层同事务双写，本任务每日兜底对账，发现漂移即告警（只读，不自动修复）。</p>
 *
 * @author Uncarbon
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SysUserTenantConsistencyChecker {

    private static final String LOG_PREFIX = "[系统管理][用户租户对账]";

    private final SysUserMapper sysUserMapper;
    private final SysUserTenantRelationMapper sysUserTenantRelationMapper;


    /**
     * 每日一次；校验 tenant_id ∈ relation 表（启用状态）
     */
    @Scheduled(initialDelay = 10, fixedDelay = 24, timeUnit = TimeUnit.HOURS)
    public void reconcile() {
        try {
            List<SysUserEntity> users = TenantContextHolder.callIgnored(() ->
                    sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                            .select(SysUserEntity::getId, SysUserEntity::getTenantId)));
            List<SysUserTenantRelationEntity> relations = TenantContextHolder.callIgnored(() ->
                    sysUserTenantRelationMapper.selectList(new LambdaQueryWrapper<SysUserTenantRelationEntity>()
                            .select(SysUserTenantRelationEntity::getTenantId,
                                    SysUserTenantRelationEntity::getUserId)));

            // user -> 其启用关系所在租户集合
            Map<Long, Set<Long>> tenantIdsByUser = relations.stream().collect(Collectors.groupingBy(
                    SysUserTenantRelationEntity::getUserId,
                    Collectors.mapping(SysUserTenantRelationEntity::getTenantId, Collectors.toSet())));

            long driftCount = users.stream()
                    .filter(user -> {
                        Set<Long> related = tenantIdsByUser.get(user.getId());
                        return related == null || !related.contains(user.getTenantId());
                    })
                    .count();

            if (driftCount > 0) {
                log.warn(LOG_PREFIX + " 检测到投影列与真源表漂移 >> 漂移用户数={}，请核对 sys_user.tenant_id 与 sys_user_tenant_relation",
                        driftCount);
            } else {
                log.info(LOG_PREFIX + " 对账通过 >> 用户数={}", users.size());
            }
        } catch (Exception e) {
            log.warn(LOG_PREFIX + " 对账执行失败，下个周期重试 >> ", e);
        }
    }

}

