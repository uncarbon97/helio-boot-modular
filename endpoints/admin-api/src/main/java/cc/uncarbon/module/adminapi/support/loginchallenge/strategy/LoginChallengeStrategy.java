package cc.uncarbon.module.adminapi.support.loginchallenge.strategy;

import cc.uncarbon.module.adminapi.support.loginchallenge.enums.LoginChallengeStrategyTypeEnum;
import cc.uncarbon.module.adminapi.support.loginchallenge.valueobj.AdminAuthChallengeVO;

/**
 * 登录挑战策略
 * 新增验证码形式（如滑块）时，实现此接口并注册为 Spring 组件即可
 *
 * @author Uncarbon
 */
public interface LoginChallengeStrategy {

    /**
     * 挑战策略类型
     */
    LoginChallengeStrategyTypeEnum type();

    /**
     * 生成登录挑战
     */
    AdminAuthChallengeVO generate();

    /**
     * 核验挑战答案
     *
     * @param challengeId 挑战唯一标识
     * @param answer      挑战答案
     * @return 是否正确
     */
    boolean validate(String challengeId, String answer);
}
