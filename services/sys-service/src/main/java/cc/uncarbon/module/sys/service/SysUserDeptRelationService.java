package cc.uncarbon.module.sys.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 系统用户-部门关联关系
 */
public interface SysUserDeptRelationService {

    /**
     * 根据用户ID，查询关联的部门IDs
     * 目前只有单成员
     */
    List<Long> listDeptIdsByUser(Long userId);

    /**
     * 根据部门IDs，查询关联的用户ID
     */
    Set<Long> listUserIdsByDepts(Collection<Long> deptIds);

    /**
     * 绑定用户与部门关联关系
     */
    void cleanAndBind(Long userId, Long deptId);

}
