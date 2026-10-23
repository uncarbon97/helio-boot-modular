package cc.uncarbon.module.sys.dal.fieldcrypto;

import cc.uncarbon.framework.helium.db.fieldcrypto.field.FieldValueMasker;
import cc.uncarbon.module.commons.util.MaskUtil;

/**
 * 邮箱脱敏器
 *
 * @author Uncarbon
 */
public class EmailFieldValueMasker implements FieldValueMasker {

    @Override
    public String mask(String plainValue) {
        return MaskUtil.maskEmail(plainValue);
    }
}
