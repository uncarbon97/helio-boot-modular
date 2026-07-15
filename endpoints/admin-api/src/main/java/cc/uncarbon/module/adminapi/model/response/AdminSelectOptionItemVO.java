package cc.uncarbon.module.adminapi.model.response;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 系统管理-下拉框数据单项 VO
 * 只有非 null 的字段才会被输出
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
@Getter
public class AdminSelectOptionItemVO implements Serializable {
    /**
     * Jackson等序列化框架，可利用此无参构造器反射生成对象
     */
    private AdminSelectOptionItemVO() {
    }

    // ID👉名称 一对（用于关联各种实体）
    @Schema(description = "ID")
    private Serializable id;
    @Schema(description = "名称")
    private String name;

    // 有时候额外需要上级ID
    @Schema(description = "上级ID")
    @Setter
    private Serializable parentId;

    // 值👉标签 一对（仅用于枚举）
    @Schema(description = "值")
    private Serializable value;
    @Schema(description = "标签")
    private String label;


    /*
    ----------------------------------------------------------------
                        自定义业务字段都写在这里
                        都要标记释义、用处、新增时版本号
                        免得每个人各取一个名，不统一
    ----------------------------------------------------------------
     */

    /**
     * @since 4.0.0
     */
    @Schema(description = "字典项编码")
    @Setter
    private String dictItemCode;


    /*
    ----------------------------------------------------------------
                        构造方法 constructors
    ----------------------------------------------------------------
     */

    public static AdminSelectOptionItemVO ofIdName(Serializable id, String name) {
        var ret = new AdminSelectOptionItemVO();
        ret.id = id;
        ret.name = name;
        return ret;
    }

    public static AdminSelectOptionItemVO ofIdNameParent(Serializable id, String name, Serializable parentId) {
        var ret = ofIdName(id, name);
        ret.parentId = parentId;
        return ret;
    }

    public static AdminSelectOptionItemVO ofValueLabel(Serializable value, String label) {
        var ret = new AdminSelectOptionItemVO();
        ret.value = value;
        ret.label = label;
        return ret;
    }

    /**
     * 构造{@code List<{@link AdminSelectOptionItemVO}>}
     * 将转换源集合中所有集合项
     * 不使用上级ID
     *
     * @param source     源集合
     * @param idGetter   id getter
     * @param nameGetter name getter
     */
    public static <T> List<AdminSelectOptionItemVO> ofIdNameBatch(
            @NonNull Collection<T> source,
            @NonNull Function<T, Serializable> idGetter,
            @NonNull Function<T, String> nameGetter
    ) {
        return ofIdNameBatch(source, idGetter, nameGetter, null);
    }

    /**
     * 构造{@code List<{@link AdminSelectOptionItemVO}>}
     * 将转换源集合中所有集合项
     * 支持上级ID
     *
     * @param source         源集合
     * @param idGetter       id getter
     * @param nameGetter     name getter
     * @param parentIdGetter 上级ID getter
     */
    public static <T> List<AdminSelectOptionItemVO> ofIdNameParentBatch(
            @NonNull Collection<T> source,
            @NonNull Function<T, Serializable> idGetter,
            @NonNull Function<T, String> nameGetter,
            @Nullable Function<T, Serializable> parentIdGetter
    ) {
        return ofIdNameParentBatch(source, idGetter, nameGetter, parentIdGetter, null, null);
    }

    /**
     * 构造{@code List<{@link AdminSelectOptionItemVO}>}
     * 将转换源集合中所有集合项
     * 不使用上级ID
     *
     * @param source                   源集合
     * @param idGetter                 id getter
     * @param nameGetter               name getter
     * @param postConversionProcessing （可选）转换后置处理过程，方便加入一些自定义字段，如 code、quantity 等
     */
    public static <T> List<AdminSelectOptionItemVO> ofIdNameBatch(
            @NonNull Collection<T> source,
            @NonNull Function<T, Serializable> idGetter,
            @NonNull Function<T, String> nameGetter,
            @Nullable BiConsumer<T, AdminSelectOptionItemVO> postConversionProcessing
    ) {
        return ofIdNameParentBatch(source, idGetter, nameGetter, null, null, postConversionProcessing);
    }

