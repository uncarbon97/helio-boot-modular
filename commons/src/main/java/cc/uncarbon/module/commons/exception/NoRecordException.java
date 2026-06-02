package cc.uncarbon.module.commons.exception;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.commons.enums.DefaultErrorCodeEnum;

public class NoRecordException extends BusinessException {

    public NoRecordException() {
        super(DefaultErrorCodeEnum.A00002);
    }

    public static <T> T throwIfNull(T obj) {
        if (obj == null) {
            throw new NoRecordException();
        }
        return obj;
    }

    public static void throwIfFalse(boolean val) {
        if (!val) {
            throw new NoRecordException();
        }
    }
}
