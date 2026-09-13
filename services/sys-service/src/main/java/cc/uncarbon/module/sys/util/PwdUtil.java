package cc.uncarbon.module.sys.util;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.experimental.UtilityClass;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码哈希工具类
 * 使用内存困难型 Argon2id 算法, 哈希结果为标准 PHC 格式字符串, 盐值内嵌其中, 无需单独存储
 */
@UtilityClass
public class PwdUtil {

    /**
     * 盐值长度（字节）
     */
    private static final int SALT_LENGTH_BYTES = 16;
    /**
     * 哈希长度（字节）
     */
    private static final int HASH_LENGTH_BYTES = 32;

    // 以下为 OWASP 推荐的 Argon2id 参数
    /**
     * 内存开销 KiB
     */
    private static final int MEMORY_KIB = 19456;
    /**
     * 迭代次数
     */
    private static final int ITERATIONS = 2;
    /**
     * 并行度
     */
    private static final int PARALLELISM = 1;

    /**
     * 校验时允许的参数上限, 防御库中哈希串被篡改后导致的资源耗尽
     */
    private static final int MAX_MEMORY_KIB = 1 << 20;
    private static final int MAX_ITERATIONS = 64;
    private static final int MAX_PARALLELISM = 8;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder().withoutPadding();
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();
    private static final int ARGON2_VERSION = Argon2Parameters.ARGON2_VERSION_13;

    /**
     * 生成密码哈希
     *
     * @return PHC 格式字符串, 形如 $argon2id$v=19$m=19456,t=2,p=1$<盐值>$<哈希>
     */
    public static String hash(String rawPwd) {
        if (CharSequenceUtil.isEmpty(rawPwd)) {
            return "";
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);

        byte[] hash = argon2id(MEMORY_KIB, ITERATIONS, PARALLELISM, salt, rawPwd);
        return "$argon2id$v=" + ARGON2_VERSION
                + "$m=" + MEMORY_KIB + ",t=" + ITERATIONS + ",p=" + PARALLELISM
                + "$" + B64_ENCODER.encodeToString(salt)
                + "$" + B64_ENCODER.encodeToString(hash);
    }

    /**
     * 校验密码是否与库中 PHC 格式哈希串匹配
     */
    public static boolean verify(String rawPwd, String storedHash) {
        if (CharSequenceUtil.isEmpty(rawPwd) || CharSequenceUtil.isEmpty(storedHash)) {
            return false;
        }

        // 按期望拆分: ['', 'argon2id', 'v=19', 'm=...,t=...,p=...', 盐值, 哈希]
        String[] parts = storedHash.split("\\$");
        if (parts.length != 6
                || !"argon2id".equals(parts[1])
                || !("v=" + ARGON2_VERSION).equals(parts[2])) {
            return false;
        }

        int memoryKib = 0;
        int iterations = 0;
        int parallelism = 0;
        try {
            for (String kv : parts[3].split(",")) {
                String[] pair = kv.split("=", 2);
                if (pair.length != 2) {
                    return false;
                }
                int value = Integer.parseInt(pair[1]);
                switch (pair[0]) {
                    case "m" -> memoryKib = value;
                    case "t" -> iterations = value;
                    case "p" -> parallelism = value;
                    default -> {
                        return false;
                    }
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }
        if (memoryKib <= 0 || memoryKib > MAX_MEMORY_KIB
                || iterations <= 0 || iterations > MAX_ITERATIONS
                || parallelism <= 0 || parallelism > MAX_PARALLELISM) {
            return false;
        }

        byte[] salt;
        byte[] expectedHash;
        try {
            salt = B64_DECODER.decode(parts[4]);
            expectedHash = B64_DECODER.decode(parts[5]);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (salt.length == 0 || expectedHash.length == 0) {
            return false;
        }

        byte[] actualHash = argon2id(memoryKib, iterations, parallelism, salt, rawPwd);
        // 常数时间比较, 防时序侧信道
        return MessageDigest.isEqual(actualHash, expectedHash);
    }

    /**
     * 执行 Argon2id 计算
     */
    private static byte[] argon2id(int memoryKib, int iterations, int parallelism, byte[] salt, String rawPwd) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(ARGON2_VERSION)
                .withIterations(iterations)
                .withMemoryAsKB(memoryKib)
                .withParallelism(parallelism)
                .withSalt(salt)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);
        byte[] hash = new byte[HASH_LENGTH_BYTES];
        generator.generateBytes(rawPwd.toCharArray(), hash);
        return hash;
    }
}
