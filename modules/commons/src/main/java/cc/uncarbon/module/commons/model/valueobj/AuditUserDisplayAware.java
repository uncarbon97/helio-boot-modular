package cc.uncarbon.module.commons.model.valueobj;

/**
 * 可填充「创建/更新操作人昵称」的值对象契约
 * <p>
 * createdBy/updatedBy 字段类型为 String，直接承载操作人昵称；
 * 操作人用户ID 不经过值对象，由 {@code AuditNicknameFiller} 从数据源（entity）侧批量解析回填
 *
 * @author Uncarbon
 */
public interface AuditUserDisplayAware {

    AuditUserDisplayAware setCreatedBy(String nickname);

    AuditUserDisplayAware setUpdatedBy(String nickname);
}
