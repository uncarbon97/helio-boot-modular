package cc.uncarbon.module.adminapi.helper;

import cc.uncarbon.module.adminapi.props.HashidsProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link HashidsHelper} 文件ID不透明编解码分支测试
 */
class HashidsHelperTest {

    private final HashidsHelper helper = new HashidsHelper(
            new HashidsProperties().setSalt("test-salt-value").setMinLength(8));


    @Test
    void encodeDecodeRoundTrip() {
        String encoded = helper.encode(123456789L);

        Assertions.assertNotNull(encoded);
        Assertions.assertTrue(encoded.length() >= 8);
        Assertions.assertEquals(123456789L, helper.decode(encoded));
    }

    @Test
    void encodeDifferentIdsYieldDifferentValues() {
        Assertions.assertNotEquals(helper.encode(1L), helper.encode(2L));
    }

    @Test
    void decodeInvalidStringYieldsNull() {
        Assertions.assertNull(helper.decode("!!!!!"));
    }
}
