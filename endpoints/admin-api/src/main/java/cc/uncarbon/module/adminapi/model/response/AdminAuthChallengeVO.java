package cc.uncarbon.module.adminapi.model.response;

import cc.uncarbon.module.adminapi.model.internal.AdminCaptchaScope;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 后台管理-鉴权挑战
 */
@Getter
public class AdminAuthChallengeVO {

    @Schema(description = "挑战类型")
    private final String type = "ocr";

    @Schema(description = "验证码图片Base64")
    private final String captchaImageEncoded;

    @Schema(description = "验证码唯一标识")
    private final String captchaId;

    @Schema(description = "验证码失效时刻")
    private final LocalDateTime expiredAt;


    public AdminAuthChallengeVO(AdminCaptchaScope source) {
        this.captchaImageEncoded = source.image().getImageBase64Data();
        this.captchaId = source.uuid();
        this.expiredAt = source.expiredAt();
    }
}
