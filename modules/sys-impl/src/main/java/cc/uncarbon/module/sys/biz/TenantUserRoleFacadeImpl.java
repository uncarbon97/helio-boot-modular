package cc.uncarbon.module.sys.biz;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.model.request.TenantRoleBindMenuRequest;
import cc.uncarbon.module.sys.model.request.TenantRoleCreateRequest;
import cc.uncarbon.module.sys.model.request.TenantUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.TenantUserCreateRequest;
import cc.uncarbon.module.sys.model.response.TenantRoleCreateResult;
import cc.uncarbon.module.sys.model.response.TenantUserBasicProfile;
import cc.uncarbon.module.sys.model.response.TenantUserCreateResult;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cc.uncarbon.module.sys.service.SysRoleService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cc.uncarbon.module.sys.service.SysUserService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final SysRoleMenuRelationService sysRoleMenuRelationService;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;


    @SneakyThrows
    @Override
    public TenantRoleCreateResult createTenantRole(TenantRoleCreateRequest request) {
        return TenantContextHolder.callWithContext(
                new SimpleTenantContext(request.getTenantId(), request.getTenantCode(), request.getTenantCode()),
                () -> sysRoleService.createTenantRole(request)
        );
    }

    @SneakyThrows
    @Override
    public TenantUserCreateResult createTenantUser(TenantUserCreateRequest request) {
        return TenantContextHolder.callWithContext(
                new SimpleTenantContext(request.getTenantId(), request.getTenantCode(), request.getTenantCode()),
                () -> sysUserService.createTenantUser(request)
        );
    }

    @Override
    public void bindTenantUserRoleRelation(TenantUserBindRoleRequest request) {
        TenantContextHolder.runWithContext(
                new SimpleTenantContext(request.getTenantId(), request.getTenantCode(), null),
                () -> sysUserRoleRelationService.cleanAndBindByUser(request.getUserId(), request.getRoleIds()));
    }

    @Override
    public void bindTenantRoleMenuRelation(TenantRoleBindMenuRequest request) {
        TenantContextHolder.runWithContext(
                new SimpleTenantContext(request.getTenantId(), request.getTenantCode(), null),
                () -> sysRoleMenuRelationService.cleanAndBind(request.getRoleId(), request.getMenuIds()));
    }

    @SneakyThrows
    @Override
    public Set<Long> syncTenantRoleMenus(long tenantId, Collection<Long> packageMenuIds) {
        return TenantContextHolder.callWithContext(
                new SimpleTenantContext(tenantId, null, null),
                () -> {
                    List<SysRoleEntity> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                            .select(SysRoleEntity::getId, SysRoleEntity::getCode)
                    );
                    Set<Long> allowedMenuIds = CollUtil.isEmpty(packageMenuIds) ? Set.of() : Set.copyOf(packageMenuIds);
                    for (SysRoleEntity role : roles) {
                        if (role.isTenantAdmin()) {
                            sysRoleMenuRelationService.cleanAndBind(role.getId(), allowedMenuIds);
                        } else {
                            Set<Long> retainedMenuIds = new HashSet<>(
                                    sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(role.getId()))
                            );
                            retainedMenuIds.retainAll(allowedMenuIds);
                            sysRoleMenuRelationService.cleanAndBind(role.getId(), retainedMenuIds);
                        }
                    }
                    return roles.stream().map(SysRoleEntity::getId).collect(Collectors.toSet());
                });
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

    @SneakyThrows
    @Override
    public List<Long> listUserIdsByTenantId(Long tenantId, Collection<EnabledStatusEnum> statusEnums) {
        return TenantContextHolder.callWithContext(
                new SimpleTenantContext(tenantId, null, null),
                () -> sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                                .select(SysUserEntity::getId)
                                // 不列举出超级管理员
                                .ne(SysUserEntity::getId, SysConstant.SUPER_ADMIN_USER_ID)
                                .in(CollUtil.isNotEmpty(statusEnums), SysUserEntity::getStatus, statusEnums)
                        )
                        .stream()
                        .map(SysUserEntity::getId)
                        .toList());
    }
}
