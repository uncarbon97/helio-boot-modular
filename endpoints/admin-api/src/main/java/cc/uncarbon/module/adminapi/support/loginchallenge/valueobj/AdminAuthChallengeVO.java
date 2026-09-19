package cc.uncarbon.module.adminapi.support.loginchallenge.valueobj;

import cc.uncarbon.module.adminapi.support.loginchallenge.enums.LoginChallengeStrategyTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Instant;

/**
 * 后台管理-鉴权挑战
 * 不同挑战类型的字段按需填充，如滑块挑战可另加滑块图字段
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
@Getter
@Setter
@RequiredArgsConstructor
public class AdminAuthChallengeVO {

    @Schema(description = "挑战类型（NONE=无挑战, OCR=图形验证码）")
    private final LoginChallengeStrategyTypeEnum type;

    @Schema(description = "验证码图片Base64")
    private String captchaImageEncoded;

    @Schema(description = "验证码唯一标识")
    private String captchaId;

    @Schema(description = "验证码失效时刻")
    private Instant expiredAt;

    public static AdminAuthChallengeVO noChallenge() {
        return new AdminAuthChallengeVO(LoginChallengeStrategyTypeEnum.NONE);
    }
}
