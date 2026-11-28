package cc.uncarbon.module.commons.model.valueobj;

/**
 * 审计操作人用户ID 数据源契约（entity 侧）
 * <p>
 * 通用实体基类已提供 {@code getCreatedBy()/getUpdatedBy()}，实体只需 {@code implements} 本接口即可
 *
 * @author Uncarbon
 */
public interface AuditingUserSource {

    Long getCreatedBy();

    Long getUpdatedBy();
}
