package cc.uncarbon.module.sys.service;

import cc.uncarbon.module.sys.model.request.SysOperateLogCreateRequest;

/**
 * 系统操作日志
 */
public interface SysOperateLogService {

    /**
     * 新增
     */
    Long create(SysOperateLogCreateRequest request);

}