    /**
     * 构造{@code List<{@link AdminSelectOptionItemVO}>}
     * 支持自定义过滤器，仅转换需要的集合项
     * 支持上级ID
     *
     * @param source                   源集合
     * @param idGetter                 id getter
     * @param nameGetter               name getter
     * @param parentIdGetter           （可选）parentId getter
     * @param sourceItemFilter         （可选）集合项过滤器
     * @param postConversionProcessing （可选）转换后置处理过程，方便加入一些自定义字段，如 code、quantity 等
     */
    public static <T> List<AdminSelectOptionItemVO> ofIdNameParentBatch(
            @NonNull Collection<T> source,
            @NonNull Function<T, Serializable> idGetter,
            @NonNull Function<T, String> nameGetter,
            @Nullable Function<T, Serializable> parentIdGetter,
            @Nullable Predicate<T> sourceItemFilter,
            @Nullable BiConsumer<T, AdminSelectOptionItemVO> postConversionProcessing
    ) {
        if (CollUtil.isEmpty(source)) {
            return List.of();
        }
        Stream<T> stream = source.stream();
        if (sourceItemFilter != null) {
            stream = stream.filter(sourceItemFilter);
        }

        return stream.map(sourceItem -> {
            AdminSelectOptionItemVO optionItem = ofIdName(idGetter.apply(sourceItem), nameGetter.apply(sourceItem));
            if (Objects.nonNull(parentIdGetter)) {
                optionItem.setParentId(parentIdGetter.apply(sourceItem));
            }
            if (Objects.nonNull(postConversionProcessing)) {
                postConversionProcessing.accept(sourceItem, optionItem);
            }
            return optionItem;
        }).toList();
    }

    /**
     * 构造{@code List<{@link AdminSelectOptionItemVO}>}
     * 将转换枚举类中所有枚举常量
     *
     * @param source 实现了 {@link BaseEnum} 的枚举类
     */
    public static <E extends BaseEnum<? extends Number>> List<AdminSelectOptionItemVO> ofEnum(@NonNull Class<E> source) {
        return ofEnum(source, null);
    }

    /**
     * 构造{@code List<{@link AdminSelectOptionItemVO}>}
     * 支持自定义过滤器，仅转换需要的枚举常量
     *
     * @param source             实现了 {@link BaseEnum} 的枚举类
     * @param enumConstantFilter （可选）枚举类中枚举常量过滤器
     */
    public static <E extends BaseEnum<? extends Serializable>> List<AdminSelectOptionItemVO> ofEnum(
            Class<E> source, @Nullable Predicate<E> enumConstantFilter
    ) {
        if (source == null) {
            return List.of();
        }
        Stream<E> stream = Arrays.stream(source.getEnumConstants());
        if (enumConstantFilter != null) {
            stream = stream.filter(enumConstantFilter);
        }
        return ofValueLabelBatch(stream.toList(), E::getValue, E::getLabel);
    }

    /**
     * 构造{@code List<{@link AdminSelectOptionItemVO}>}
     * 将转换源集合中所有集合项
     *
     * @param source      源集合
     * @param valueGetter id getter
     * @param labelGetter name getter
     */
    public static <T> List<AdminSelectOptionItemVO> ofValueLabelBatch(
            @NonNull Collection<T> source,
            @NonNull Function<T, Serializable> valueGetter,
            @NonNull Function<T, String> labelGetter) {
        return ofValueLabelBatch(source, valueGetter, labelGetter, null);
    }

    /**
     * 构造{@code List<{@link AdminSelectOptionItemVO}>}
     * 将转换源集合中所有集合项
     *
     * @param source      源集合
     * @param valueGetter id getter
     * @param labelGetter name getter
     */
    public static <T> List<AdminSelectOptionItemVO> ofValueLabelBatch(
            @NonNull Collection<T> source,
            @NonNull Function<T, Serializable> valueGetter,
            @NonNull Function<T, String> labelGetter,
            @Nullable BiConsumer<T, AdminSelectOptionItemVO> postConversionProcessing) {
        Stream<T> stream = source.stream();
        return stream.map(item -> {
            AdminSelectOptionItemVO optionItem = ofValueLabel(valueGetter.apply(item), labelGetter.apply(item));
            if (Objects.nonNull(postConversionProcessing)) {
                postConversionProcessing.accept(item, optionItem);
            }
            return optionItem;
        }).toList();
    }
}
