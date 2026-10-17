package cc.uncarbon.module.commons.satoken;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link StpKit} 单元测试
 */
class StpKitTest {

    /**
     * ADMIN 门面应绑定后台管理账户体系
     */
    @Test
    void adminStpLogicBindsAdminLoginType() {
        Assertions.assertNotNull(StpKit.ADMIN);
        Assertions.assertEquals(StpLoginType.ADMIN, StpKit.ADMIN.getLoginType());
    }

    /**
     * APP 门面应绑定 C 端账户体系
     */
    @Test
    void appStpLogicBindsAppLoginType() {
        Assertions.assertNotNull(StpKit.APP);
        Assertions.assertEquals(StpLoginType.APP, StpKit.APP.getLoginType());
    }

    /**
     * ADMIN 与 APP 应为相互独立的账户体系
     */
    @Test
    void adminAndAppAreDistinctAccountSystems() {
        Assertions.assertNotSame(StpKit.ADMIN, StpKit.APP);
        Assertions.assertNotEquals(StpKit.ADMIN.getLoginType(), StpKit.APP.getLoginType());
    }

    /**
     * 登录类型常量应与 UserTypeCodeEnum 枚举名对齐
     */
    @Test
    void loginTypesAlignWithUserTypeCodeEnum() {
        Assertions.assertEquals("ADMIN_USER", StpLoginType.ADMIN);
        Assertions.assertEquals("APP_USER", StpLoginType.APP);
    }
}
