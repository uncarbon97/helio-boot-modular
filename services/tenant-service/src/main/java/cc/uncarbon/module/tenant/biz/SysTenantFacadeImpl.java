package cc.uncarbon.module.tenant.biz;

import cc.uncarbon.framework.core.constant.HeliumConstant;
import cc.uncarbon.framework.core.enums.EnabledStatusEnum;
import cc.uncarbon.framework.core.function.StreamFunction;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.entity.TenantMetaEntity;
import cc.uncarbon.module.sys.enums.SysErrorEnum;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.tenant.facade.SysTenantFacade;
import cc.uncarbon.module.sys.model.request.AdminSysRoleUpsertRequest;
import cc.uncarbon.module.sys.model.request.AdminSysUserUpsertRequest;
import cc.uncarbon.module.tenant.model.request.AdminSysTenantUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.TenantMetaBO;
import cc.uncarbon.module.sys.model.valueobj.SysTenantKickOutUsersBO;
import cc.uncarbon.module.sys.service.SysRoleService;
import cc.uncarbon.module.sys.service.SysTenantService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cc.uncarbon.module.sys.service.impl.SysUserServiceImpl;
import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统租户Facade接口实现类
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysTenantFacadeImpl implements SysTenantFacade {

    private final SysTenantService sysTenantService;
    private final SysRoleService sysRoleService;
    private final SysUserRoleRelationService sysUserRoleRelationService;
    private final SysUserServiceImpl sysUserService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long adminCreate(AdminSysTenantUpsertRequest request) {
        log.info("[系统管理-新增系统租户] >> 入参={}", request);
        sysTenantService.checkRepeat(request);

        /*
        1. 加入一个新租户(tenant)
        这里是直接顺带创建管理员账号了, 你可以根据业务需要决定是否创建
         */
        TenantMetaEntity entity = sysTenantService.adminCreate(request);

        Long newTenantEntityId = entity.getId();
        Long newTenantId = entity.getTenantId();

        /*
        2. 创建一个新角色(role)
        注意: 这里并没有指派其可见菜单，需要用超管账号授权
         */
        Long newRoleId = sysRoleService.adminCreate(
                new AdminSysRoleUpsertRequest()
                        .setTenantId(newTenantId)
                        .setName(request.getName() + "主管理员")
                        .setCode(SysConstant.TENANT_ADMIN_ROLE_CODE)
        );

        // 3. 创建一个新用户
        Long newUserId = sysUserService.adminCreate(
                new AdminSysUserUpsertRequest()
                        .setTenantId(newTenantId)
                        .setPin(request.getTenantAdminPin())
                        .setPasswordOfNewUser(request.getTenantAdminPassword())
                        .setNickname(request.getName() + "主管理员")
                        .setEmail(request.getTenantAdminEmail())
                        .setPhoneNo(request.getTenantAdminPhoneNo())
                        // 默认为正常状态
                        .setStatus(SysUserStatusEnum.ENABLED)
        );

        // 4. 将新用户绑定至新角色上
        sysUserRoleRelationService.adminCreate(newTenantId, newUserId, newRoleId);

        // 5. 把管理员账号更新进库
        TenantMetaEntity update = new TenantMetaEntity().setTenantAdminUserId(newUserId);
        update.setId(newTenantEntityId);
        sysTenantService.adminUpdate(update);
        return newTenantId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysTenantKickOutUsersBO adminUpdate(AdminSysTenantUpsertRequest request) {
        sysTenantService.adminUpdate(request);

        if (request.getStatus() == EnabledStatusEnum.DISABLED) {
            Long tenantId = CollUtil.getFirst(determineTenantIdsByPrimaryKeys(Collections.singleton(request.getId())).values());
            // 新状态是禁用，查出需要强制登出的用户
            Long tenantId = CollUtil.getFirst(determineTenantIdsByPrimaryKeys(Collections.singleton(dto.getId())).values());
            if (Objects.nonNull(tenantId)) {
                List<Long> tenantSysUserIds = sysUserService.listUserIdsByTenantId(tenantId, Collections.singleton(EnabledStatusEnum.ENABLED));
                return new SysTenantKickOutUsersBO(tenantSysUserIds);
            }
        }
        return new SysTenantKickOutUsersBO();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysTenantKickOutUsersBO adminDelete(Collection<Long> ids) {
        Collection<Long> tenantIds = determineTenantIdsByPrimaryKeys(ids).values();
        if (CollUtil.isNotEmpty(tenantIds)) {
            // 不能删除「超级租户」（租户ID=0）
            SysErrorEnum.CANNOT_DELETE_PRIVILEGED_TENANT.assertNotContains(tenantIds, HeliumConstant.Tenant.DEFAULT_PRIVILEGED_TENANT_ID);

            // 删除租户管理员角色、租户
            sysRoleService.adminDeleteTenantRoles(tenantIds, Collections.singleton(SysConstant.TENANT_ADMIN_ROLE_VALUE));
            sysTenantService.adminDelete(ids);

            // 查出需要强制登出的用户
            List<Long> tenantSysUserIds = tenantIds.stream()
                    .map(tenantId -> sysUserService.listUserIdsByTenantId(tenantId, Collections.singleton(EnabledStatusEnum.ENABLED)))
                    .flatMap(Collection::stream).toList();
            return new SysTenantKickOutUsersBO(tenantSysUserIds);
        }
        return new SysTenantKickOutUsersBO();
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 根据租户主键ID，确定租户IDs
     * @return map[主键ID, 租户ID]
     */
    private Map<Long, Long> determineTenantIdsByPrimaryKeys(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Map.of();
        }
        List<TenantMetaBO> sysTenantInfos = sysTenantService.listByIds(ids, false);
        if (CollUtil.isEmpty(sysTenantInfos)) {
            return Map.of();
        }
        return sysTenantInfos.stream().collect(Collectors.toMap(TenantMetaBO::getId, TenantMetaBO::getTenantId, StreamFunction.ignoredThrowingMerger()));
    }

}
