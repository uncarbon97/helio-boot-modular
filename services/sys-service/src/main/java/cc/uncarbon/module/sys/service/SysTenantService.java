package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.core.exception.BusinessException;
import cc.uncarbon.framework.core.page.PageParam;
import cc.uncarbon.framework.core.page.PageResult;
import cc.uncarbon.module.sys.entity.SysTenantEntity;
import cc.uncarbon.module.sys.model.request.AdminInsertSysTenantDTO;
import cc.uncarbon.module.sys.model.request.AdminListSysTenantDTO;
import cc.uncarbon.module.sys.model.request.AdminUpdateSysTenantDTO;
import cc.uncarbon.module.sys.model.response.SysTenantBO;

import java.util.Collection;
import java.util.List;

/**
 * 系统租户
 */
public interface SysTenantService {

    /**
     * 系统管理-分页列表
     */
    PageResult<SysTenantBO> adminList(AdminListSysTenantDTO dto);

    /**
     * 系统管理-新增
     */
    SysTenantEntity adminInsert(AdminInsertSysTenantDTO dto);

    /**
     * 系统管理-修改
     */
    void adminUpdate(AdminUpdateSysTenantDTO dto);

    /**
     * 系统管理-修改
     */
    void adminUpdate(SysTenantEntity entity);

    /**
     * 系统管理-删除
     */
    void adminDelete(Collection<Long> ids);

    /**
     * 根据 ID 取详情
     */
    SysTenantBO getOneById(Long id);

    /**
     * 根据 ID 取详情
     */
    SysTenantBO getOneById(Long id, boolean throwIfInvalidId) throws BusinessException;

    /**
     * 根据租户ID(非主键ID)，得到租户实体
     */
    SysTenantEntity getTenantEntityByTenantId(Long tenantId);

    /**
     * 检查是否存在重复
     */
    void checkRepeat(AdminInsertSysTenantDTO dto);

    /**
     * 根据主键IDs，取租户BOs
     */
    List<SysTenantBO> listByIds(Collection<Long> ids, boolean fillTenantAdminUser);
}
