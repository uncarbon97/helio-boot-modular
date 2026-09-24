package cc.uncarbon.module.adminapi.helper;

import cc.uncarbon.module.adminapi.constant.AdminCacheKeyConstant;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户-在会话用户登记簿
 * 登记当前处于某租户上下文的会话（切换切入），租户被禁用时，据此并入既有强制登出链路
 */
@RequiredArgsConstructor
@Component
public class TenantSwitchHelper {

    /**
     * 登记有效期（秒）：对齐 SA-Token timeout，外加 1 天冗余
     */
    private static long expireSeconds;


    private final SaTokenConfig saTokenConfig;
    private final StringRedisTemplate stringRedisTemplate;

    @PostConstruct
    public void init() {
        expireSeconds = saTokenConfig.getTimeout() + 24 * 60 * 60;
    }

    /**
     * 登记会话当前处于某租户
     */
    public void register(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return;
        }
        String key = String.format(AdminCacheKeyConstant.TENANT_LOGGED_IN, tenantId);
        stringRedisTemplate.opsForSet().add(key, String.valueOf(userId));
        stringRedisTemplate.expire(key, Duration.ofSeconds(expireSeconds));
    }

    /**
     * 注销会话与某租户的关联（切换离开、登出）
     */
    public void unregister(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return;
        }
        stringRedisTemplate.opsForSet().remove(String.format(AdminCacheKeyConstant.TENANT_LOGGED_IN, tenantId), String.valueOf(userId));
    }

    /**
     * 列举当前处于某租户的全部用户ID；无登记时返回空集合
     */
    public Set<Long> listUserIds(Long tenantId) {
        if (tenantId == null) {
            return Set.of();
        }
        var members = stringRedisTemplate.opsForSet().members(String.format(AdminCacheKeyConstant.TENANT_LOGGED_IN, tenantId));
        if (CollUtil.isEmpty(members)) {
            return Set.of();
        }
        return members.stream().map(Long::valueOf).collect(Collectors.toSet());
    }

}
