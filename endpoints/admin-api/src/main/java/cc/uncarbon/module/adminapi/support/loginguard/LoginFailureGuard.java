package cc.uncarbon.module.adminapi.support.loginguard;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.adminapi.errorcode.AdminApiErrorCodeEnum;
import cc.uncarbon.module.adminapi.props.LoginFailureGuardProperties;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录失败次数限制（防撞库）
 * 按 租户编码+账号 维度在 Redis 计数，达到阈值后临时锁定
 *
 * @author Uncarbon
 */
@EnableConfigurationProperties(value = LoginFailureGuardProperties.class)
@RequiredArgsConstructor
@Component
public class LoginFailureGuard {

    private static final String CACHE_KEY_LOGIN_FAIL_COUNT = "login-challenge:fail-count:%s:%s";

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final LoginFailureGuardProperties props;


    /**
     * 登录前检查是否已锁定
     */
    public void assertNotLocked(String tenantCode, String pin) {
        String count = stringRedisTemplate.opsForValue().get(cacheKey(tenantCode, pin));
        if (count != null && count.chars().allMatch(Character::isDigit)
                && Integer.parseInt(count) >= props.getMaxFailures()) {
            throw new BusinessException(AdminApiErrorCodeEnum.A04004);
        }
    }

    /**
     * 记录一次登录失败
     */
    public void recordFailure(String tenantCode, String pin) {
        String key = cacheKey(tenantCode, pin);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(props.getLockSeconds()));
        }
    }

    /**
     * 登录成功后清除计数
     */
    public void clear(String tenantCode, String pin) {
        stringRedisTemplate.delete(cacheKey(tenantCode, pin));
    }

    private String cacheKey(String tenantCode, String pin) {
        return String.format(CACHE_KEY_LOGIN_FAIL_COUNT,
                CharSequenceUtil.nullToEmpty(tenantCode), CharSequenceUtil.nullToEmpty(pin));
    }
}
