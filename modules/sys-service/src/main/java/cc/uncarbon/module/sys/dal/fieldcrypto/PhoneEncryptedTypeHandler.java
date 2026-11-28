package cc.uncarbon.module.sys.dal.fieldcrypto;

import cc.uncarbon.framework.helium.db.fieldcrypto.typehandler.EncryptedFieldTypeHandler;

/**
 * 手机号列级加密
 *
 * @author Uncarbon
 */
public class PhoneEncryptedTypeHandler extends EncryptedFieldTypeHandler {

    @Override
    public String columnPath() {
        return SysUserCipherCols.PHONE_COLUMN_PATH;
    }
}
