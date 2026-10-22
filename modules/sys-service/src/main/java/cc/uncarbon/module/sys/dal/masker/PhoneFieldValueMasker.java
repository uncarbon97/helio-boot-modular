package cc.uncarbon.module.sys.dal.masker;

import cc.uncarbon.framework.helium.fieldcrypto.annotation.FieldValueMasker;
import cc.uncarbon.module.commons.util.MaskUtil;

/**
 * 手机号脱敏器（masked 冗余列自动填充）
 *
 * <p>口径与 {@link MaskUtil#maskPhone(String)} 同源：18967038927 -&gt; 189****8927。</p>
 *
 * @author Uncarbon
 */
public class PhoneFieldValueMasker implements FieldValueMasker {

    @Override
    public String mask(String plainValue) {
        return MaskUtil.maskPhone(plainValue);
    }
}
