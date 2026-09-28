package cc.uncarbon.module.adminapi.support.loginchallenge.strategy;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.adminapi.errorcode.AdminApiErrorCodeEnum;
import cc.uncarbon.module.adminapi.props.LoginChallengeProperties;
import cc.uncarbon.module.adminapi.support.loginchallenge.enums.LoginChallengeStrategyTypeEnum;
import cc.uncarbon.module.adminapi.support.loginchallenge.valueobj.AdminAuthChallengeVO;
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
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.time.Duration;

/**
 * {@link OcrCaptchaChallengeStrategy} 图形验证码分支测试（GETDEL 防重放由 Redis 脚本保证）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OcrCaptchaChallengeStrategyTest {

    @Mock
    private RedisTemplate<String, String> stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginChallengeProperties props;
    private OcrCaptchaChallengeStrategy strategy;


    @BeforeEach
    void setUp() {
        Mockito.doReturn(valueOperations).when(stringRedisTemplate).opsForValue();
        props = new LoginChallengeProperties();
        strategy = new OcrCaptchaChallengeStrategy(stringRedisTemplate, props);
    }

    @Test
    void typeIsOcr() {
        Assertions.assertEquals(LoginChallengeStrategyTypeEnum.OCR, strategy.type());
    }

    @Test
    void validateRejectsBlankInputs() {
        Assertions.assertFalse(strategy.validate(null, "abcd"));
        Assertions.assertFalse(strategy.validate("uuid", null));
        Assertions.assertFalse(strategy.validate(" ", " "));
    }

    @Test
    void validateRejectsWrongLengthWithoutTouchingRedis() {
        props.getOcr().setAnswerLength(4);

        Assertions.assertFalse(strategy.validate("uuid", "abc"));

        Mockito.verify(stringRedisTemplate, Mockito.never()).execute(
                Mockito.any(DefaultRedisScript.class), Mockito.anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void validateComparesCaseInsensitivelyAfterGetdel() {
        props.getOcr().setAnswerLength(4);
        Mockito.when(stringRedisTemplate.execute(
                Mockito.any(DefaultRedisScript.class), Mockito.anyList())).thenReturn("ABCD");

        Assertions.assertTrue(strategy.validate("uuid", "abcd"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void validateFailsWhenAnswerMissing() {
        props.getOcr().setAnswerLength(4);
        Mockito.when(stringRedisTemplate.execute(
                Mockito.any(DefaultRedisScript.class), Mockito.anyList())).thenReturn(null);

        Assertions.assertFalse(strategy.validate("uuid", "abcd"));
    }

    @Test
    void generateHappyPath() {
        Mockito.when(valueOperations.setIfAbsent(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(Duration.class))).thenReturn(Boolean.TRUE);

        AdminAuthChallengeVO vo = strategy.generate();

        Assertions.assertEquals(LoginChallengeStrategyTypeEnum.OCR, vo.getType());
        Assertions.assertNotNull(vo.getCaptchaId());
        Assertions.assertEquals(300, vo.getValidSeconds());
        Assertions.assertNotNull(vo.getCaptchaImageEncoded());
        Mockito.verify(valueOperations).set(Mockito.contains(vo.getCaptchaId()),
                Mockito.anyString(), Mockito.any(Duration.class));
    }

    @Test
    void generateThrowsWhenAllSlotsOccupied() {
        Mockito.when(valueOperations.setIfAbsent(Mockito.anyString(), Mockito.anyString(),
                Mockito.any(Duration.class))).thenReturn(Boolean.FALSE);

        var ex = Assertions.assertThrows(BusinessException.class, () -> strategy.generate());
        Assertions.assertEquals(AdminApiErrorCodeEnum.B04001, ex.getErrorCode());
    }
}
