package cc.uncarbon.module.commons.enumdict;

import cc.uncarbon.module.commons.enums.UserTypeCodeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link EnumDictSpec} 单元测试
 */
class EnumDictSpecTest {

    @Test
    void ofMapsAllConstantsInOrdinalOrder() {
        EnumDictSpec spec = EnumDictSpec.of("user_type", "用户类型", "描述",
                UserTypeCodeEnum.class, UserTypeCodeEnum::getValue, UserTypeCodeEnum::getLabel);

        Assertions.assertEquals("user_type", spec.code());
        Assertions.assertEquals("用户类型", spec.name());
        Assertions.assertEquals("描述", spec.description());
        Assertions.assertEquals(2, spec.items().size());

        EnumDictSpec.Item first = spec.items().getFirst();
        Assertions.assertEquals("ADMIN_USER", first.code());
        Assertions.assertEquals("ADMIN_USER", first.value());
        Assertions.assertEquals("后台管理用户", first.label());
        Assertions.assertEquals(0, first.sort());

        EnumDictSpec.Item second = spec.items().get(1);
        Assertions.assertEquals("APP_USER", second.code());
        Assertions.assertEquals(1, second.sort());
    }

    @Test
    void ofMapsArbitraryEnum() {
        EnumDictSpec spec = EnumDictSpec.of("demo", "演示", null,
                InnerEnum.class, InnerEnum::getCode, InnerEnum::caption);

        Assertions.assertNull(spec.description());
        Assertions.assertEquals(2, spec.items().size());
        Assertions.assertEquals("A", spec.items().get(0).value());
        Assertions.assertEquals("甲", spec.items().get(0).label());
        Assertions.assertEquals("B", spec.items().get(1).value());
    }

    enum InnerEnum {
        ONE("A", "甲"),
        TWO("B", "乙"),
        ;

        private final String code;
        private final String caption;

        InnerEnum(String code, String caption) {
            this.code = code;
            this.caption = caption;
        }

        String getCode() {
            return code;
        }

        String caption() {
            return caption;
        }
    }
}
