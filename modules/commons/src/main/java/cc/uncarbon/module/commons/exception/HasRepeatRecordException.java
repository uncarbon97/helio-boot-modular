package cc.uncarbon.module.commons.exception;

import cc.uncarbon.framework.helium.base.errorcode.StructuredErrorCode;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.commons.errorcode.DefaultErrorCodeEnum;

/**
 * 存在重复记录异常
 */
public class HasRepeatRecordException extends BusinessException {

    public HasRepeatRecordException(String errorMsg) {
        super(DefaultErrorCodeEnum.A00001.getErrorCode(), errorMsg);
    }

    public HasRepeatRecordException(StructuredErrorCode errorCode) {
        super(errorCode);
    }
}
