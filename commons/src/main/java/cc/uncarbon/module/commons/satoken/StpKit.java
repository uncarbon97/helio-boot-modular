package cc.uncarbon.module.commons.satoken;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;

/**
 * StpLogic 门面类，管理项目中所有的 StpLogic 账号体系
 *
 * @see <a href="https://sa-token.cc/doc.html#/up/many-account">...</a>
 */
public class StpKit {

    /**
     * 默认原生会话对象
     */
    public static final StpLogic DEFAULT = StpUtil.stpLogic;

    /**
     * 管理 Admin 账户体系下所有账号的登录、权限认证
     */
    public static final StpLogic ADMIN = new StpLogic(StpLoginType.ADMIN);

}
