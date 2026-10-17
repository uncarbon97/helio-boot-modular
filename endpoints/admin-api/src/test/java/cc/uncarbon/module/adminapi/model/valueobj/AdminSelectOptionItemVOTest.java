package cc.uncarbon.module.adminapi.model.valueobj;

import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

/**
 * {@link AdminSelectOptionItemVO} 静态工厂与批量转换分支测试
 */
class AdminSelectOptionItemVOTest {

    @Test
    void ofIdNameMapsFields() {
        var item = AdminSelectOptionItemVO.ofIdName(1L, "一项");

        Assertions.assertEquals(1L, item.getId());
        Assertions.assertEquals("一项", item.getName());
        Assertions.assertNull(item.getParentId());
        Assertions.assertNull(item.getValue());
        Assertions.assertNull(item.getLabel());
    }


    /*
    ----------------------------------------------------------------
                        单项构造
    ----------------------------------------------------------------
     */

    @Test
    void ofIdNameParentMapsParentId() {
        var item = AdminSelectOptionItemVO.ofIdNameParent(2L, "子项", 1L);

        Assertions.assertEquals(2L, item.getId());
        Assertions.assertEquals(1L, item.getParentId());
    }

    @Test
    void ofValueLabelMapsFieldsAndSupportsSetters() {
        var item = AdminSelectOptionItemVO.ofValueLabel(1, "启用");
        item.setDictItemCode("DICT_X");
        item.setStorageCode("MINIO");

        Assertions.assertEquals(1, item.getValue());
        Assertions.assertEquals("启用", item.getLabel());
        Assertions.assertEquals("DICT_X", item.getDictItemCode());
        Assertions.assertEquals("MINIO", item.getStorageCode());
    }

    @Test
    void ofIdNameBatchMapsAllItems() {
        var ret = AdminSelectOptionItemVO.ofIdNameBatch(
                List.of(new Source(1L, "a", null), new Source(2L, "b", 1L)),
                Source::id, Source::name);

        Assertions.assertEquals(2, ret.size());
        Assertions.assertEquals(1L, ret.get(0).getId());
        Assertions.assertEquals("a", ret.get(0).getName());
        // 不带 parentIdGetter 时上级ID为空
        Assertions.assertNull(ret.get(1).getParentId());
    }


    /*
    ----------------------------------------------------------------
                        批量构造
    ----------------------------------------------------------------
     */

    @Test
    void ofIdNameBatchReturnsEmptyForEmptySource() {
        Assertions.assertEquals(List.of(),
                AdminSelectOptionItemVO.ofIdNameBatch(List.of(), Source::id, Source::name));
    }

    @Test
    void ofIdNameBatchAppliesPostConversionProcessing() {
        var ret = AdminSelectOptionItemVO.ofIdNameBatch(
                List.of(new Source(1L, "a", null)),
                Source::id, Source::name,
                (source, target) -> target.setDictItemCode("C" + source.id()));

        Assertions.assertEquals("C1", ret.get(0).getDictItemCode());
    }

    @Test
    void ofIdNameParentBatchAppliesFilterAndParentId() {
        var ret = AdminSelectOptionItemVO.ofIdNameParentBatch(
                List.of(new Source(1L, "a", null), new Source(2L, "b", 1L)),
                Source::id, Source::name, Source::parentId,
                source -> Objects.equals(source.parentId(), 1L),
                (source, target) -> target.setStorageCode("S"));

        // 过滤器仅保留带上级ID的项
        Assertions.assertEquals(1, ret.size());
        Assertions.assertEquals(2L, ret.get(0).getId());
        Assertions.assertEquals(1L, ret.get(0).getParentId());
        Assertions.assertEquals("S", ret.get(0).getStorageCode());
    }

    @Test
    void ofIdNameParentBatchToleratesNullParentIdGetter() {
        var ret = AdminSelectOptionItemVO.ofIdNameParentBatch(
                List.of(new Source(1L, "a", null)),
                Source::id, Source::name, null);

        Assertions.assertEquals(1, ret.size());
        Assertions.assertNull(ret.get(0).getParentId());
    }

    @Test
    void ofIdNameParentBatchReturnsEmptyForEmptySource() {
        Assertions.assertEquals(List.of(),
                AdminSelectOptionItemVO.ofIdNameParentBatch(
                        List.of(), Source::id, Source::name, Source::parentId));
    }

    @Test
    void ofEnumMapsAllConstants() {
        var ret = AdminSelectOptionItemVO.ofEnum(LogResultStatusEnum.class);

        Assertions.assertEquals(2, ret.size());
        Assertions.assertEquals(LogResultStatusEnum.SUCCESS.getValue(), ret.get(0).getValue());
        Assertions.assertEquals("成功", ret.get(0).getLabel());
        Assertions.assertEquals(LogResultStatusEnum.FAILED.getValue(), ret.get(1).getValue());
    }


    /*
    ----------------------------------------------------------------
                        枚举转换
    ----------------------------------------------------------------
     */

    @Test
    void ofEnumAppliesFilter() {
        var ret = AdminSelectOptionItemVO.ofEnum(
                LogResultStatusEnum.class, e -> e == LogResultStatusEnum.SUCCESS);

        Assertions.assertEquals(1, ret.size());
        Assertions.assertEquals("成功", ret.get(0).getLabel());
    }

    @Test
    void ofEnumReturnsEmptyForNullSource() {
        Class<LogResultStatusEnum> nullSource = null;

        Assertions.assertEquals(List.of(), AdminSelectOptionItemVO.ofEnum(nullSource));
        Assertions.assertEquals(List.of(), AdminSelectOptionItemVO.ofEnum(nullSource, e -> true));
    }

    @Test
    void ofValueLabelBatchMapsAllItems() {
        var ret = AdminSelectOptionItemVO.ofValueLabelBatch(
                List.of(new Source(1L, "a", null), new Source(2L, "b", null)),
                Source::id, Source::name);

        Assertions.assertEquals(2, ret.size());
        Assertions.assertEquals(1L, ret.get(0).getValue());
        Assertions.assertEquals("b", ret.get(1).getLabel());
    }


    /*
    ----------------------------------------------------------------
                        值-标签批量转换
    ----------------------------------------------------------------
     */

    @Test
    void ofValueLabelBatchAppliesPostConversionProcessing() {
        var ret = AdminSelectOptionItemVO.ofValueLabelBatch(
                List.of(new Source(1L, "a", null)),
                Source::id, Source::name,
                (source, target) -> target.setDictItemCode("V" + source.name()));

        Assertions.assertEquals("Va", ret.get(0).getDictItemCode());
    }

    /**
     * 转换源实体
     */
    private record Source(Long id, String name, Long parentId) {
    }
}
