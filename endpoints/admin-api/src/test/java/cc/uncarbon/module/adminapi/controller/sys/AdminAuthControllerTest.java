package cc.uncarbon.module.adminapi.controller.sys;

import cc.uncarbon.module.adminapi.props.LoginChallengeProperties;
import cc.uncarbon.module.adminapi.support.loginchallenge.enums.LoginChallengeStrategyTypeEnum;
import cc.uncarbon.module.adminapi.support.loginchallenge.strategy.LoginChallengeStrategy;
import cc.uncarbon.module.adminapi.support.loginchallenge.valueobj.AdminAuthChallengeVO;
import cc.uncarbon.module.adminapi.support.loginguard.LoginFailureGuard;
import cc.uncarbon.module.sys.service.AdminLoginService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

/**
 * {@link AdminAuthController} 挑战解析分支测试
 * （登录方法涉及 sa-token 全局状态，见集成测试）
 */
@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTest {

    @Test
    void challengeReturnsNoChallengeWhenStrategyNone() {
        AdminAuthController controller = new AdminAuthController(
                Mockito.mock(AdminLoginService.class),
                new LoginChallengeProperties(),
                List.of(),
                Mockito.mock(LoginFailureGuard.class));

        var ret = controller.challenge();

        Assertions.assertEquals(LoginChallengeStrategyTypeEnum.NONE, ret.getData().getType());
    }

    @Test
    void challengeDelegatesToConfiguredStrategy() {
        LoginChallengeStrategy strategy = Mockito.mock(LoginChallengeStrategy.class);
        Mockito.when(strategy.type()).thenReturn(LoginChallengeStrategyTypeEnum.OCR);
        Mockito.when(strategy.generate()).thenReturn(
                new AdminAuthChallengeVO(LoginChallengeStrategyTypeEnum.OCR).setCaptchaId("x"));
        LoginChallengeProperties props = new LoginChallengeProperties();
        props.setStrategy(LoginChallengeStrategyTypeEnum.OCR);

        AdminAuthController controller = new AdminAuthController(
                Mockito.mock(AdminLoginService.class), props, List.of(strategy),
                Mockito.mock(LoginFailureGuard.class));

        var ret = controller.challenge();

        Assertions.assertEquals("x", ret.getData().getCaptchaId());
    }

    @Test
    void failFastWhenConfiguredStrategyMissing() {
        LoginChallengeProperties props = new LoginChallengeProperties();
        props.setStrategy(LoginChallengeStrategyTypeEnum.OCR);

        AdminAuthController controller = new AdminAuthController(
                Mockito.mock(AdminLoginService.class), props, List.of(),
                Mockito.mock(LoginFailureGuard.class));

        Assertions.assertThrows(IllegalStateException.class, controller::validateChallengeStrategyConfig);
    }
}
