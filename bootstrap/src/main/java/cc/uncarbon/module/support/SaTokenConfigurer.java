package cc.uncarbon.module.support;

import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.satoken.context.SaTokenContextForScopedValue;
import cc.uncarbon.module.adminapi.helper.RolePermissionCacheHelper;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.spring.SaTokenContextRegister;
import cn.dev33.satoken.stp.StpInterface;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * SA-Token 二次配置类
 *
 * @author Uncarbon
 */
@RequiredArgsConstructor
@Configuration
public class SaTokenConfigurer implements StpInterface, WebMvcConfigurer {

    private final RolePermissionCacheHelper rolePermissionCacheHelper;


    /**
     * 注册基于 {@link ScopedValue} 的 sa-token 上下文，替代官方默认的 ThreadLocal 版本 {@link SaTokenContextForThreadLocal}
     * 以兼容虚拟线程
     */
    @PostConstruct
    public void registerScopedValueSaTokenContext() {
        SaManager.setSaTokenContext(new SaTokenContextForScopedValue());
    }

    /**
     * 覆盖 {@link SaTokenContextRegister} 中的 {@link SaTokenContextFilterForJakartaServlet} 注册过程
     * TODO 还是不能直接覆盖，另外想办法吧
     */
    @Bean
    @Primary
    public SaTokenContextFilterForJakartaServlet saTokenContextFilterForJakartaServlet() {
        return new SaTokenContextFilterForJakartaServlet() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                // 什么都不做，直接放行
                chain.doFilter(request, response);
            }
        };
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注解拦截器
        registry
                .addInterceptor(new SaInterceptor())
                .addPathPatterns("/**");
    }

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return rolePermissionCacheHelper.getCurrentUserPermissions();
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        UserContext u = UserContextHolder.getContext();
        if (u != null) {
            Collection<String> roleCodes = u.getRoleCodes();
            if (roleCodes instanceof List<String> asList) {
                return asList;
            }
            return new ArrayList<>(roleCodes);
        }
        return List.of();
    }
}
