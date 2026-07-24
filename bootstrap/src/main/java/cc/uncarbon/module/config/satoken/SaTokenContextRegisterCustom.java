package cc.uncarbon.module.config.satoken;

import cc.uncarbon.framework.helium.satoken.context.SaTokenContextForScopedValue;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.filter.SaFirewallCheckFilterForJakartaServlet;
import cn.dev33.satoken.filter.SaTokenCorsFilterForJakartaServlet;
import cn.dev33.satoken.spring.SaTokenContextRegister;
import cn.dev33.satoken.spring.pathmatch.SaPathPatternParserUtil;
import cn.dev33.satoken.strategy.SaStrategy;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义 SA-Token 上下文注册类，替代默认的 {@link SaTokenContextRegister}
 */
@Configuration
public class SaTokenContextRegisterCustom {

    /**
     * 注册基于 {@link ScopedValue} 的 sa-token 上下文，替代官方默认的 ThreadLocal 版本 {@link SaTokenContextForThreadLocal}
     * 以兼容虚拟线程
     */
    @PostConstruct
    public void registerScopedValueSaTokenContext() {
        SaManager.setSaTokenContext(new SaTokenContextForScopedValue());
    }

    /*
    以下代码从 SaTokenContextRegister 直接抄
    但去掉了 SaTokenContextFilterForJakartaServlet
     */

    public SaTokenContextRegisterCustom() {
        // 重写路由匹配算法
        SaStrategy.instance.routeMatcher = SaPathPatternParserUtil::match;
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
