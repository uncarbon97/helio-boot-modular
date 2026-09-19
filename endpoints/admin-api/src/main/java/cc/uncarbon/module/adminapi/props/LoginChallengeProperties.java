package cc.uncarbon.module.adminapi.props;

import cc.uncarbon.module.adminapi.support.loginchallenge.enums.LoginChallengeStrategyTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 后台管理-登录挑战配置
 */
@ConfigurationProperties(prefix = "admin-api.login-challenge")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LoginChallengeProperties {

    /**
     * 挑战策略
     */
    private LoginChallengeStrategyTypeEnum strategy = LoginChallengeStrategyTypeEnum.NONE;

    /**
     * 图形验证码（OCR）配置
     */
    private Ocr ocr = new Ocr();


    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Ocr {

        /**
         * 验证码答案长度
         */
        private Integer answerLength = 4;

        /**
         * 验证码有效秒数
         */
        private Integer validSeconds = 300;
    }
}
