package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.model.query.AdminSysLoginLogListQuery;
import cc.uncarbon.module.sys.model.request.CreateSysLoginLogRequest;
import cc.uncarbon.module.sys.model.valueobj.SysLoginLogDTO;

/**
 * 系统登录日志
 */
public interface SysLoginLogService {

    /**
     * 后台管理-分页查询
     */
    PageResult<SysLoginLogDTO> adminList(AdminSysLoginLogListQuery query);

    /**
     * 新增
     */
    Long create(CreateSysLoginLogRequest request);

    /**
     * 根据 ID 取详情
     */
    SysLoginLogDTO getById(Long id);

    /**
     * 根据 ID 取详情，未取到会抛出 {@link NoRecordException}
     */
    SysLoginLogDTO getNonnullById(Long id) throws NoRecordException;

}
