package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.module.sys.model.request.AdminAuthPasswordLoginRequest;
import cc.uncarbon.module.sys.model.response.SysUserLoginResult;
import cc.uncarbon.module.sys.model.valueobj.AdminLoginTenantUIConfigVO;
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

    /**
     * 重建后台管理用户的会话上下文，角色按指定租户作用域解析（仅包含启用状态的角色）
     * <p>用于 USER_FIRST 切换租户后原位刷新权限快照；超级管理员权限与视角解耦，仍按归属租户解析</p>
     *
     * @return 用户已不存在或被禁用时返回 null
     */
    @Nullable
    UserContext buildSessionUserContext(Long userId, TenantContext tenantContext);

    /**
     * 后台管理-登录页，控制前端租户相关 UI
     */
    AdminLoginTenantUIConfigVO getTenantUIConfig();

}
