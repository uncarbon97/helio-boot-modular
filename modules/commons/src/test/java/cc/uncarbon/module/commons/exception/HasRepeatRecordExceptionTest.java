package cc.uncarbon.module.commons.exception;

import cc.uncarbon.module.commons.errorcode.DefaultErrorCodeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link HasRepeatRecordException} 单元测试
 */
class HasRepeatRecordExceptionTest {

    /**
     * 字符串构造器应使用默认重复数据错误码 A00001
     */
    @Test
    void messageConstructorUsesDefaultRepeatErrorCode() {
        HasRepeatRecordException ex = new HasRepeatRecordException("已存在相同的账号");

        Assertions.assertEquals("A00001", ex.getCode());
        Assertions.assertEquals("已存在相同的账号", ex.getMessage());
        Assertions.assertNull(ex.getErrorCode());
    }

    /**
     * 结构化错误码构造器应携带原始错误码对象
     */
    @Test
    void errorCodeConstructorCarriesStructuredErrorCode() {
        HasRepeatRecordException ex = new HasRepeatRecordException(DefaultErrorCodeEnum.A00003);

        Assertions.assertSame(DefaultErrorCodeEnum.A00003, ex.getErrorCode());
        Assertions.assertEquals("A00003", ex.getCode());
        Assertions.assertEquals("长度不得大于 {}", ex.getMessage());
        Assertions.assertNotNull(ex.getTemplateParams());
        Assertions.assertEquals(0, ex.getTemplateParams().length);
    }

    /**
     * 异常应为运行时异常且关闭爬栈
     */
    @Test
    void exceptionIsRuntimeAndNoStackFilled() {
        HasRepeatRecordException ex = new HasRepeatRecordException("重复");

        Assertions.assertInstanceOf(RuntimeException.class, ex);
        Assertions.assertSame(ex, ex.fillInStackTrace());
    }
}
