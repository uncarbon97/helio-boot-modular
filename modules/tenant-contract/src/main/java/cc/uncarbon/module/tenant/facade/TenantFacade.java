package cc.uncarbon.module.tenant.facade;

import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
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
     * 是否启用多租户隔离
     */
    boolean isTenantEnabled();

    /**
     * 当前登录模式
     */
    TenantLoginModeEnum getLoginMode();

    /**
     * 列举指定用户可切换到的租户
     * <p>超级管理员：除平台自营域外的全部启用租户
     * <p>普通用户：TENANT_FIRST=本人归属租户；USER_FIRST=本人启用关联租户（排除平台自营域）
     */
    List<TenantContext> listSelectableTenants(@Nullable Long userId);

    /**
     * 按租户编码解析切换目标租户
     * <p>不存在 / 已禁用时抛业务异常</p>
     */
    TenantContext resolveSwitchTarget(String tenantCode);

    /**
     * 断言指定用户可以切入目标租户
     * <p>超级管理员可切入任意租户；USER_FIRST 下普通用户可切入本人启用关联租户（平台自营域除外）</p>
     */
    void assertSwitchable(Long userId, Long targetTenantId);

    /**
     * 按 ID 解析启用状态的租户上下文
     *
     * @return 不存在或已禁用时返回 null
     */
    @Nullable
    TenantContext resolveEnabledTenant(Long tenantId);

    /**
     * 解析指定用户的默认（归属）租户上下文
     *
     * @return 无归属租户时返回 null（个人空间/平台视角）
     */
    @Nullable
    TenantContext resolveDefaultTenant(Long userId);

}
