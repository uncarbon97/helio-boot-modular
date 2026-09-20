package cc.uncarbon.module.tenant.service;


import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.tenant.model.query.AdminTenantMetaListQuery;
import cc.uncarbon.module.tenant.model.request.AdminTenantCreateRequest;
import cc.uncarbon.module.tenant.model.request.AdminTenantMetaUpdateRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaDTO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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
     *
     * @return 套餐发生变化时受影响的角色ID集合，否则为空集合；用于调用方按需刷新权限缓存
     */
    Set<Long> adminUpdate(AdminTenantMetaUpdateRequest request);

    /**
     * 后台管理-修改状态
     *
     * @return 禁用时返回该租户全部用户ID，启用时为空集合；用于调用方按需强制登出
     */
    List<Long> adminSetStatus(AdminSetStatusRequest<Long, EnabledStatusEnum> request);

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
     * 根据租户编码取详情
     * @param fillDetail 是否填充详情
     */
    TenantMetaDTO getByCode(String code, boolean fillDetail);

    /**
     * 列举启用状态的租户（不填充详情）
     */
    List<TenantMetaDTO> listEnabled();

}
