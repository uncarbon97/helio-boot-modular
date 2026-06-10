package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.module.sys.model.request.AdminPasswordLoginRequest;
import cc.uncarbon.module.sys.model.response.AdminLoginResult;

/**
 * 后台管理-登录
 */
public interface AdminLoginService {

    /**
     * 密码登录
     */
    AdminLoginResult passwordLogin(AdminPasswordLoginRequest request, VisitorContext visitorContext);
}
