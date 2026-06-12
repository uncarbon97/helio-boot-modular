package cc.uncarbon.module.commons.satoken;

import cc.uncarbon.module.commons.enums.UserTypeCodeEnum;
import lombok.experimental.UtilityClass;

/**
 * SA-Token 账户体系分类
 *
 * @see <a href="https://sa-token.cc/doc.html#/up/many-account">...</a>
 */
@UtilityClass
public class StpLoginType {

    /**
     * @see UserTypeCodeEnum#ADMIN_USER
     */
    public final String ADMIN = "ADMIN_USER";

    /**
     * @see UserTypeCodeEnum#APP_USER
     */
    public final String APP = "APP_USER";

}
