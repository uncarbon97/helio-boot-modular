package cc.uncarbon.module.sys.biz;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.model.request.TenantRoleCreateRequest;
import cc.uncarbon.module.sys.model.request.TenantUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.TenantUserCreateRequest;
import cc.uncarbon.module.sys.model.response.TenantRoleCreateResult;
import cc.uncarbon.module.sys.model.response.TenantUserBasicProfile;
import cc.uncarbon.module.sys.model.response.TenantUserCreateResult;
import cc.uncarbon.module.sys.service.SysRoleService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cc.uncarbon.module.sys.service.SysUserService;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
    public TenantRoleCreateResult createTenantRole(TenantRoleCreateRequest request) {
        return sysRoleService.createTenantRole(request);
    }

    @Override
    public TenantUserCreateResult createTenantUser(TenantUserCreateRequest request) {
        return sysUserService.createTenantUser(request);
    }

    @Override
    public void bindTenantUserRoleRelation(TenantUserBindRoleRequest request) {
        sysUserRoleRelationService.tenantUserBindRole(request);
    }

    @SneakyThrows
    @Override
    public TenantUserBasicProfile getTenantUserBasicProfile(long tenantId, long userId) {
        return TenantContextHolder.callWithContext(
                new SimpleTenantContext(tenantId, null, null),
                () -> {
                    var user = sysUserService.getNonnullById(userId);
                    var ret = new TenantUserBasicProfile();
                    BeanUtil.copyProperties(user, ret);
                    return ret;
                });
    }

    @Override
    public List<Long> listUserIdsByTenantId(Long tenantId, Collection<EnabledStatusEnum> statusEnums) {
        return List.of();
    }
}
