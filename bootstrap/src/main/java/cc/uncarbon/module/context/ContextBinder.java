package cc.uncarbon.module.context;

import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.i18n.context.I18nContext;
import cc.uncarbon.framework.helium.i18n.context.I18nContextHolder;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.framework.helium.web.context.VisitorContextHolder;
import lombok.experimental.UtilityClass;

import java.util.concurrent.Callable;

/**
 * 把各种上下文绑定到当前线程的 {@link ScopedValue} 作用域内
 *
 * @author Uncarbon
 */
@UtilityClass
public final class ContextBinder {

    /**
     * 在作用域内执行，有返回值
     */
    public static <T> T callWithContext(VisitorContext visitor, UserContext user, TenantContext tenant,
                                        I18nContext i18n, Callable<T> op)
            throws Exception {
        ScopedValue.Carrier carrier = carrierOf(visitor, user, tenant, i18n);
        return carrier.call(op::call);
    }

    /**
     * 在作用域内执行，无返回值
     */
    public static void runWithContext(VisitorContext visitor, UserContext user, TenantContext tenant,
                                      I18nContext i18n, Runnable op) {
        ScopedValue.Carrier carrier = carrierOf(visitor, user, tenant, i18n);
        carrier.run(op);
    }

    /**
     * 捕获当前线程全量上下文快照，返回在子线程执行前恢复快照的 {@link Runnable}
     *
     * <p>ScopedValue 并不会被 @Async / taskExecutor 提交的普通虚拟线程继承（仅 StructuredTaskScope 子任务继承），
     * 跨线程提交时需经本方法包装，捕获与恢复的上下文口径同 {@link #runWithContext}，另含忽略租户标记。</p>
     */
    public static Runnable wrapWithContext(Runnable op) {
        VisitorContext visitor = VisitorContextHolder.getContext();
        UserContext user = UserContextHolder.getContext();
        TenantContext tenant = TenantContextHolder.getContext();
        I18nContext i18n = I18nContextHolder.getContext();
        boolean tenantIgnored = TenantContextHolder.isIgnored();

        return () -> carrierOf(visitor, user, tenant, i18n)
                .where(TenantContextHolder.ignoredScoped(), tenantIgnored)
                .run(op);
    }

    private static ScopedValue.Carrier carrierOf(VisitorContext visitor, UserContext user,
                                                 TenantContext tenant, I18nContext i18n) {
        return ScopedValue
                .where(VisitorContextHolder.scoped(), visitor)
                .where(UserContextHolder.scoped(), user)
                .where(TenantContextHolder.scoped(), tenant)
                .where(I18nContextHolder.scoped(), i18n);
    }
}
