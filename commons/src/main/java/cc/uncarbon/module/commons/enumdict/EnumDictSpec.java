package cc.uncarbon.module.commons.enumdict;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * 枚举字典规范
 *
 * @author Uncarbon
 */
public record EnumDictSpec(
        @Schema(description = "字典编码")
        String code,
        @Schema(description = "字典名称")
        String name,
        @Schema(description = "字典描述")
        String description,
        @Schema(description = "字典项集合")
        List<Item> items
) {
    public record Item(
            @Schema(description = "字典项编码")
            String code,
            @Schema(description = "字典项值")
            String value,
            @Schema(description = "字典项标签")
            String label,
            @Schema(description = "排序")
            int sort
    ) {
    }

    /**
     * 通用构造：从任意枚举类的常量中，按传入的 lambda 整理为内置字典
     *
     * @param enumClass   枚举类
     * @param valueMapper 取值，内部转 String
     * @param labelMapper 取标签
     */
    public static <E extends Enum<E>> EnumDictSpec of(
            String code,
            String name,
            String description,
            Class<E> enumClass,
            Function<E, ?> valueMapper,
            Function<E, String> labelMapper) {

        List<Item> items = Arrays.stream(enumClass.getEnumConstants())
                .map(e -> new Item(
                        e.name(),
                        labelMapper.apply(e),
                        String.valueOf(valueMapper.apply(e)),
                        e.ordinal()))
                .toList();
        return new EnumDictSpec(code, name, description, items);
    }
}
