package cc.uncarbon.module.sys.iplocation;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.module.commons.enumdict.EnumDictSpec;
import cc.uncarbon.module.sys.support.DictContributor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link DictContributor} 内置框架枚举字典贡献测试
 */
class DictContributorTest {

    private final DictContributor contributor = new DictContributor();

    @Test
    void contributeRegistersThreeBuiltinDicts() {
        Map<String, EnumDictSpec> specs = contributor.contribute().stream()
                .collect(Collectors.toMap(EnumDictSpec::code, s -> s));

        Assertions.assertEquals(3, specs.size());
        Assertions.assertTrue(specs.containsKey("enabled_status"));
        Assertions.assertTrue(specs.containsKey("gender"));
        Assertions.assertTrue(specs.containsKey("yes_or_no"));
    }

    @Test
    void enabledStatusItemsFollowEnumOrder() {
        EnumDictSpec spec = findSpec("enabled_status", "启用状态枚举");

        // 字典项按枚举常量顺序展开：禁用在前、启用在后
        Assertions.assertEquals(2, spec.items().size());
        EnumDictSpec.Item disabled = spec.items().get(0);
        Assertions.assertEquals("DISABLED", disabled.code());
        Assertions.assertEquals("0", disabled.value());
        Assertions.assertEquals(EnabledStatusEnum.DISABLED.getLabel(), disabled.label());
        Assertions.assertEquals(0, disabled.sort());

        EnumDictSpec.Item enabled = spec.items().get(1);
        Assertions.assertEquals("ENABLED", enabled.code());
        Assertions.assertEquals("1", enabled.value());
        Assertions.assertEquals(EnabledStatusEnum.ENABLED.getLabel(), enabled.label());
        Assertions.assertEquals(1, enabled.sort());
    }

    @Test
    void genderItemsCoverAllConstants() {
        EnumDictSpec spec = findSpec("gender", "生理性别枚举");

        Assertions.assertEquals(3, spec.items().size());
        List<String> codes = spec.items().stream().map(EnumDictSpec.Item::code).toList();
        Assertions.assertEquals(List.of("UNKNOWN", "MALE", "FEMALE"), codes);
    }

    @Test
    void yesOrNoItemsCoverAllConstants() {
        EnumDictSpec spec = findSpec("yes_or_no", "是或否枚举");

        Assertions.assertEquals(2, spec.items().size());
        List<String> values = spec.items().stream().map(EnumDictSpec.Item::value).toList();
        Assertions.assertEquals(List.of("0", "1"), values);
        Assertions.assertEquals(YesOrNoEnum.YES.getLabel(), spec.items().get(1).label());
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    private EnumDictSpec findSpec(String code, String expectedName) {
        List<EnumDictSpec> ret = List.copyOf(contributor.contribute());
        return ret.stream()
                .filter(s -> code.equals(s.code()))
                .peek(s -> Assertions.assertEquals(expectedName, s.name()))
                .findFirst()
                .orElseThrow();
    }
}
