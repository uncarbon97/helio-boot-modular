package cc.uncarbon.module.tenant.facade;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.module.tenant.model.valueobj.TenantLoginContextResult;
import cc.uncarbon.module.tenant.model.valueobj.TenantValidateResult;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 租户门面
 */
public interface TenantFacade {

    /**
     * 校验传入的租户编码
     */
    TenantValidateResult validateByCode(@Nullable String tenantCode);



    /**
     * 校验传入的租户ID
     */
    TenantValidateResult validateById(@Nullable Long tenantId);

    /**
     * 列举启用状态的租户（仅三元组信息，供租户切换器使用）
     */
    List<TenantContext> listEnabled();

    /**
     * 列举用户可选租户（租户视角域）
     * 超级管理员：全部启用租户
     * 用户优先模式（USER_FIRST）用户：关联表内启用租户，默认租户排最前
     * 租户优先模式（TENANT_FIRST）用户：仅归属租户（单元素）
     */
    List<TenantContext> listSelectableTenants(Long userId);

    /**
     * 解析切换目标租户
     *
     * @throws BusinessException 租户不存在或已禁用
     */
    TenantContext resolveSwitchTarget(String tenantCode);

    /**
     * 校验用户可切入目标租户
     * 超级管理员放行
     * 用户优先模式需在关联内且启用
     * 租户优先模式仅归属租户
     *
     * @throws BusinessException 无权切入
     */
    void assertSwitchable(Long userId, Long targetTenantId);

    /**
     * 用户默认租户上下文
     * 用户优先模式取关联表默认租户，租户优先模式取 sys_user.tenant_id 归属
     *
     * @return 无归属或归属租户均不可用时返回 null（个人空间）
     */
    TenantContext resolveDefaultTenant(Long userId);

    /**
     * 登录链路-按登录模式推导租户上下文
     * 超级管理员：平台视角（tenantContext=null），忽略 tenantCode
     * 用户优先模式（USER_FIRST）：按用户-租户关联推导，多归属时附 tenantOptions，默认租户排最前
     * 租户优先模式（TENANT_FIRST）：校验 tenantCode 与用户归属一致性
     *
     * @param userIsSuperAdmin 调用方已判定的超管标记
     * @param tenantCode       登录请求中的租户编码，可空
     * @param userTenantId     用户归属租户ID（租户优先模式一致性校验用，可空）
     * @throws BusinessException 编码无效或未提供、租户已禁用、账号未归属租户、编码与归属不一致等
     */
    TenantLoginContextResult resolveLoginTenant(Long userId, boolean userIsSuperAdmin,
                                                @Nullable String tenantCode, @Nullable Long userTenantId);

}
