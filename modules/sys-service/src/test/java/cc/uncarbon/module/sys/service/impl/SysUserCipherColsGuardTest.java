package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.fieldcrypto.annotation.BlindIndexOf;
import cc.uncarbon.framework.helium.fieldcrypto.annotation.BlindIndexValueExtractors;
import cc.uncarbon.module.sys.constant.SysUserCipherCols;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * sys_user 密文列 wrapper 旁路防护（静态规则）
 *
 * <p>规则一（条件封口）：main 源码中 {@code SysUserEntity::getPhoneNo} / {@code getEmail}
 * 不得进任何 wrapper 条件（明文 getter 进条件 = 对密文列做明文等值/LIKE，静默查空）；
 * 唯一白名单 {@code SysUserMapper}（clearPiiContact 的 .set 路径，只封条件不封 set）。</p>
 *
 * <p>规则二/三（同源断言）：{@code SysUserCipherCols} 的 purpose/aad 及查询侧 extractor
 * 必须与实体注解声明一致。自带违规样例自测（探测器能命中且干净行不误报）。</p>
 */
class SysUserCipherColsGuardTest {

    private static final Pattern CIPHER_GETTER_IN_CONDITION = Pattern.compile(
            "SysUserEntity::(getPhoneNo|getEmail)\\b[^;]*" +
                    "|\\.(eq|ne|like|notLike|in|notIn|gt|ge|lt|le|having|orderBy)\\s*\\([^)]*SysUserEntity::(getPhoneNo|getEmail)\\b");

    private static final Pattern PLAIN_GETTER_REF = Pattern.compile("SysUserEntity::(getPhoneNo|getEmail)\\b");

    private static final String WHITELIST_FILE = "SysUserMapper.java";

    @Test
    void noCipherGetterInWrapperConditions() throws IOException {
        List<String> violations = scanMainSources();
        Assertions.assertTrue(violations.isEmpty(),
                "密文列旁路违规（getPhoneNo/getEmail 进 wrapper 或主源码直接引用，白名单外）:\n"
                        + String.join("\n", violations));
    }

    @Test
    void detectorSelfTest() {
        // 违规样例 1：明文 getter 进 eq 条件
        Assertions.assertTrue(isFlagged(".eq(SysUserEntity::getPhoneNo, keyword)"));
        // 违规样例 2：邮箱 getter 直接引用（主源码白名单外）
        Assertions.assertTrue(isFlagged("wrapper.in(SysUserEntity::getEmail, ids)"));
        // 干净样例：派生列 getter（getEmailFullBlindIndex / getPhoneNoTail4BlindIndex）不误报
        Assertions.assertFalse(isFlagged(".eq(SysUserEntity::getPhoneNoTail4BlindIndex, tailIndex)"));
        Assertions.assertFalse(isFlagged(".eq(SysUserEntity::getEmailFullBlindIndex, emailIndex)"));
        Assertions.assertFalse(isFlagged(".eq(SysUserEntity::getPin, pin)"));
        // 干净样例：专用清空方法的 set 路径（在白名单文件内，本自测仅验证正则不封 set）
        Assertions.assertFalse(CIPHER_GETTER_IN_CONDITION.matcher(".set(SysUserEntity::getPhoneNo, null)").find());
    }

    @Test
    void entityAnnotationsMatchConstants() {
        Assertions.assertEquals(SysUserCipherCols.PHONE_FULL_PURPOSE, purposeOf("phoneNoFullBlindIndex"));
        Assertions.assertEquals(SysUserCipherCols.PHONE_TAIL4_PURPOSE, purposeOf("phoneNoTail4BlindIndex"));
        Assertions.assertEquals(SysUserCipherCols.EMAIL_FULL_PURPOSE, purposeOf("emailFullBlindIndex"));
        Assertions.assertEquals(BlindIndexValueExtractors.CleanTrim.class, extractorOf("phoneNoFullBlindIndex"));
        Assertions.assertEquals(BlindIndexValueExtractors.TailDigits.class, extractorOf("phoneNoTail4BlindIndex"));
        Assertions.assertEquals(BlindIndexValueExtractors.LowerTrim.class, extractorOf("emailFullBlindIndex"));
    }

    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    private static boolean isFlagged(String codeLine) {
        return PLAIN_GETTER_REF.matcher(codeLine).find();
    }

    private static List<String> scanMainSources() throws IOException {
        Path moduleRoot = Paths.get(System.getProperty("user.dir")).resolve("src/main/java");
        if (!Files.isDirectory(moduleRoot)) {
            // IDE 工作目录可能定在模块根或仓库根，两者都尝试
            moduleRoot = Paths.get(System.getProperty("user.dir"))
                    .resolve("modules/sys-service/src/main/java");
        }
        Assertions.assertTrue(Files.isDirectory(moduleRoot), "找不到 sys-service 主源码目录: " + moduleRoot);

        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(moduleRoot)) {
            files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.getFileName().toString().equals(WHITELIST_FILE))
                    .forEach(p -> {
                        try {
                            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                            for (int i = 0; i < lines.size(); i++) {
                                if (isFlagged(lines.get(i))) {
                                    violations.add(p + ":" + (i + 1) + " -> " + lines.get(i).trim());
                                }
                            }
                        } catch (IOException e) {
                            throw new IllegalStateException("读取源码失败: " + p, e);
                        }
                    });
        }
        return violations;
    }

    private static String purposeOf(String fieldName) {
        BlindIndexOf annotation = annotationOf(fieldName);
        return annotation.purpose();
    }

    private static Class<?> extractorOf(String fieldName) {
        return annotationOf(fieldName).extractor();
    }

    private static BlindIndexOf annotationOf(String fieldName) {
        for (Class<?> clazz = SysUserEntity.class; clazz != null && clazz != Object.class;
             clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals(fieldName)) {
                    BlindIndexOf annotation = field.getAnnotation(BlindIndexOf.class);
                    Assertions.assertNotNull(annotation, fieldName + " 缺少 @BlindIndexOf 注解");
                    return annotation;
                }
            }
        }
        throw new IllegalStateException("SysUserEntity 上找不到字段 " + fieldName);
    }
}
