package cc.uncarbon.module.commons.enumdict;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记实现了 {@link cc.uncarbon.framework.helium.base.enums.BaseEnum} 的枚举类，为字典枚举类
 *
 * @author Uncarbon
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumDict {

    /**
     * 是否启用，仅 {@code true} 才会加入内置字典缓存
     */
    boolean enabled() default true;

    /**
     * 字典分类编码
     * 如果为空，则自动从枚举类名转换，如：{@code UserStatusEnum} -> {@code user_status}
     */
    String code() default "";

    /**
     * 字典分类外显名称
     */
    String name();

    /**
     * 字典分类描述
     */
    String description() default "";

}
