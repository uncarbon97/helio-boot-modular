package cc.uncarbon.module.filter;

import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.satoken.context.SaTokenContextForScopedValue;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.web.constant.ServletFilterOrder;
import cc.uncarbon.framework.helium.web.context.SimpleVisitorContext;
import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.framework.helium.web.util.IPUtil;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.context.ContextBinder;
import cn.dev33.satoken.context.model.SaTokenContextModelBox;
import cn.dev33.satoken.stp.StpLogic;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 上下文绑定过滤器
 * 覆盖 {@link VisitorContext}、{@link UserContext} 和 {@link TenantContext}，同时绑定 sa-token 请求上下文
 *
 * <p>所有上下文均基于 {@link ScopedValue}，兼容虚拟线程；作用域内启动的子线程会自动继承绑定。</p>
 *
 * @author Uncarbon
 */
@Order(ServletFilterOrder.CONTEXT_BINDING_FILTER)
@Component
public class ContextBindingFilter extends OncePerRequestFilter {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest servletRequest,
                                    @NonNull HttpServletResponse servletResponse,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        VisitorContext v = resolveVisitor(servletRequest);
        StpLogic stpLogic = resolveStpLogic(servletRequest);
        SaTokenContextModelBox box = SaTokenContextForScopedValue.boxOf(servletRequest, servletResponse);
        try {
            SaTokenContextForScopedValue.where(box).call(() -> {
                UserContext u = resolveUser(stpLogic);
                TenantContext t = resolveTenant(stpLogic);
                // 内层：复用 ContextBinder 绑定三类业务上下文（嵌套 ScopedValue，天然继承外层绑定）
                ContextBinder.callWithContext(v, u, t, () -> {
                    chain.doFilter(servletRequest, servletResponse);
                    return null;
                });
                return null;
            });
        } catch (ServletException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private @NonNull VisitorContext resolveVisitor(HttpServletRequest servletRequest) {
        return new SimpleVisitorContext()
                .setIp(IPUtil.getClientIPAddress(servletRequest, 0))
                // SpringMVC 已经对 UA 做了基本的过滤
                .setUserAgent(servletRequest.getHeader(HttpHeaders.USER_AGENT))
                .setHttpRequestMethod(servletRequest.getMethod())
                .setHttpRequestPath(servletRequest.getRequestURI());
    }

    private @Nullable StpLogic resolveStpLogic(HttpServletRequest servletRequest) {
        // 根据路径前缀，确认对应的 StpLogic
        String path = servletRequest.getRequestURI();
        if (pathMatcher.match(ApiPathPrefix.ADMIN_PATTERN, path)) {
            return StpKit.ADMIN;
        } else if (pathMatcher.match(ApiPathPrefix.APP_PATTERN, path)) {
            return StpKit.APP;
        }
        return null;
    }

    private @Nullable UserContext resolveUser(@Nullable StpLogic stpLogic) {
        if (stpLogic != null && stpLogic.isLogin()
                && stpLogic.getSession().get(UserContext.CAMEL_NAME) instanceof UserContext u) {
            return u;
        }
        return null;
    }

    private @Nullable TenantContext resolveTenant(@Nullable StpLogic stpLogic) {
        if (stpLogic != null && stpLogic.isLogin()
                && stpLogic.getSession().get(TenantContext.CAMEL_NAME) instanceof TenantContext t) {
            return t;
        }
        return null;
    }
}
