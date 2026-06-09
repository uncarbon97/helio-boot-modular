package cc.uncarbon.module.sys.biz;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.model.request.AppendTenantRoleRequest;
import cc.uncarbon.module.sys.model.request.AppendTenantUserRequest;
import cc.uncarbon.module.sys.model.request.BindTenantUserRoleRelationRequest;
import cc.uncarbon.module.sys.model.response.AppendTenantRoleResult;
import cc.uncarbon.module.sys.model.response.AppendTenantUserResult;
import cc.uncarbon.module.sys.service.SysRoleService;
import cc.uncarbon.module.sys.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 租户用户、角色门面
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TenantUserRoleFacadeImpl implements TenantUserRoleFacade {

    private final SysRoleService sysRoleService;
    private final SysUserService sysUserService;


    @Override
    public AppendTenantRoleResult appendTenantRole(AppendTenantRoleRequest request) {
        return sysRoleService.appendTenantRole(request);
    }

    @Override
    public AppendTenantUserResult appendTenantUser(AppendTenantUserRequest request) {
        return sysUserService.appendTenantUser(request);
    }

    @Override
    public void bindTenantUserRoleRelation(BindTenantUserRoleRelationRequest request) {

    }

    @Override
    public List<Long> listUserIdsByTenantId(Long tenantId, Collection<EnabledStatusEnum> statusEnums) {
        return List.of();
    }
}
