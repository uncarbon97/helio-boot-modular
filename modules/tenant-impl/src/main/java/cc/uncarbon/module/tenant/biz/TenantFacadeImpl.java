package cc.uncarbon.module.tenant.biz;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import cc.uncarbon.framework.helium.tenant.enums.TenantStrategyEnum;
import cc.uncarbon.framework.helium.tenant.props.HeliumTenantProperties;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.tenant.errorcode.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cc.uncarbon.module.tenant.model.valueobj.TenantLoginContextResult;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaDTO;
import cc.uncarbon.module.tenant.model.valueobj.TenantValidateResult;
import cc.uncarbon.module.tenant.service.TenantService;
import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 租户门面
 * 租户视角域的查询（可选租户、切换目标、切入校验、默认租户）在此收敛；
 * 用户侧信息（角色、归属）经 {@link TenantUserRoleFacade} 反向查询，避免 sys 对 tenant 实现的强耦合
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TenantFacadeImpl implements TenantFacade {

    private static final String LOG_PREFIX = "[租户门面]";

    private final TenantService tenantService;
    private final HeliumTenantProperties props;
    private final TenantUserRoleFacade tenantUserRoleFacade;


    @Override
    public TenantValidateResult validateByCode(@Nullable String tenantCode) {
        if (tenantCode == null) {
            // 未提供租户编码：仅在多租户关闭（strategy=NONE，单体开发态）时放行
            return props.doesTenantEnabled()
                    ? TenantValidateResult.fail(TenantErrorCodeEnum.A03001)
                    : TenantValidateResult.pass();
        }
        try {
            // 忽略租户态，从主数据库查询租户元数据；无论何种策略，提供了编码就真实校验，保持行为一致
            return TenantContextHolder.callIgnored(() -> validateMeta(tenantService.getByCode(tenantCode, false)));
        } catch (Exception e) {
            log.warn(LOG_PREFIX + "校验租户编码失败 >> {}", tenantCode, e);
            return TenantValidateResult.fail(TenantErrorCodeEnum.B03001);
        }
    }

    @Override
    public TenantValidateResult validateById(@Nullable Long tenantId) {
        if (tenantId == null) {
            return props.doesTenantEnabled()
                    ? TenantValidateResult.fail(TenantErrorCodeEnum.A03001)
                    : TenantValidateResult.pass();
        }
        try {
            return TenantContextHolder.callIgnored(() -> validateMeta(tenantService.getById(tenantId)));
        } catch (Exception e) {
            log.warn(LOG_PREFIX + "校验租户ID失败 >> {}", tenantId, e);
            return TenantValidateResult.fail(TenantErrorCodeEnum.B03001);
        }
    }

    @Override
    public List<TenantContext> listEnabled() {
        try {
            return TenantContextHolder.callIgnored(() ->
                    tenantService.listEnabled().stream()
                            .map(meta -> (TenantContext) new SimpleTenantContext(meta.getId(), meta.getName(), meta.getCode()))
                            .toList());
        } catch (Exception e) {
            log.warn(LOG_PREFIX + "列举启用租户失败", e);
            return List.of();
        }
    }

    @Override
    public List<TenantContext> listSelectableTenants(Long userId) {
        if (tenantUserRoleFacade.isSuperAdmin(userId)) {
            return listEnabled();
        }
        if (TenantLoginModeEnum.USER_FIRST == props.getLoginMode()) {
            return toContexts(tenantUserRoleFacade.listUserEnabledTenantIds(userId));
        }
        // 租户优先模式：仅归属租户
        Long homeTenantId = tenantUserRoleFacade.getUserTenantId(userId);
        return homeTenantId == null ? List.of() : toContexts(List.of(homeTenantId));
    }

    @Override
    public TenantContext resolveSwitchTarget(String tenantCode) {
        TenantValidateResult valid = validateByCode(tenantCode);
        if (!valid.isValid()) {
            throw new BusinessException(valid.getErrorCode());
        }
        if (valid.getTenantId() == null) {
            // 多租户特性未开启
            throw new BusinessException(TenantErrorCodeEnum.A03001);
        }
        return new SimpleTenantContext(valid.getTenantId(), valid.getTenantName(), valid.getTenantCode());
    }

    @Override
    public void assertSwitchable(Long userId, Long targetTenantId) {
        if (tenantUserRoleFacade.isSuperAdmin(userId)) {
            return;
        }
        if (TenantLoginModeEnum.TENANT_FIRST == props.getLoginMode()) {
            // 租户优先模式：仅归属租户
            Long homeTenantId = tenantUserRoleFacade.getUserTenantId(userId);
            if (!Objects.equals(homeTenantId, targetTenantId)) {
                throw new BusinessException(TenantErrorCodeEnum.A03009);
            }
            return;
        }
        if (TenantLoginModeEnum.USER_FIRST == props.getLoginMode()) {
            if (!tenantUserRoleFacade.listUserEnabledTenantIds(userId).contains(targetTenantId)) {
                throw new BusinessException(TenantErrorCodeEnum.A03009);
            }
            return;
        }
    }

    @Override
    public TenantContext resolveDefaultTenant(Long userId) {
        if (TenantLoginModeEnum.USER_FIRST == props.getLoginMode()) {
            List<Long> tenantIds = tenantUserRoleFacade.listUserEnabledTenantIds(userId);
            if (tenantIds.isEmpty()) {
                return null;
            }
            List<TenantContext> contexts = toContexts(tenantIds);
            return contexts.isEmpty() ? null : contexts.get(0);
        }
        Long homeTenantId = tenantUserRoleFacade.getUserTenantId(userId);
        if (homeTenantId == null) {
            return null;
        }
        List<TenantContext> contexts = toContexts(List.of(homeTenantId));
        return contexts.isEmpty() ? null : contexts.get(0);
    }

    @Override
    public TenantLoginContextResult resolveLoginTenant(Long userId, boolean userIsSuperAdmin,
                                                       @Nullable String tenantCode, @Nullable Long userTenantId) {
        TenantLoginContextResult ret = new TenantLoginContextResult(props.getLoginMode());
        if (userIsSuperAdmin) {
            // 超级管理员一律平台视角登录，忽略请求中的租户编码；切换租户是登录后的显式动作
            return ret.setTenantContext(null).setTenantOptions(List.of());
        }
        if (TenantLoginModeEnum.TENANT_FIRST == props.getLoginMode()) {
            // 租户优先模式（先租户后用户）：登录时提供租户编码
            TenantValidateResult valid = validateByCode(tenantCode);
            if (!valid.isValid()) {
                throw new BusinessException(valid.getErrorCode());
            }
            if (valid.getTenantId() != null) {
                SysErrorCodeEnum.A01008.throwIfNull(userTenantId);
                if (!Objects.equals(valid.getTenantId(), userTenantId)) {
                    throw new BusinessException(TenantErrorCodeEnum.A03010);
                }
                return ret.setTenantContext(new SimpleTenantContext(valid.getTenantId(), valid.getTenantName(), valid.getTenantCode()))
                        .setTenantOptions(List.of());
            }
        }
        if (TenantLoginModeEnum.USER_FIRST == props.getLoginMode()) {
            // 用户优先模式（先用户后租户）：免填租户编码，按用户-租户关联推导
            List<TenantContext> selectableTenants = listSelectableTenants(userId);
            if (selectableTenants.isEmpty()) {
                // 无任何归属租户：个人空间（空数据视图），角色快照不按租户限定
                return ret.setTenantContext(null).setTenantOptions(List.of());
            }
            // 默认租户排最前，直接进入
            return ret.setTenantContext(CollUtil.getFirst(selectableTenants))
                    // 多归属时附可选列表供前端切换
                    .setTenantOptions(selectableTenants.size() > 1 ? selectableTenants : List.of())
                    .setTenantScopedRole(true);
        }
        throw new IllegalStateException("TenantStrategy 为" + TenantStrategyEnum.NONE + " 且未填编码");
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private TenantValidateResult validateMeta(@Nullable TenantMetaDTO tenantMeta) {
        if (Objects.isNull(tenantMeta)) {
            return TenantValidateResult.fail(TenantErrorCodeEnum.A03001);
        }
        if (EnabledStatusEnum.DISABLED == tenantMeta.getStatus()) {
            return TenantValidateResult.fail(TenantErrorCodeEnum.A03006);
        }
        return TenantValidateResult.pass(tenantMeta);
    }

    /**
     * 候选租户ID → 有效（存在且启用）租户上下文，保持入参顺序（默认租户排最前）
     */
    private List<TenantContext> toContexts(List<Long> tenantIds) {
        return tenantIds.stream()
                .map(this::validateById)
                .filter(TenantValidateResult::isValid)
                .filter(valid -> valid.getTenantId() != null)
                .map(valid -> (TenantContext) new SimpleTenantContext(
                        valid.getTenantId(), valid.getTenantName(), valid.getTenantCode()))
                .toList();
    }

}
