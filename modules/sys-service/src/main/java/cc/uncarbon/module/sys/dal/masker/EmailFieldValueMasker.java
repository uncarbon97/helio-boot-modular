package cc.uncarbon.module.sys.dal.masker;

import cc.uncarbon.framework.helium.fieldcrypto.annotation.FieldValueMasker;
import cc.uncarbon.module.commons.util.PiiMaskUtil;

/**
 * 邮箱脱敏器（masked 冗余列自动填充）
 *
 * <p>口径与 {@link PiiMaskUtil#maskEmail(String)} 同源：alicealex@example.com -&gt; al***@example.com。</p>
 *
 * @author Uncarbon
 */
public class EmailFieldValueMasker implements FieldValueMasker {

    @Override
    public String mask(String plainValue) {
        return PiiMaskUtil.maskEmail(plainValue);
    }
}
