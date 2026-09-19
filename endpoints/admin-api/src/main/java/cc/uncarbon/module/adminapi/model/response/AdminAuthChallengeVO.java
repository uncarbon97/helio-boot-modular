package cc.uncarbon.module.adminapi.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 后台管理-鉴权挑战
 * 不同挑战类型的字段按需填充，如滑块挑战可另加滑块图字段
 */
@Accessors(chain = true)
@Getter
@Setter
public class AdminAuthChallengeVO {

    @Schema(description = "挑战类型（none=无挑战, ocr=图形验证码）")
    private String type = "none";

    @Schema(description = "验证码图片Base64")
    private String captchaImageEncoded;

    @Schema(description = "验证码唯一标识")
    private String captchaId;

    @Schema(description = "验证码失效时刻")
    private LocalDateTime expiredAt;
}
