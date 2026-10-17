package cc.uncarbon.module.sys.errorcode;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.commons.errorcode.ErrorCodeBizGroup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link SysErrorCodeEnum} 单元测试
 */
class SysErrorCodeEnumTest {

    /**
     * 枚举常量集合应非空
     */
    @Test
    void valuesNotEmpty() {
        Assertions.assertTrue(SysErrorCodeEnum.values().length > 0);
    }

    /**
     * 每个常量的友好描述应非空文本
     */
    @Test
    void everyConstantHasNonBlankFriendlyMessage() {
        for (SysErrorCodeEnum item : SysErrorCodeEnum.values()) {
            Assertions.assertNotNull(item.getErrorMsgFriendly(), item + " 缺少友好描述");
            Assertions.assertFalse(item.getErrorMsgFriendly().isBlank(), item + " 友好描述为空文本");
        }
    }

    /**
     * 每个常量的错误码应等于枚举名
     */
    @Test
    void everyConstantErrorCodeEqualsEnumName() {
        for (SysErrorCodeEnum item : SysErrorCodeEnum.values()) {
            Assertions.assertEquals(item.name(), item.getErrorCode(), item + " 错误码应等于枚举名");
        }
    }

    /**
     * 每个常量名应符合 [A|B][01][CCC] 系统管理业务域格式
     */
    @Test
    void everyConstantFollowsSysBizGroupPattern() {
        String pattern = "^[AB]" + ErrorCodeBizGroup.SYS + "\\d{3}$";

        for (SysErrorCodeEnum item : SysErrorCodeEnum.values()) {
            Assertions.assertTrue(item.name().matches(pattern),
                    item + " 未命中 [A|B][01][CCC] 格式: " + pattern);
        }
    }

    /**
     * 已知常量应保留既定文案
     */
    @Test
    void knownConstantsKeepFriendlyMessages() {
        Assertions.assertEquals("账号或密码不正确", SysErrorCodeEnum.A01001.getErrorMsgFriendly());
        Assertions.assertEquals("不能使用 {} 作为角色编码", SysErrorCodeEnum.A01010.getErrorMsgFriendly());
        Assertions.assertEquals("登录组件错误，请联系管理员", SysErrorCodeEnum.B01001.getErrorMsgFriendly());
    }

    /**
     * throwIf 为真时应抛出携带模板参数的业务异常
     */
    @Test
    void throwIfFillsTemplateParams() {
        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> SysErrorCodeEnum.A01010.throwIf(true, "SuperAdmin"));

        Assertions.assertSame(SysErrorCodeEnum.A01010, ex.getErrorCode());
        Assertions.assertEquals("A01010", ex.getCode());
        Assertions.assertEquals("不能使用 SuperAdmin 作为角色编码", ex.getMessage());
    }

    /**
     * throwIf 为假时应正常返回
     */
    @Test
    void throwIfFalseDoesNotThrow() {
        Assertions.assertDoesNotThrow(() -> SysErrorCodeEnum.A01001.throwIf(false));
    }
}
