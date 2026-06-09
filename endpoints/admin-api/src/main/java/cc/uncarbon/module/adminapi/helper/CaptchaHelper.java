package cc.uncarbon.module.adminapi.helper;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.adminapi.enums.AdminApiErrorEnum;
import cc.uncarbon.module.adminapi.model.internal.AdminCaptchaScope;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 验证码助手类；可将验证码答案缓存至 Redis
 *
 * @author Uncarbon
 */
@Component
@RequiredArgsConstructor
public class CaptchaHelper {

    private static final String CACHE_KEY_CAPTCHA_ANSWER = "Authorization:captcha:%s";

    /**
     * 验证码答案长度
     */
    private static final int CAPTCHA_ANSWER_LENGTH = 4;

    /**
     * 验证码有效秒数
     */
    private static final int CAPTCHA_VALID_SECONDS = 300;

    private final RedisTemplate<String, String> stringRedisTemplate;


    /**
     * 生成一个验证码
     */
    public AdminCaptchaScope generate() {
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
            throw new BusinessException(AdminApiErrorEnum.CAPTCHA_GENERATE_FAILED);
        }

        // 定义图形验证码的长、宽、验证码字符数、干扰线宽度
        ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(196, 50, CAPTCHA_ANSWER_LENGTH, 4);

        // 将验证码答案保存至 redis, 有效期5分钟
        Duration duration = Duration.of(CAPTCHA_VALID_SECONDS, ChronoUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(captchaCacheKey, captcha.getCode(), duration);
        LocalDateTime expiredAt = LocalDateTimeUtil.offset(LocalDateTimeUtil.now(), CAPTCHA_VALID_SECONDS, ChronoUnit.SECONDS);

        return new AdminCaptchaScope(captcha, uuid.toString(true), expiredAt);
    }

    /**
     * 核验验证码是否输入正确
     *
     * @param uuid          验证码唯一标识（UUID）
     * @param captchaAnswer 验证码答案
     * @return 是否正确
     */
    public boolean validate(String uuid, String captchaAnswer) {
        if (CharSequenceUtil.hasBlank(uuid, captchaAnswer)) {
            return false;
        }

        String cacheKey = String.format(CACHE_KEY_CAPTCHA_ANSWER, uuid);
        boolean equals;
        try {
            if (CharSequenceUtil.length(captchaAnswer) != CAPTCHA_ANSWER_LENGTH) {
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
