package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.sys.model.interior.UserDeptContainer;
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
     *
     * @param id 主键ID
     * @return null or 详情
     */
    SysDeptDTO getById(Long id);

    /**
     * 根据 ID 取详情
     *
     * @param id              主键ID
     * @param throwIfNotFound 未找到时是否抛出异常
     * @return null or 详情
     */
    SysDeptDTO getById(Long id, boolean throwIfNotFound) throws BusinessException;

    /**
     * 取当前用户关联部门信息
     * 仅内部使用
     *
     * @param queryVisibleDept 是否要进一步查询可见部门
     */
    UserDeptContainer getCurrentUserDeptContainer(boolean queryVisibleDept);

    /**
     * 取指定用户关联部门信息
     * 仅内部使用
     *
     * @param queryVisibleDept 是否要进一步查询可见部门
     */
    UserDeptContainer getSpecifiedUserDeptContainer(Long specifiedUserId, boolean queryVisibleDept);

}
