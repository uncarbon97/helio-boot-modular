package cc.uncarbon.module.adminapi.helper;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户-在会话用户登记簿
 * 登记当前处于某租户上下文的会话（登录进入、切换切入），租户被禁用时据此并入既有强制登出链路
 * <p>
 * Redis SET：键 Tenant:current-admins:{tenantId}，成员=登录用户ID；
 * TTL 与 sa-token 会话超时对齐（30 天 + 1 天冗余），残留键自动过期自愈
 */
@Component
@RequiredArgsConstructor
public class TenantSwitchRegistry {

    private static final String CACHE_KEY = "Tenant:current-admins:%s";

    /**
     * 登记有效期（秒）：对齐 token timeout 30 天，外加 1 天冗余
     */
    private static final long EXPIRE_SECONDS = 2678400L;


    private final StringRedisTemplate stringRedisTemplate;


    /**
     * 登记会话当前处于某租户
     */
    public void register(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return;
        }
        String key = String.format(CACHE_KEY, tenantId);
        stringRedisTemplate.opsForSet().add(key, String.valueOf(userId));
        stringRedisTemplate.expire(key, Duration.ofSeconds(EXPIRE_SECONDS));
    }

    /**
     * 注销会话与某租户的关联（切换离开、登出）
     */
    public void unregister(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return;
        }
        stringRedisTemplate.opsForSet().remove(String.format(CACHE_KEY, tenantId), String.valueOf(userId));
    }

    /**
     * 列举当前处于某租户的全部用户ID；无登记时返回空集合
     */
    public Set<Long> listUserIds(Long tenantId) {
        if (tenantId == null) {
            return Set.of();
        }
        var members = stringRedisTemplate.opsForSet().members(String.format(CACHE_KEY, tenantId));
        if (CollUtil.isEmpty(members)) {
            return Set.of();
        }
        return members.stream().map(Long::valueOf).collect(Collectors.toSet());
    }

}
