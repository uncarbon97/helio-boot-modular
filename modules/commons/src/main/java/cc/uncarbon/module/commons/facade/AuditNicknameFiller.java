package cc.uncarbon.module.commons.facade;

import cc.uncarbon.module.commons.model.valueobj.AuditUserDisplayAware;
import cc.uncarbon.module.commons.model.valueobj.AuditingUserSource;
import cc.uncarbon.module.commons.model.valueobj.OperateLogUserDisplayAware;
import cc.uncarbon.module.commons.model.valueobj.OperateLogUserSource;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * 审计操作人昵称批量填充器
 * <p>
 * 以数据源（entity）携带的操作人用户ID 为准，一次 in 查询批量解析昵称；
 * 值对象只承载昵称，不暴露用户ID；
 * 用户已删除（含逻辑删除）时填充国际化文案「已删除用户({ID})」，ID 为 null 时保持 null
 *
 * @author Uncarbon
 */
public interface AuditNicknameFiller {

    /**
     * 按数据源顺序转换值对象，并批量填充 createdBy / updatedBy；空集合直接返回空列表
     */
    <E extends AuditingUserSource, T extends AuditUserDisplayAware> List<T> fill(
            Collection<E> sources, Function<E, T> converter);

    /**
     * 按数据源顺序转换值对象，并批量填充 userNickname（按 userId）；空集合直接返回空集合
     */
    <E extends OperateLogUserSource, T extends OperateLogUserDisplayAware> List<T> fillByUserId(
            Collection<E> sources, Function<E, T> converter);

    /**
     * 单条转换并填充 createdBy / updatedBy；source 为 null 时返回 null
     */
    default <E extends AuditingUserSource, T extends AuditUserDisplayAware> T fillOne(
            E source, Function<E, T> converter) {
        return source == null ? null : fill(List.of(source), converter).getFirst();
    }

    /**
     * 单条转换并填充 userNickname（按 userId）；source 为 null 时返回 null
     */
    default <E extends OperateLogUserSource, T extends OperateLogUserDisplayAware> T fillByUserIdOne(
            E source, Function<E, T> converter) {
        return source == null ? null : fillByUserId(List.of(source), converter).getFirst();
    }
}
