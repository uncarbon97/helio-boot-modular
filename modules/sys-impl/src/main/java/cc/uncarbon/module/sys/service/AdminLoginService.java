package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.module.sys.model.request.AdminAuthPasswordLoginRequest;
import cc.uncarbon.module.sys.model.response.SysUserLoginResult;
import org.jspecify.annotations.Nullable;

/**
 * 后台管理-登录
 */
public interface AdminLoginService {

    /**
     * 密码登录
     */
    SysUserLoginResult passwordLogin(AdminAuthPasswordLoginRequest request, VisitorContext visitorContext);

    /**
     * 重建后台管理用户的会话上下文（仅包含启用状态的角色）
     * 用于用户-角色关系变化后原位刷新 sa-token 会话快照，无需用户重新登录
     *
     * @return 用户已不存在或被禁用时返回 null
     */
    @Nullable
    UserContext buildSessionUserContext(Long userId);

}
