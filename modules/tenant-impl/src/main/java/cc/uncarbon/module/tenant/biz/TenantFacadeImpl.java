package cc.uncarbon.module.tenant.biz;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import cc.uncarbon.framework.helium.tenant.props.HeliumTenantProperties;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.tenant.constant.TenantConstant;
import cc.uncarbon.module.tenant.errorcode.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaDTO;
import cc.uncarbon.module.tenant.model.valueobj.TenantValidateResult;
import cc.uncarbon.module.tenant.service.TenantService;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 租户门面
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TenantFacadeImpl implements TenantFacade {

    private static final String LOG_PREFIX = "[租户管理][租户门面]";


    private final TenantService tenantService;
    private final TenantUserRoleFacade tenantUserRoleFacade;
    private final HeliumTenantProperties props;


    @Override
    public TenantValidateResult validateByCode(@Nullable String tenantCode) {
        if (!props.doesTenantEnabled()) {
            return TenantValidateResult.pass();
        }
        if (CharSequenceUtil.isBlank(tenantCode)) {
            // 空编码明确报错，不静默回退默认租户
            return TenantValidateResult.fail(TenantErrorCodeEnum.A03011);
        }
        try {
            // 忽略租户态，从主数据库查询租户元数据
            return TenantContextHolder.callIgnored(() -> {
                TenantMetaDTO tenantMeta = tenantService.getByCode(tenantCode, false);
                if (Objects.nonNull(tenantMeta)) {
                    if (EnabledStatusEnum.DISABLED == tenantMeta.getStatus()) {
                        return TenantValidateResult.fail(TenantErrorCodeEnum.A03006);
                    }
                    return TenantValidateResult.pass(tenantMeta);
                }
                return TenantValidateResult.fail(TenantErrorCodeEnum.A03001);
            });
        } catch (Exception e) {
            log.error(LOG_PREFIX + " 验证租户编码失败 >> {}", tenantCode, e);
            return TenantValidateResult.fail(TenantErrorCodeEnum.B03001);
        }
    }

    @Override
    public boolean isTenantEnabled() {
        return props.doesTenantEnabled();
    }

    @Override
    public TenantLoginModeEnum getLoginMode() {
        return props.getLoginMode();
    }

    @Override
    public List<TenantContext> listSelectableTenants(@Nullable Long userId) {
        if (userId == null) {
            return List.of();
        }
        try {
            return TenantContextHolder.callIgnored(() -> {
                if (tenantUserRoleFacade.isSuperAdmin(userId)) {
                    // 超级管理员：除平台自营域外的全部启用租户
                    return tenantService.listEnabled().stream()
                            .filter(meta -> !Objects.equals(meta.getId(), TenantConstant.FIRST_PARTY_TENANT_ID))
                            .map(TenantMetaDTO::toTenantContext)
                            .toList();
                }
                if (TenantLoginModeEnum.USER_FIRST == props.getLoginMode()) {
                    // USER_FIRST：按 relation 真源展开本人启用关联租户（排除平台自营域，保持加入先后顺序）
                    List<TenantContext> related = new ArrayList<>();
                    for (Long tenantId : tenantUserRoleFacade.listEnabledTenantIdsByUser(userId)) {
                        if (TenantConstant.FIRST_PARTY_TENANT_ID == tenantId) {
                            continue;
                        }
                        TenantContext context = resolveEnabledTenant(tenantId);
                        if (context != null) {
                            related.add(context);
                        }
                    }
                    return List.copyOf(related);
                }
                // TENANT_FIRST：普通用户即归属租户
                TenantContext home = resolveDefaultTenant(userId);
                return home == null ? List.of() : List.of(home);
            });
        } catch (Exception e) {
            log.error(LOG_PREFIX + " 列举可切换租户失败 >> userId={}", userId, e);
            return List.of();
        }
    }

    @Override
    public TenantContext resolveSwitchTarget(String tenantCode) {
        TenantMetaDTO meta;
        try {
            meta = TenantContextHolder.callIgnored(() -> tenantService.getByCode(tenantCode, false));
        } catch (Exception e) {
            log.error(LOG_PREFIX + " 解析切换目标租户失败 >> {}", tenantCode, e);
            throw new BusinessException(TenantErrorCodeEnum.B03001);
        }
        if (meta == null) {
            throw new BusinessException(TenantErrorCodeEnum.A03001);
        }
        if (EnabledStatusEnum.DISABLED == meta.getStatus()) {
            throw new BusinessException(TenantErrorCodeEnum.A03006);
        }
        return meta.toTenantContext();
    }

    @Override
    public void assertSwitchable(Long userId, Long targetTenantId) {
        if (userId != null && tenantUserRoleFacade.isSuperAdmin(userId)) {
            return;
        }
        if (userId != null && targetTenantId != null
                // USER_FIRST：普通用户可切入本人启用关联的租户（平台自营域除外）
                && TenantLoginModeEnum.USER_FIRST == props.getLoginMode()
                && TenantConstant.FIRST_PARTY_TENANT_ID != targetTenantId
                && tenantUserRoleFacade.listEnabledTenantIdsByUser(userId).contains(targetTenantId)) {
            return;
        }
        throw new BusinessException(TenantErrorCodeEnum.A03012);
    }

    @Nullable
    @Override
    public TenantContext resolveEnabledTenant(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        try {
            return TenantContextHolder.callIgnored(() -> {
                TenantMetaDTO meta = tenantService.getById(tenantId);
                return meta == null || EnabledStatusEnum.DISABLED == meta.getStatus() ? null : meta.toTenantContext();
            });
        } catch (Exception e) {
            log.error(LOG_PREFIX + " 解析启用租户失败 >> tenantId={}", tenantId, e);
            return null;
        }
    }

    @Nullable
    @Override
    public TenantContext resolveDefaultTenant(Long userId) {
        if (userId == null) {
            return null;
        }
        Long homeTenantId = tenantUserRoleFacade.getUserHomeTenantId(userId);
        if (homeTenantId == null) {
            return null;
        }
        try {
            return TenantContextHolder.callIgnored(() -> {
                TenantMetaDTO meta = tenantService.getById(homeTenantId);
                if (meta == null || EnabledStatusEnum.DISABLED == meta.getStatus()) {
                    return null;
                }
                return meta.toTenantContext();
            });
        } catch (Exception e) {
            log.error(LOG_PREFIX + " 解析默认租户失败 >> userId={}", userId, e);
            return null;
        }
    }
}
