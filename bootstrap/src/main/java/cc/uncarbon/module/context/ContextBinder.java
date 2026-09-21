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

    private static ScopedValue.Carrier carrierOf(VisitorContext visitor, UserContext user,
                                                 TenantContext tenant, I18nContext i18n) {
        return ScopedValue
                .where(VisitorContextHolder.scoped(), visitor)
                .where(UserContextHolder.scoped(), user)
                .where(TenantContextHolder.scoped(), tenant)
                .where(I18nContextHolder.scoped(), i18n);
    }
}
