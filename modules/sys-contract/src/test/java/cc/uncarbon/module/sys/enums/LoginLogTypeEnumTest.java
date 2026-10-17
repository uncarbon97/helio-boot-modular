package cc.uncarbon.module.sys.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * {@link LoginLogTypeEnum} 单元测试
 */
class LoginLogTypeEnumTest {

    /**
     * 已知常量的取值与标签应一一对应
     */
    @Test
    void knownConstantsValuesAndLabels() {
        Assertions.assertEquals(1, LoginLogTypeEnum.PASSWORD_LOGIN.getValue());
        Assertions.assertEquals("密码登录", LoginLogTypeEnum.PASSWORD_LOGIN.getLabel());
        Assertions.assertEquals(2, LoginLogTypeEnum.LOGOUT.getValue());
        Assertions.assertEquals("登出", LoginLogTypeEnum.LOGOUT.getLabel());
        Assertions.assertEquals(2, LoginLogTypeEnum.values().length);
    }

    /**
     * 所有常量应有取值且标签非空
     */
    @Test
    void everyConstantHasValueAndNonBlankLabel() {
        for (LoginLogTypeEnum item : LoginLogTypeEnum.values()) {
            Assertions.assertNotNull(item.getValue());
            Assertions.assertFalse(item.getLabel().isBlank());
        }
    }

    /**
     * BaseEnum.of 应支持密码登录与登出按 value 反查
     */
    @Test
    void baseEnumOfRoundTrip() {
        Optional<LoginLogTypeEnum> login = BaseEnum.of(LoginLogTypeEnum.class, 1);
        Assertions.assertTrue(login.isPresent());
        Assertions.assertSame(LoginLogTypeEnum.PASSWORD_LOGIN, login.get());

        Optional<LoginLogTypeEnum> logout = BaseEnum.of(LoginLogTypeEnum.class, 2);
        Assertions.assertTrue(logout.isPresent());
        Assertions.assertSame(LoginLogTypeEnum.LOGOUT, logout.get());
    }

    /**
     * BaseEnum.of 对未知取值应返回空 Optional
     */
    @Test
    void baseEnumOfReturnsEmptyForUnknownValue() {
        Assertions.assertFalse(BaseEnum.of(LoginLogTypeEnum.class, 3).isPresent());
    }
}
