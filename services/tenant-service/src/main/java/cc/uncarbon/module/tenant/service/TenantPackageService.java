package cc.uncarbon.module.tenant.service;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.tenant.model.query.AdminTenantPackageListQuery;
import cc.uncarbon.module.tenant.model.request.AdminTenantPackageBindMenuRequest;
import cc.uncarbon.module.tenant.model.request.AdminTenantPackageUpsertRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantPackageBindMenuResult;
import cc.uncarbon.module.tenant.model.valueobj.TenantPackageDTO;

import java.util.Collection;

/**
 * 租户套餐
 */
public interface TenantPackageService {

    /**
     * 后台管理-分页查询
     */
    PageResult<TenantPackageDTO> adminList(AdminTenantPackageListQuery query);

    /**
     * 后台管理-新增
     */
    Long adminCreate(AdminTenantPackageUpsertRequest request);

    /**
     * 后台管理-修改
     */
    void adminUpdate(AdminTenantPackageUpsertRequest request);

    /**
     * 后台管理-删除
     */
    void adminDelete(Collection<Long> ids);

    /**
     * 后台管理-绑定租户套餐菜单
     */
    TenantPackageBindMenuResult adminBindMenu(AdminTenantPackageBindMenuRequest request);

    /**
     * 根据 ID 取详情
     */
    TenantPackageDTO getById(Long id);

    /**
     * 根据 ID 取详情，未取到会抛出 {@link NoRecordException}
     */
    TenantPackageDTO getNonnullById(Long id) throws NoRecordException;

}
