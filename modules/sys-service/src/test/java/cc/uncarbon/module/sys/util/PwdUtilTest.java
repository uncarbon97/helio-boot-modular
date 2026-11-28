package cc.uncarbon.module.sys.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link PwdUtil} Argon2id 哈希/校验分支测试
 */
class PwdUtilTest {

    private static final String RAW = "pass12345678";


    @Test
    void hashProducesPhcFormat() {
        String hash = PwdUtil.hash(RAW);

        Assertions.assertTrue(hash.startsWith("$argon2id$v=19$m=19456,t=2,p=1$"), hash);
        Assertions.assertEquals(6, hash.split("\\$").length);
    }

    @Test
    void hashEmptyPwdReturnsEmpty() {
        Assertions.assertEquals("", PwdUtil.hash(""));
        Assertions.assertEquals("", PwdUtil.hash(null));
    }

    @Test
    void verifyRoundTrip() {
        String hash = PwdUtil.hash(RAW);

        Assertions.assertTrue(PwdUtil.verify(RAW, hash));
        Assertions.assertFalse(PwdUtil.verify(RAW + "x", hash));
    }

    @Test
    void verifyBlankInputReturnsFalse() {
        Assertions.assertFalse(PwdUtil.verify(null, "$argon2id$v=19$m=19456,t=2,p=1$AAAA$BBBB"));
        Assertions.assertFalse(PwdUtil.verify(RAW, null));
        Assertions.assertFalse(PwdUtil.verify("", ""));
    }

    @Test
    void verifyRejectsMalformedStoredHash() {
        // 段数不对
        Assertions.assertFalse(PwdUtil.verify(RAW, "$argon2id$v=19$m=19456,t=2,p=1$AAAA"));
        // 算法不对
        Assertions.assertFalse(PwdUtil.verify(RAW, "$argon2i$v=19$m=19456,t=2,p=1$AAAA$BBBB"));
        // 版本不对
        Assertions.assertFalse(PwdUtil.verify(RAW, "$argon2id$v=16$m=19456,t=2,p=1$AAAA$BBBB"));
        // 未知参数键
        Assertions.assertFalse(PwdUtil.verify(RAW, "$argon2id$v=19$x=1$m=19456$AAAA$BBBB"));
        // 非数字参数
        Assertions.assertFalse(PwdUtil.verify(RAW, "$argon2id$v=19$m=abc,t=2,p=1$AAAA$BBBB"));
        // 悬空等号
        Assertions.assertFalse(PwdUtil.verify(RAW, "$argon2id$v=19$m=19456,t=2$AAAA$BBBB"));
    }

    @Test
    void verifyRejectsParameterDoSAttempts() {
        String hash = PwdUtil.hash(RAW);
        // 替换参数为超大值，超出上限直接拒绝，不进入哈希计算
        String oversized = hash.replaceFirst("m=19456", "m=" + (2 << 20));
        Assertions.assertFalse(PwdUtil.verify(RAW, oversized));
        String tooManyIters = hash.replaceFirst("t=2", "t=65");
        Assertions.assertFalse(PwdUtil.verify(RAW, tooManyIters));
        String tooMuchParallel = hash.replaceFirst("p=1", "p=9");
        Assertions.assertFalse(PwdUtil.verify(RAW, tooMuchParallel));
        String zeroMemory = hash.replaceFirst("m=19456", "m=0");
        Assertions.assertFalse(PwdUtil.verify(RAW, zeroMemory));
    }

    @Test
    void verifyRejectsBadBase64OrEmptyParts() {
        // 非法 Base64 字符
        Assertions.assertFalse(PwdUtil.verify(RAW, "$argon2id$v=19$m=19456,t=2,p=1$****????$BBBB"));
        // 空 Base64 解出空字节数组
        Assertions.assertFalse(PwdUtil.verify(RAW, "$argon2id$v=19$m=19456,t=2,p=1$$$$"));
    }

    @Test
    void hashIsSalted() {
        Assertions.assertNotEquals(PwdUtil.hash(RAW), PwdUtil.hash(RAW));
    }
}
