package cc.uncarbon.module.commons.satoken;

import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cn.dev33.satoken.stp.StpLogic;

/**
 * StpLogic 门面类，管理项目中所有的 StpLogic 账号体系
 *
 * @see <a href="https://sa-token.cc/doc.html#/up/many-account">...</a>
 */
public class StpKit {

    /**
     * 管理 Admin 账户体系下所有账号的登录、权限认证
     * 关联 {@link ApiPathPrefix#ADMIN} 前缀
     */
    public static final StpLogic ADMIN = new StpLogic(StpLoginType.ADMIN);

    /**
     * 管理 App 账户体系下所有账号的登录、权限认证
     * 关联 {@link ApiPathPrefix#APP} 前缀
     */
    public static final StpLogic APP = new StpLogic(StpLoginType.APP);

}
