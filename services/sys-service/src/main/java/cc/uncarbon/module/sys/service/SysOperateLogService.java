package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.model.query.AdminSysOperateLogListQuery;
import cc.uncarbon.module.sys.model.request.SysOperateLogCreateRequest;
import cc.uncarbon.module.sys.model.valueobj.SysOperateLogDTO;

/**
 * 系统操作日志
 */
public interface SysOperateLogService {

    /**
     * 后台管理-分页查询
     */
    PageResult<SysOperateLogDTO> adminList(AdminSysOperateLogListQuery query);

    /**
     * 根据 ID 取详情
     */
    SysOperateLogDTO getById(Long id);

    /**
     * 根据 ID 取详情，未取到会抛出 {@link NoRecordException}
     */
    SysOperateLogDTO getNonnullById(Long id) throws NoRecordException;

    /**
     * 新增
     */
    Long create(SysOperateLogCreateRequest request);

}
