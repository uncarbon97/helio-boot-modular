package cc.uncarbon.module.sys.biz;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.model.request.CreateTenantRoleRequest;
import cc.uncarbon.module.sys.model.request.CreateTenantUserRequest;
import cc.uncarbon.module.sys.model.request.BindTenantUserRoleRelationRequest;
import cc.uncarbon.module.sys.model.response.CreateTenantRoleResult;
import cc.uncarbon.module.sys.model.response.CreateTenantUserResult;
import cc.uncarbon.module.sys.model.response.TenantUserBasicProfile;
import cc.uncarbon.module.sys.service.SysRoleService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
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
    private final SysUserRoleRelationService sysUserRoleRelationService;


    @Override
    public CreateTenantRoleResult createTenantRole(CreateTenantRoleRequest request) {
        return sysRoleService.createTenantRole(request);
    }

    @Override
    public CreateTenantUserResult createTenantUser(CreateTenantUserRequest request) {
        return sysUserService.createTenantUser(request);
    }

    @Override
    public void bindTenantUserRoleRelation(BindTenantUserRoleRelationRequest request) {
        sysUserRoleRelationService.bindTenantUserRoleRelation(request);
    }

    @Override
    public TenantUserBasicProfile getTenantUserBasicProfile(long tenantId, long userId) {
        return null;
    }

    @Override
    public List<Long> listUserIdsByTenantId(Long tenantId, Collection<EnabledStatusEnum> statusEnums) {
        return List.of();
    }
}
