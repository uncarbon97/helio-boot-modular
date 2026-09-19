package cc.uncarbon.module.adminapi.helper;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.adminapi.errorcode.AdminApiErrorCodeEnum;
import cc.uncarbon.module.adminapi.model.response.AdminAuthChallengeVO;
import cc.uncarbon.module.adminapi.props.LoginChallengeProperties;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 图形验证码（OCR）登录挑战处理器；可将验证码答案缓存至 Redis
 *
 * @author Uncarbon
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(LoginChallengeProperties.class)
public class OcrCaptchaHelper implements LoginChallengeHandler {

    private static final String CACHE_KEY_CAPTCHA_ANSWER = "Authorization:captcha:ocr:%s";

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final LoginChallengeProperties loginChallengeProperties;


    @Override
    public String type() {
        return "ocr";
    }

    @Override
    public AdminAuthChallengeVO generate() {
        int answerLength = loginChallengeProperties.getOcr().getAnswerLength();
        int validSeconds = loginChallengeProperties.getOcr().getValidSeconds();

        // redis预占位；随机10个UUID，应该有个能成的吧……
        UUID uuid = null;
        String captchaCacheKey = null;
        Boolean successFlag = Boolean.FALSE;
        for (int count = 0; count < 10; count++) {
            uuid = UUID.randomUUID();
            captchaCacheKey = String.format(CACHE_KEY_CAPTCHA_ANSWER, uuid.toString(true));
            successFlag = stringRedisTemplate.opsForValue().setIfAbsent(captchaCacheKey, CharSequenceUtil.EMPTY);
            if (Boolean.TRUE.equals(successFlag)) {
                break;
            }
        }
        if (!Boolean.TRUE.equals(successFlag)) {
            throw new BusinessException(AdminApiErrorCodeEnum.B04001);
        }

        // 定义图形验证码的长、宽、验证码字符数、干扰线宽度
        ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(196, 50, answerLength, 4);

        // 将验证码答案保存至 redis
        Duration duration = Duration.of(validSeconds, ChronoUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(captchaCacheKey, captcha.getCode(), duration);
        LocalDateTime expiredAt = LocalDateTimeUtil.offset(LocalDateTimeUtil.now(), validSeconds, ChronoUnit.SECONDS);

        return new AdminAuthChallengeVO()
                .setType(type())
                .setCaptchaImageEncoded(captcha.getImageBase64Data())
                .setCaptchaId(uuid.toString(true))
                .setExpiredAt(expiredAt);
    }

    @Override
    public boolean validate(String uuid, String captchaAnswer) {
        if (CharSequenceUtil.hasBlank(uuid, captchaAnswer)) {
            return false;
        }

        String cacheKey = String.format(CACHE_KEY_CAPTCHA_ANSWER, uuid);
        boolean equals;
        try {
            if (CharSequenceUtil.length(captchaAnswer) != loginChallengeProperties.getOcr().getAnswerLength()) {
                // 长度不同
                return false;
            }

            String answerInRedis = stringRedisTemplate.opsForValue().get(cacheKey);
            equals = CharSequenceUtil.equalsIgnoreCase(answerInRedis, captchaAnswer);
        } finally {
            stringRedisTemplate.delete(cacheKey);
        }
        return equals;
    }
}
