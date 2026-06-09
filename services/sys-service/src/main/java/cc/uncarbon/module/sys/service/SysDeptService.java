package cc.uncarbon.module.sys.service;

import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.model.internal.UserDeptScope;
import cc.uncarbon.module.sys.model.request.AdminSysDeptUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysDeptDTO;

import java.util.Collection;
import java.util.List;

/**
 * 部门
 */
public interface SysDeptService {

    /**
     * 后台管理-列表
     */
    List<SysDeptDTO> adminList();

    /**
     * 后台管理-新增
     */
    Long adminCreate(AdminSysDeptUpsertRequest request);

    /**
     * 后台管理-修改
     */
    void adminUpdate(AdminSysDeptUpsertRequest request);

    /**
     * 后台管理-删除
     */
    void adminDelete(Collection<Long> ids);

    /**
     * 后台管理-下拉框数据
     *
     * @param inferiorsOnly 只能看到本部门及以下
     */
    List<SysDeptDTO> adminSelectOptions(boolean inferiorsOnly);

    /**
     * 根据 ID 取详情
     */
    SysDeptDTO getById(Long id);

    /**
     * 根据 ID 取详情，未取到会抛出 {@link NoRecordException}
     */
    SysDeptDTO getNonnullById(Long id) throws NoRecordException;

    /**
     * 取当前用户关联部门信息
     * 仅内部使用
     *
     * @param queryVisibleDept 是否要进一步查询可见部门
     */
    UserDeptScope getCurrentUserDept(boolean queryVisibleDept);

    /**
     * 取指定用户关联部门信息
     * 仅内部使用
     *
     * @param queryVisibleDept 是否要进一步查询可见部门
     */
    UserDeptScope getSpecifiedUserDept(Long specifiedUserId, boolean queryVisibleDept);

}
