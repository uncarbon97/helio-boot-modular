package cc.uncarbon.module.sys.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 后台用户-部门关联
 */
public interface SysUserDeptRelationService {

    /**
     * 列举用户ID关联的部门IDs
     */
    List<Long> getUserDeptIds(Long userId);

    /**
     * 先清理用户ID所有关联关系, 再绑定用户ID与部门ID
     */
    void cleanAndBind(Long userId, Long deptId);

    /**
     * 列举部门IDs关联的用户IDs
     */
    Set<Long> listUserIdsByDeptIds(Collection<Long> deptIds);
}
