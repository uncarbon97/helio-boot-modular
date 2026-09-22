package cc.uncarbon.module.adminapi.support.loginchallenge.strategy;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.adminapi.errorcode.AdminApiErrorCodeEnum;
import cc.uncarbon.module.adminapi.props.LoginChallengeProperties;
import cc.uncarbon.module.adminapi.support.loginchallenge.enums.LoginChallengeStrategyTypeEnum;
import cc.uncarbon.module.adminapi.support.loginchallenge.valueobj.AdminAuthChallengeVO;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 图形验证码（OCR）登录挑战策略
 * 验证码答案缓存至 Redis
 *
 * @author Uncarbon
 */
@EnableConfigurationProperties(value = LoginChallengeProperties.class)
@RequiredArgsConstructor
@Component
public class OcrCaptchaChallengeStrategy implements LoginChallengeStrategy {

    private static final String CACHE_KEY_CAPTCHA_ANSWER = "login-challenge:ocr:%s";

    /**
     * GETDEL 脚本
     */
    private static final DefaultRedisScript<String> GET_AND_DELETE_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]) redis.call('DEL', KEYS[1]) return v", String.class);

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final LoginChallengeProperties props;


    @Override
    public LoginChallengeStrategyTypeEnum type() {
        return LoginChallengeStrategyTypeEnum.OCR;
    }

    @Override
    public AdminAuthChallengeVO generate() {
        int answerLength = props.getOcr().getAnswerLength();
        int validSeconds = props.getOcr().getValidSeconds();

        // redis预占位；随机10个UUID，应该有个能成的吧……
        UUID uuid = null;
        String captchaCacheKey = null;
        Boolean successFlag = Boolean.FALSE;
        for (int count = 0; count < 10; count++) {
            uuid = UUID.randomUUID();
            captchaCacheKey = String.format(CACHE_KEY_CAPTCHA_ANSWER, uuid.toString(true));
            successFlag = stringRedisTemplate.opsForValue()
                    .setIfAbsent(captchaCacheKey, CharSequenceUtil.EMPTY, Duration.of(validSeconds, ChronoUnit.SECONDS));
            if (Boolean.TRUE.equals(successFlag)) {
                break;
            }
        }
        if (!Boolean.TRUE.equals(successFlag)) {
            throw new BusinessException(AdminApiErrorCodeEnum.B04001);
        }

        // 定义图形验证码的长、宽、验证码字符数、干扰线宽度
        ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(200, 50, answerLength, 4);

        // 将验证码答案保存至 redis
        Duration duration = Duration.of(validSeconds, ChronoUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(captchaCacheKey, captcha.getCode(), duration);

        return new AdminAuthChallengeVO(type())
                .setCaptchaImageEncoded(captcha.getImageBase64Data())
                .setCaptchaId(uuid.toString(true))
                .setValidSeconds(validSeconds);
    }

    @Override
    public boolean validate(String uuid, String captchaAnswer) {
        if (CharSequenceUtil.hasBlank(uuid, captchaAnswer)) {
            return false;
        }

        String cacheKey = String.format(CACHE_KEY_CAPTCHA_ANSWER, uuid);
        if (CharSequenceUtil.length(captchaAnswer) != props.getOcr().getAnswerLength()) {
            // 长度不同
            return false;
        }

        // 原子操作，防止并发请求重放同一验证码
        String answerInRedis = stringRedisTemplate.execute(GET_AND_DELETE_SCRIPT, List.of(cacheKey));
        return CharSequenceUtil.equalsIgnoreCase(answerInRedis, captchaAnswer);
    }
}
