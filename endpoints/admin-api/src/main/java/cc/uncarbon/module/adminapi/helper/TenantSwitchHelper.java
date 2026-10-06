package cc.uncarbon.module.adminapi.helper;

import cc.uncarbon.module.adminapi.constant.AdminCacheKeyConstant;
import cc.uncarbon.module.adminapi.model.internal.TenantSwitchInfo;
import cc.uncarbon.module.commons.satoken.StpKit;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.session.SaSession;
import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户-在会话用户登记簿
 * 登记当前处于某租户上下文的会话（切换切入），租户被禁用时，据此触发强制退回原视角（见 ExitTenantSwitchEvent）
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

    private static @NonNull String toCacheKey(Long tenantId) {
        return String.format(AdminCacheKeyConstant.TENANT_LOGGED_IN, tenantId);
    }

    /**
     * 登记会话当前处于某租户
     */
    public void register(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return;
        }
        String key = toCacheKey(tenantId);
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
        stringRedisTemplate.opsForSet().remove(toCacheKey(tenantId), String.valueOf(userId));
    }

    /**
     * 判断当前已登录用户，是否处于租户切换中
     */
    public static boolean isSwitching() {
        SaSession session = StpKit.ADMIN.getSession();
        return session.get(TenantSwitchInfo.CAMEL_NAME) instanceof TenantSwitchInfo;
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 列举当前处于某租户的全部用户ID；无登记时返回空集合
     */
    public Set<Long> listUserIds(Long tenantId) {
        if (tenantId == null) {
            return Set.of();
        }
        var members = stringRedisTemplate.opsForSet().members(toCacheKey(tenantId));
        if (CollUtil.isEmpty(members)) {
            return Set.of();
        }
        return members.stream().map(Long::valueOf).collect(Collectors.toSet());
    }
}
