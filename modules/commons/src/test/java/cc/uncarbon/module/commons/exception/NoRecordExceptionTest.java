package cc.uncarbon.module.commons.exception;

import cc.uncarbon.module.commons.errorcode.DefaultErrorCodeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link NoRecordException} 单元测试
 */
class NoRecordExceptionTest {

    /**
     * 默认构造器应使用不存在数据错误码 A00002
     */
    @Test
    void defaultConstructorUsesNoRecordErrorCode() {
        NoRecordException ex = new NoRecordException();

        Assertions.assertSame(DefaultErrorCodeEnum.A00002, ex.getErrorCode());
        Assertions.assertEquals("A00002", ex.getCode());
        Assertions.assertEquals("不存在数据", ex.getMessage());
    }

    /**
     * 对象为 null 时 throwIfNull 应抛出异常
     */
    @Test
    void throwIfNullThrowsWhenNull() {
        NoRecordException ex = Assertions.assertThrows(NoRecordException.class,
                () -> NoRecordException.throwIfNull(null));

        Assertions.assertSame(DefaultErrorCodeEnum.A00002, ex.getErrorCode());
    }

    /**
     * 对象非 null 时 throwIfNull 应原样返回同一对象
     */
    @Test
    void throwIfNullReturnsSameObjectWhenNotNull() {
        Object obj = new Object();

        Object returned = NoRecordException.throwIfNull(obj);

        Assertions.assertSame(obj, returned);
    }

    /**
     * 泛型场景下 throwIfNull 应保留传入类型
     */
    @Test
    void throwIfNullReturnsNullTypedValue() {
        String value = NoRecordException.throwIfNull("文本");

        Assertions.assertEquals("文本", value);
    }

    /**
     * 表达式为假时 throwIfFalse 应抛出异常
     */
    @Test
    void throwIfFalseThrowsWhenFalse() {
        Assertions.assertThrows(NoRecordException.class, () -> NoRecordException.throwIfFalse(false));
    }

    /**
     * 表达式为真时 throwIfFalse 应正常返回
     */
    @Test
    void throwIfFalseDoesNothingWhenTrue() {
        Assertions.assertDoesNotThrow(() -> NoRecordException.throwIfFalse(true));
    }
}
