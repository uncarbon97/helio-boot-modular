package cc.uncarbon.module.sys.biz;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.facade.TenantSysBridgeFacade;
import cc.uncarbon.module.sys.model.request.AppendTenantRoleRequest;
import cc.uncarbon.module.sys.model.request.AppendTenantUserRequest;
import cc.uncarbon.module.sys.model.request.BindTenantUserRoleRelationRequest;
import cc.uncarbon.module.sys.model.response.AppendTenantRoleResult;
import cc.uncarbon.module.sys.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 租户-系统管理桥接门面
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TenantSysBridgeFacadeImpl implements TenantSysBridgeFacade {

    private final SysRoleService sysRoleService;


    @Override
    public AppendTenantRoleResult appendTenantRole(AppendTenantRoleRequest request) {
        return null;
    }

    @Override
    public void appendTenantUser(AppendTenantUserRequest request) {

    }

    @Override
    public void bindTenantUserRoleRelation(BindTenantUserRoleRelationRequest request) {

    }

    @Override
    public List<Long> listUserIdsByTenantId(Long tenantId, Collection<EnabledStatusEnum> statusEnums) {
        return List.of();
    }
}
