package cc.uncarbon.module.tenant.biz;

import cc.uncarbon.framework.helium.tenant.props.HeliumTenantProperties;
import cc.uncarbon.module.tenant.errorcode.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaDTO;
import cc.uncarbon.module.tenant.model.valueobj.TenantValidateResult;
import cc.uncarbon.module.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 租户门面
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TenantFacadeImpl implements TenantFacade {

    private final TenantService tenantService;
    private final HeliumTenantProperties props;

//
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public Long adminCreate(AdminTenantCreateRequest request) {
//        log.info("[系统管理-新增系统租户] >> 入参={}", request);
//
//        /*
//        1. 加入一个新租户(tenant)
//        这里是直接顺带创建管理员账号了, 你可以根据业务需要决定是否创建
//         */
//        TenantMetaEntity entity = tenantMetaService.adminCreate(request);
//
//        Long newTenantEntityId = entity.getId();
//        // TODO: tenantId 映射需要确认
//        Long newTenantId = entity.getId();
//
//        /*
//        2. 创建一个新角色(role)
//        注意: 这里并没有指派其可见菜单，需要用超管账号授权
//         */
//        Long newRoleId = sysRoleService.adminCreate(
//                new AdminSysRoleUpsertRequest()
//                        .setTenantId(newTenantId)
//                        .setName(request.getName() + "主管理员")
//                        .setCode(SysConstant.TENANT_ADMIN_ROLE_CODE)
//        );
//
//        // 3. 创建一个新用户
//        Long newUserId = sysUserService.adminCreate(
//                new AdminSysUserUpsertRequest()
//                        .setTenantId(newTenantId)
//                        .setPin(request.getTenantAdminPin())
//                        .setPasswordOfNewUser(request.getTenantAdminPwd())
//                        .setNickname(request.getName() + "主管理员")
//                        .setEmail(request.getTenantAdminEmail())
//                        .setPhoneNo(request.getTenantAdminPhoneNo())
//                        // 默认为正常状态
//                        .setStatus(SysUserStatusEnum.ENABLED)
//        );
//
//        // 4. 将新用户绑定至新角色上
//        sysUserRoleRelationService.adminCreate(newTenantId, newUserId, newRoleId);
//
//        // 5. 把管理员账号更新进库
//        TenantMetaEntity update = new TenantMetaEntity();
//        update.setId(newTenantEntityId);
//        update.setAdminUserId(newUserId);
//        // TODO: 需要 TenantMetaService 提供更新 adminUserId 的方法
//
//        return newTenantId;
//    }
//
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public TenantMetaKickOutUsersBO adminUpdate(AdminTenantMetaUpdateRequest request) {
//        tenantMetaService.adminUpdate(request);
//
//        if (request.getStatus() == EnabledStatusEnum.DISABLED) {
//            Long tenantId = CollUtil.getFirst(determineTenantIdsByPrimaryKeys(Set.of(request.getId())).values());
//            if (Objects.nonNull(tenantId)) {
//                List<Long> tenantSysUserIds = sysUserService.listUserIdsByTenantId(tenantId, Set.of(EnabledStatusEnum.ENABLED));
//                return new TenantMetaKickOutUsersBO(tenantSysUserIds);
//            }
//        }
//        return new TenantMetaKickOutUsersBO();
//    }
//
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public TenantMetaKickOutUsersBO adminDelete(Collection<Long> ids) {
//        Collection<Long> tenantIds = determineTenantIdsByPrimaryKeys(ids).values();
//        if (CollUtil.isNotEmpty(tenantIds)) {
//            // 不能删除「超级租户」（租户ID=0）
//            // TODO: 需要 TenantErrorCodeEnum 提供 CANNOT_DELETE_PRIVILEGED_TENANT
//
//            // 删除租户管理员角色、租户
//            sysRoleService.adminDeleteTenantRoles(tenantIds, Set.of(SysConstant.TENANT_ADMIN_ROLE_CODE));
//            tenantMetaService.adminDelete(ids);
//
//            // 查出需要强制登出的用户
//            List<Long> tenantSysUserIds = tenantIds.stream()
//                    .map(tenantId -> sysUserService.listUserIdsByTenantId(tenantId, Set.of(EnabledStatusEnum.ENABLED)))
//                    .flatMap(Collection::stream).toList();
//            return new TenantMetaKickOutUsersBO(tenantSysUserIds);
//        }
//        return new TenantMetaKickOutUsersBO();
//    }

    @Override
    public TenantValidateResult validateByCode(@Nullable String tenantCode) {
        if (!props.doesTenantEnabled()) {
            return TenantValidateResult.pass();
        }
        if (tenantCode != null) {
            TenantMetaDTO tenantMeta = tenantService.getByCode(tenantCode, false);
            if (Objects.nonNull(tenantMeta)) {
                return TenantValidateResult.pass(tenantMeta);
            }
        }
        return TenantValidateResult.fail(TenantErrorCodeEnum.A03001);
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
//    private Map<Long, Long> determineTenantIdsByPrimaryKeys(Collection<Long> ids) {
//        if (CollUtil.isEmpty(ids)) {
//            return Map.of();
//        }
//        List<TenantMetaDTO> sysTenantInfos = tenantMetaService.listByIds(ids, false);
//        if (CollUtil.isEmpty(sysTenantInfos)) {
//            return Map.of();
//        }
//        // TODO: TenantMetaDTO 没有 getTenantId()，需要确认映射关系
//        return sysTenantInfos.stream().collect(Collectors.toMap(TenantMetaDTO::getId, TenantMetaDTO::getId, StreamFunction.ignoredThrowingMerger()));
//    }

}
