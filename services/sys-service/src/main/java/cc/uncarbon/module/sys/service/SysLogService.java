package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.sys.model.query.AdminSysLogListQuery;
import cc.uncarbon.module.sys.model.request.AdminInsertSysLogDTO;
import cc.uncarbon.module.sys.model.valueobj.SysLogBO;

/**
 * 系统日志
 */
public interface SysLogService {

    /**
     * 系统管理-分页查询
     */
    PageResult<SysLogBO> adminList(AdminSysLogListQuery query);

    /**
     * 系统管理-新增
     */
    Long adminCreate(AdminInsertSysLogDTO dto);

    /**
     * 根据 ID 取详情
     */
    SysLogBO getOneById(Long id);

    /**
     * 根据 ID 取详情
     */
    SysLogBO getOneById(Long id, boolean throwIfInvalidId) throws BusinessException;
}
