package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.module.sys.model.request.AdminAuthPasswordLoginRequest;
import cc.uncarbon.module.sys.model.response.SysUserLoginResult;

/**
 * 后台管理-登录
 */
public interface AdminLoginService {

    /**
     * 密码登录
     */
    SysUserLoginResult passwordLogin(AdminAuthPasswordLoginRequest request, VisitorContext visitorContext);

}
