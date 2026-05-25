package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.core.exception.BusinessException;
import cc.uncarbon.framework.core.page.PageParam;
import cc.uncarbon.framework.core.page.PageResult;
import cc.uncarbon.module.sys.model.request.AdminInsertSysLogDTO;
import cc.uncarbon.module.sys.model.request.AdminListSysLogDTO;
import cc.uncarbon.module.sys.model.response.SysLogBO;

/**
 * 系统日志
 */
public interface SysLogService {

    /**
     * 系统管理-分页列表
     */
    PageResult<SysLogBO> adminList(AdminListSysLogDTO dto);

    /**
     * 系统管理-新增
     */
    Long adminInsert(AdminInsertSysLogDTO dto);

    /**
     * 根据 ID 取详情
     */
    SysLogBO getOneById(Long id);

    /**
     * 根据 ID 取详情
     */
    SysLogBO getOneById(Long id, boolean throwIfInvalidId) throws BusinessException;
}
