package cc.uncarbon.module.sys.biz;

import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserTenantRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserTenantRelationMapper;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.model.request.TenantRoleBindMenuRequest;
import cc.uncarbon.module.sys.model.request.TenantRoleCreateRequest;
import cc.uncarbon.module.sys.model.request.TenantUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.TenantUserCreateRequest;
import cc.uncarbon.module.sys.model.response.TenantRoleCreateResult;
import cc.uncarbon.module.sys.model.response.TenantUserCreateResult;
import cc.uncarbon.module.sys.model.valueobj.TenantUserBasicProfileDTO;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cc.uncarbon.module.sys.service.SysRoleService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cc.uncarbon.module.sys.service.SysUserService;
import cc.uncarbon.module.sys.util.SysUtil;
import cc.uncarbon.module.tenant.facade.TenantConfigFacade;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户用户、角色门面
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class TenantUserRoleFacadeImpl implements TenantUserRoleFacade {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserTenantRelationMapper sysUserTenantRelationMapper;
    private final SysRoleService sysRoleService;
    private final SysUserService sysUserService;
    private final SysUserRoleRelationService sysUserRoleRelationService;
    private final SysRoleMenuRelationService sysRoleMenuRelationService;
    private final TenantConfigFacade tenantConfigFacade;


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
        TenantUserCreateResult ret = TenantContextHolder.callWithContext(
                new SimpleTenantContext(request.getTenantId(), request.getTenantCode(), request.getTenantCode()),
                () -> sysUserService.createTenantUser(request)
        );
        // 双写：sys_user_tenant_relation 为唯一真源，sys_user.tenant_id 投影列由 INSERT 填充维护
        sysUserTenantRelationMapper.insert(SysUserTenantRelationEntity.of(
                request.getTenantId(), ret.getNewUserId()));
        return ret;
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
    public TenantUserBasicProfileDTO getTenantUserBasicProfile(long tenantId, long userId) {
        return TenantContextHolder.callWithContext(
                new SimpleTenantContext(tenantId, null, null),
                () -> {
                    var user = sysUserService.getNonnullBasicProfileById(userId);
                    var ret = new TenantUserBasicProfileDTO();
                    BeanUtil.copyProperties(user, ret);
                    return ret;
                });
    }

    @SneakyThrows
    @Override
    public List<Long> listUserIdsByTenant(long tenantId, Collection<EnabledStatusEnum> statusEnums) {
        boolean userFirst = TenantLoginModeEnum.USER_FIRST == tenantConfigFacade.getLoginMode();
        return TenantContextHolder.callWithContext(
                new SimpleTenantContext(tenantId, null, null),
                () -> {
                    List<Long> memberUserIds = userFirst
                            // USER_FIRST：成员真源在 sys_user_tenant_relation，含投影列指向他租户的拉入成员
                            ? listRelationMemberUserIds(tenantId, statusEnums)
                            // TENANT_FIRST：成员即 sys_user.tenant_id 投影列，行级过滤天然限定
                            : listProjectedUserIds(statusEnums);
                    return memberUserIds.stream()
                            .filter(id -> !SysConstant.SUPER_ADMIN_USER_ID.equals(id))
                            .toList();
                });
    }

    @SneakyThrows
    @Override
    public List<Long> listEnabledTenantIdsByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return sysUserTenantRelationMapper.selectList(new LambdaQueryWrapper<SysUserTenantRelationEntity>()
                        .select(SysUserTenantRelationEntity::getTenantId)
                        .eq(SysUserTenantRelationEntity::getUserId, userId)
                        .orderByAsc(SysUserTenantRelationEntity::getId))
                .stream()
                .map(SysUserTenantRelationEntity::getTenantId)
                .toList();
    }

    @Override
    public void rememberActiveTenant(Long userId, Long tenantId) {
        if (userId == null || tenantId == null || SysUtil.isSuperAdmin(userId)) {
            return;
        }
        TenantContextHolder.runIgnored(() -> sysUserMapper.updateTenantId(userId, tenantId));
    }

    @SneakyThrows
    @Nullable
    @Override
    public Long getUserHomeTenantId(Long userId) {
        if (userId == null) {
            return null;
        }
        // 忽略租户态，强行读取投影列
        return TenantContextHolder.callIgnored(() -> {
            var entity = sysUserMapper.selectById(userId);
            if (entity != null) {
                return entity.getTenantId();
            }
            return null;
        });
    }

    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */


    private List<Long> listProjectedUserIds(Collection<EnabledStatusEnum> statusEnums) {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                        .select(SysUserEntity::getId)
                        .in(CollUtil.isNotEmpty(statusEnums), SysUserEntity::getStatus, statusEnums))
                .stream()
                .map(SysUserEntity::getId)
                .toList();
    }

    /**
     * USER_FIRST 成员集合：先取关联表成员ID，再忽略行级隔离回查 sys_user 过滤状态（成员投影列可能指向他租户）
     */
    @SneakyThrows
    private List<Long> listRelationMemberUserIds(long tenantId, Collection<EnabledStatusEnum> statusEnums) {
        Set<Long> memberUserIds = sysUserTenantRelationMapper.listUserIdsByTenantId(tenantId);
        if (CollUtil.isEmpty(memberUserIds) || CollUtil.isEmpty(statusEnums)) {
            return List.copyOf(CollUtil.emptyIfNull(memberUserIds));
        }
        return TenantContextHolder.callIgnored(() -> sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUserEntity>()
                                .select(SysUserEntity::getId)
                                .in(SysUserEntity::getId, memberUserIds)
                                .in(SysUserEntity::getStatus, statusEnums)
                                .orderByAsc(SysUserEntity::getId))
                .stream()
                .map(SysUserEntity::getId)
                .toList());
    }
}
