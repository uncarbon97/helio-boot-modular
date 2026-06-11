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

    public final String ADMIN = UserTypeCodeEnum.ADMIN_USER.getValue();
    public final String APP = UserTypeCodeEnum.APP_USER.getValue();

}
