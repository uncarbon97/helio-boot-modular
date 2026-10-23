package cc.uncarbon.module.sys.dal.fieldcrypto;

import cc.uncarbon.framework.helium.db.fieldcrypto.typehandler.EncryptedFieldTypeHandler;
import cc.uncarbon.module.sys.constant.SysUserCipherCols;

/**
 * 邮箱列级加密
 *
 * @author Uncarbon
 */
public class EmailEncryptedTypeHandler extends EncryptedFieldTypeHandler {

    @Override
    public String columnPath() {
        return SysUserCipherCols.EMAIL_COLUMN_PATH;
    }
}
