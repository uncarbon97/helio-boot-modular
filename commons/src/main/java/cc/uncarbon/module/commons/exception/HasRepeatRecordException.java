package cc.uncarbon.module.commons.exception;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.commons.enums.DefaultErrorCodeEnum;

public class HasRepeatRecordException extends BusinessException {

    public HasRepeatRecordException(String errorMsg) {
        super(DefaultErrorCodeEnum.A00001.getErrorCode(), errorMsg);
    }
}
