package cc.uncarbon.module.sys.dal.typehandler;

import cc.uncarbon.framework.helium.db.mybatisplus.handler.EncryptedFieldTypeHandler;
import cc.uncarbon.module.sys.constant.SysUserCipherCols;

/**
 * sys_user.email_encrypted 列级加密 TypeHandler
 *
 * <p>columnAad 为冻结常量：改名 = 该列全部历史密文不可解</p>
 *
 * @author Uncarbon
 */
public class SysUserEmailEncryptedTypeHandler extends EncryptedFieldTypeHandler {

    @Override
    protected String columnAad() {
        return SysUserCipherCols.EMAIL_CIPHER_AAD;
    }
}
