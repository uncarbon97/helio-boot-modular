package cc.uncarbon.module.adminapi.support.loginguard;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import cc.uncarbon.module.adminapi.errorcode.AdminApiErrorCodeEnum;
import cc.uncarbon.module.adminapi.props.LoginFailureGuardProperties;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import org.mockito.ArgumentMatchers;

/**
 * {@link LoginFailureGuard} 防撞库计数分支测试（锁定 key 维度按登录模式收敛）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginFailureGuardTest {

    @Mock
    private RedisTemplate<String, String> stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private TenantFacade tenantFacade;

    private LoginFailureGuard guard;


    @BeforeEach
    void setUp() {
        Mockito.doReturn(valueOperations).when(stringRedisTemplate).opsForValue();
        guard = new LoginFailureGuard(stringRedisTemplate, new LoginFailureGuardProperties(), tenantFacade);
    }

    @Test
    void assertNotLockedPassesWhenUnderThreshold() {
        stubMode(TenantLoginModeEnum.TENANT_FIRST);
        Mockito.when(valueOperations.get(Mockito.anyString())).thenReturn("4");

        Assertions.assertDoesNotThrow(() -> guard.assertNotLocked("t5", "u2"));
    }

    @Test
    void assertNotLockedThrowsAtThreshold() {
        stubMode(TenantLoginModeEnum.TENANT_FIRST);
        Mockito.when(valueOperations.get(Mockito.anyString())).thenReturn("5");

        var ex = Assertions.assertThrows(BusinessException.class, () -> guard.assertNotLocked("t5", "u2"));
        Assertions.assertEquals(AdminApiErrorCodeEnum.A04004, ex.getErrorCode());
    }

    @Test
    void assertNotLockedIgnoresNonDigitCount() {
        stubMode(TenantLoginModeEnum.TENANT_FIRST);
        Mockito.when(valueOperations.get(Mockito.anyString())).thenReturn("abc");

        Assertions.assertDoesNotThrow(() -> guard.assertNotLocked("t5", "u2"));
    }

    @Test
    void assertNotLockedPassesWhenNoCount() {
        stubMode(TenantLoginModeEnum.USER_FIRST);
        Mockito.when(valueOperations.get(Mockito.anyString())).thenReturn(null);

        Assertions.assertDoesNotThrow(() -> guard.assertNotLocked(null, "u2"));
    }

    @Test
    void recordFailureSetsExpiryOnFirstFailureOnly() {
        stubMode(TenantLoginModeEnum.TENANT_FIRST);
        Mockito.when(valueOperations.increment(Mockito.anyString())).thenReturn(1L);

        guard.recordFailure("t5", "u2");

        Mockito.verify(stringRedisTemplate).expire(Mockito.anyString(), Mockito.eq(Duration.ofSeconds(900)));
    }

    @Test
    void recordFailureSkipsExpiryOnSubsequentFailures() {
        stubMode(TenantLoginModeEnum.TENANT_FIRST);
        Mockito.when(valueOperations.increment(Mockito.anyString())).thenReturn(2L);

        guard.recordFailure("t5", "u2");

        Mockito.verify(stringRedisTemplate, Mockito.never()).expire(Mockito.anyString(), Mockito.any(Duration.class));
    }

    @Test
    void clearDeletesCounter() {
        stubMode(TenantLoginModeEnum.TENANT_FIRST);

        guard.clear("t5", "u2");

        Mockito.verify(stringRedisTemplate).delete(Mockito.contains("u2"));
    }

    @Test
    void cacheKeyDimensionDiffersByLoginMode() {
        // TENANT_FIRST：按「租户编码+账号」计数
        stubMode(TenantLoginModeEnum.TENANT_FIRST);
        guard.recordFailure("t5", "u2");
        Mockito.verify(valueOperations).increment(
                ArgumentMatchers.argThat(key -> key != null && key.contains("t5") && key.contains("u2")));

        // USER_FIRST：pin 全局唯一，按账号计数（租户维度固定为 global）
        stubMode(TenantLoginModeEnum.USER_FIRST);
        guard.recordFailure("t5", "u2");
        Mockito.verify(valueOperations).increment(
                ArgumentMatchers.argThat(key -> key != null && key.contains("global") && key.contains("u2")));
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private void stubMode(TenantLoginModeEnum mode) {
        Mockito.when(tenantFacade.getLoginMode()).thenReturn(mode);
    }
}
