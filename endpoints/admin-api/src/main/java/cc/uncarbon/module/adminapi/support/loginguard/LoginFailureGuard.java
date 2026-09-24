package cc.uncarbon.module.adminapi.support.loginguard;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import cc.uncarbon.module.adminapi.constant.AdminCacheKeyConstant;
import cc.uncarbon.module.adminapi.errorcode.AdminApiErrorCodeEnum;
import cc.uncarbon.module.adminapi.props.LoginFailureGuardProperties;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录失败次数限制（防撞库）
 *
 * <p>锁定 key 维度按登录模式收敛到本类一处（B8）：
 * TENANT_FIRST 按「租户编码+账号」（continew 教训：不含租户则同名互锁）；
 * USER_FIRST 按「账号」（pin 全局唯一，天然按人）</p>
 *
 * @author Uncarbon
 */
@EnableConfigurationProperties(value = LoginFailureGuardProperties.class)
@RequiredArgsConstructor
@Component
public class LoginFailureGuard {

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final LoginFailureGuardProperties props;
    private final TenantFacade tenantFacade;


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
        if (count != null && count <= 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(props.getLockSeconds()));
        }
    }

    /**
     * 登录成功后清除计数
     */
    public void clear(String tenantCode, String pin) {
        stringRedisTemplate.delete(cacheKey(tenantCode, pin));
    }

    /**
     * 构造缓存键
     */
    private String cacheKey(String tenantCode, String pin) {
        String normalizedPin = CharSequenceUtil.nullToEmpty(pin);
        TenantLoginModeEnum loginMode = tenantFacade.getLoginMode();
        if (loginMode == TenantLoginModeEnum.TENANT_FIRST) {
            // 租户优先：按「租户编码+账号」计数
            return String.format(AdminCacheKeyConstant.LOGIN_CHALLENGE_FAIL_COUNT,
                    CharSequenceUtil.nullToEmpty(tenantCode), normalizedPin);
        } else if (loginMode == TenantLoginModeEnum.USER_FIRST) {
            // 用户优先：pin 全局唯一，按账号计数（切换模式时计数重置，可容忍）
            return String.format(AdminCacheKeyConstant.LOGIN_CHALLENGE_FAIL_COUNT, "global", normalizedPin);
        }
        throw new IllegalStateException("不能根据 loginMode=" + loginMode + " 确定缓存键");
    }
}
