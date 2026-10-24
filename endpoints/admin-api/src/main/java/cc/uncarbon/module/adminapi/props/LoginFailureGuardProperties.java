package cc.uncarbon.module.adminapi.props;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 后台管理-登录失败限制配置（防撞库）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "admin-api.login-failure-guard")
public class LoginFailureGuardProperties {

    /**
     * 允许的最大连续失败次数，达到后临时锁定
     */
    private Integer maxFailures = 5;

    /**
     * 锁定时长（秒）
     */
    private Integer lockSeconds = 900;

}
