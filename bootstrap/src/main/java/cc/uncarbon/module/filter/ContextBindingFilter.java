package cc.uncarbon.module.filter;

import cc.uncarbon.framework.helium.base.constant.ServletFilterOrder;
import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.web.context.SimpleVisitorContext;
import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.framework.helium.web.context.VisitorContextHolder;
import cc.uncarbon.framework.helium.web.util.IPUtil;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpKit;
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
        UserContext u = resolveUser(stpLogic);
        TenantContext t = resolveTenant(stpLogic);

        try {
            ScopedValue.where(VisitorContextHolder.scoped(), v)
                    .where(UserContextHolder.scoped(), u)
                    .where(TenantContextHolder.scoped(), t)
                    .call(() -> {
                        chain.doFilter(servletRequest, servletResponse);
                        return null;
                    });
        } catch (ServletException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @NonNull
    private VisitorContext resolveVisitor(HttpServletRequest servletRequest) {
        return new SimpleVisitorContext()
                .setIp(IPUtil.getClientIPAddress(servletRequest))
                .setUserAgent(servletRequest.getHeader(HttpHeaders.USER_AGENT))
                .setHttpRequestMethod(servletRequest.getMethod())
                .setHttpRequestPath(servletRequest.getRequestURI());
    }

    @Nullable
    private UserContext resolveUser(@Nullable StpLogic stpLogic) {
        if (stpLogic != null && stpLogic.isLogin()
                && stpLogic.getSession().get(UserContext.CAMEL_NAME) instanceof UserContext u) {
            return u;
        }
        return null;
    }

    @Nullable
    private TenantContext resolveTenant(@Nullable StpLogic stpLogic) {
        if (stpLogic != null && stpLogic.isLogin()
                && stpLogic.getSession().get(TenantContext.CAMEL_NAME) instanceof TenantContext t) {
            return t;
        }
        return null;
    }

    @Nullable
    private StpLogic resolveStpLogic(HttpServletRequest servletRequest) {
        // 根据路径前缀，确认对应的 StpLogic
        String path = servletRequest.getRequestURI();
        if (pathMatcher.match(ApiPathPrefix.ADMIN_PATTERN, path)) {
            return StpKit.ADMIN;
        } else if (pathMatcher.match(ApiPathPrefix.APP_PATTERN, path)) {
            return StpKit.APP;
        }
        return null;
    }
}
