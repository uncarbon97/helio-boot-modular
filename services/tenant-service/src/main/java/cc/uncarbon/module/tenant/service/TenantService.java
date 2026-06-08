package cc.uncarbon.module.tenant.service;


import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.tenant.model.query.AdminTenantMetaListQuery;
import cc.uncarbon.module.tenant.model.request.AdminTenantCreateRequest;
import cc.uncarbon.module.tenant.model.request.AdminTenantMetaUpdateRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaDTO;

import java.util.Collection;
import java.util.List;

/**
 * 租户
 */
public interface TenantService {

    /**
     * 后台管理-分页查询
     */
    PageResult<TenantMetaDTO> adminList(AdminTenantMetaListQuery query);

    /**
     * 后台管理-新增
     */
    Long adminCreate(AdminTenantCreateRequest request);

    /**
     * 后台管理-修改
     */
    void adminUpdate(AdminTenantMetaUpdateRequest request);

    /**
     * 后台管理-删除
     */
    void adminDelete(Collection<Long> ids);

    /**
     * 根据 ID 取详情
     */
    TenantMetaDTO getById(Long id);

    /**
     * 根据 ID 取详情，未取到会抛出 {@link NoRecordException}
     */
    TenantMetaDTO getNonnullById(Long id) throws NoRecordException;

    /**
     * 根据主键IDs，取租户BOs
     */
    List<TenantMetaDTO> listByIds(Collection<Long> ids, boolean fillTenantAdminUser);

}
