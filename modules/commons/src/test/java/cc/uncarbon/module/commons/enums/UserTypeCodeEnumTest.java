package cc.uncarbon.module.commons.enums;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * {@link UserTypeCodeEnum} 单元测试
 */
class UserTypeCodeEnumTest {

    /**
     * 所有常量的取值应为枚举名且标签非空
     */
    @Test
    void allConstantsHaveNameValueAndNonBlankLabel() {
        for (UserTypeCodeEnum item : UserTypeCodeEnum.values()) {
            Assertions.assertEquals(item.name(), item.getValue(), "getValue 应返回枚举名");
            Assertions.assertNotNull(item.getLabel());
            Assertions.assertFalse(item.getLabel().isBlank(), "label 不能为空文本");
        }
    }

    /**
     * 已知常量与标签应一一对应
     */
    @Test
    void knownConstantsAndLabels() {
        Assertions.assertEquals("后台管理用户", UserTypeCodeEnum.ADMIN_USER.getLabel());
        Assertions.assertEquals("C端用户", UserTypeCodeEnum.APP_USER.getLabel());
        Assertions.assertEquals(2, UserTypeCodeEnum.values().length);
    }

    /**
     * ofName 应支持所有常量名往返反查
     */
    @Test
    void ofNameRoundTripForAllConstants() {
        for (UserTypeCodeEnum item : UserTypeCodeEnum.values()) {
            Assertions.assertSame(item, UserTypeCodeEnum.ofName(item.name()));
        }
    }

    /**
     * 未知名称、null、空串反查应返回 null
     */
    @Test
    void ofNameReturnsNullForUnknownName() {
        Assertions.assertNull(UserTypeCodeEnum.ofName("NOT_EXIST"));
        Assertions.assertNull(UserTypeCodeEnum.ofName(null));
        Assertions.assertNull(UserTypeCodeEnum.ofName(""));
    }

    /**
     * ofName 应区分大小写
     */
    @Test
    void ofNameIsCaseSensitive() {
        Assertions.assertNull(UserTypeCodeEnum.ofName("admin_user"));
    }

    /**
     * BaseEnum.of 应支持精确值与忽略大小写匹配
     */
    @Test
    void baseEnumOfMatchesByValueIgnoringCase() {
        Optional<UserTypeCodeEnum> exact = BaseEnum.of(UserTypeCodeEnum.class, "ADMIN_USER");
        Assertions.assertTrue(exact.isPresent());
        Assertions.assertSame(UserTypeCodeEnum.ADMIN_USER, exact.get());

        Optional<UserTypeCodeEnum> caseInsensitive = BaseEnum.of(UserTypeCodeEnum.class, "app_user");
        Assertions.assertTrue(caseInsensitive.isPresent());
        Assertions.assertSame(UserTypeCodeEnum.APP_USER, caseInsensitive.get());
    }

    /**
     * BaseEnum.of 对未知值应返回空 Optional
     */
    @Test
    void baseEnumOfReturnsEmptyForUnknownValue() {
        Assertions.assertFalse(BaseEnum.of(UserTypeCodeEnum.class, "NOT_EXIST").isPresent());
    }
}
