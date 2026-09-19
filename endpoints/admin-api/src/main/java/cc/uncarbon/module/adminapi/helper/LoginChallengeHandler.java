package cc.uncarbon.module.adminapi.helper;

import cc.uncarbon.module.adminapi.model.response.AdminAuthChallengeVO;

/**
 * 登录挑战处理器
 * 新增验证码形式（如滑块）时，实现此接口并注册为 Spring 组件即可
 *
 * @author Uncarbon
 */
public interface LoginChallengeHandler {

    /**
     * 挑战类型标识；对应配置 admin-api.login-challenge.strategy（小写）
     */
    String type();

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
