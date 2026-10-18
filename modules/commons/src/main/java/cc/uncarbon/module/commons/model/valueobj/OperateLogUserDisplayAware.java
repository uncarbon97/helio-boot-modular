package cc.uncarbon.module.commons.model.valueobj;

/**
 * 可填充「操作人昵称」的值对象契约（操作/登录日志类，操作人字段前缀为 userId）
 * <p>
 * setter 返回自身类型，与 {@code @Accessors(chain = true)} 生成的链式 setter 协变兼容
 *
 * @author Uncarbon
 */
public interface OperateLogUserDisplayAware {

    OperateLogUserDisplayAware setUserNickname(String nickname);
}
