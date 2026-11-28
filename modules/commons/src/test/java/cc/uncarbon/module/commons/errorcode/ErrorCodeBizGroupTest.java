package cc.uncarbon.module.commons.errorcode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

/**
 * {@link ErrorCodeBizGroup} 单元测试
 */
class ErrorCodeBizGroupTest {

    /**
     * 各业务域常量应保持预期取值
     */
    @Test
    void allGroupConstantsHaveExpectedValues() {
        Assertions.assertEquals("00", ErrorCodeBizGroup.UNCLASSIFIED);
        Assertions.assertEquals("01", ErrorCodeBizGroup.SYS);
        Assertions.assertEquals("02", ErrorCodeBizGroup.FILE);
        Assertions.assertEquals("03", ErrorCodeBizGroup.TENANT);
        Assertions.assertEquals("04", ErrorCodeBizGroup.ADMIN_API);
    }

    /**
     * 所有业务域常量应为 2 位数字串
     */
    @Test
    void allGroupConstantsAreTwoDigitStrings() {
        String[] groups = {
                ErrorCodeBizGroup.UNCLASSIFIED,
                ErrorCodeBizGroup.SYS,
                ErrorCodeBizGroup.FILE,
                ErrorCodeBizGroup.TENANT,
                ErrorCodeBizGroup.ADMIN_API
        };

        for (String group : groups) {
            Assertions.assertNotNull(group);
            Assertions.assertTrue(group.matches("\\d{2}"), "业务域段应为 2 位数字: " + group);
        }
    }

    /**
     * 各业务域常量取值互不重复
     */
    @Test
    void groupValuesAreUnique() {
        Assertions.assertEquals(5, Stream.of(
                        ErrorCodeBizGroup.UNCLASSIFIED,
                        ErrorCodeBizGroup.SYS,
                        ErrorCodeBizGroup.FILE,
                        ErrorCodeBizGroup.TENANT,
                        ErrorCodeBizGroup.ADMIN_API)
                .distinct().count());
    }
}
