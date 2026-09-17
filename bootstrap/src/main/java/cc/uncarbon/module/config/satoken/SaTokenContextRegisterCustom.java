package cc.uncarbon.module.config.satoken;

import cc.uncarbon.framework.helium.satoken.context.SaTokenContextForScopedValue;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.filter.SaFirewallCheckFilterForJakartaServlet;
import cn.dev33.satoken.filter.SaTokenCorsFilterForJakartaServlet;
import cn.dev33.satoken.spring.SaTokenContextRegister;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义 SA-Token 上下文注册类，替代默认的 {@link SaTokenContextRegister}
 * 去除了 {@link SaTokenContextRegister#saTokenContextFilterForServlet()} 的注册
 */
@Configuration
public class SaTokenContextRegisterCustom extends SaTokenContextRegister {

    public SaTokenContextRegisterCustom() {
        super();
    }

    /**
     * 注册基于 {@link ScopedValue} 的 sa-token 上下文，替代官方默认的 ThreadLocal 版本 {@link SaTokenContextForThreadLocal}
     * 以兼容虚拟线程
     */
    @PostConstruct
    public void registerScopedValueSaTokenContext() {
        SaManager.setSaTokenContext(new SaTokenContextForScopedValue());
    }

    /**
     * CORS 跨域策略过滤器
     *
     * @return /
     */
    @Bean
    public SaTokenCorsFilterForJakartaServlet saTokenCorsFilterForJakartaServlet() {
        return new SaTokenCorsFilterForJakartaServlet();
    }

    /**
     * 防火墙过滤器
     *
     * @return /
     */
    @Bean
    public SaFirewallCheckFilterForJakartaServlet saFirewallCheckFilterForJakartaServlet() {
        return new SaFirewallCheckFilterForJakartaServlet();
    }
}
