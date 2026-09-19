package cc.uncarbon.module.config;

import cc.uncarbon.framework.helium.websecurity.props.HeliumWebSecurityProperties;
import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域设置
 * 因可能个别项目需要单独设置，单拎出来放在这
 * 来源列表配置于上游 helium-starter-web-security 的 HeliumWebSecurityProperties.Cors
 * （配置键：helium.web.security.cors.allowed-origins）
 *
 * @author Uncarbon
 */
@RequiredArgsConstructor
@Configuration
public class CorsConfigurer implements WebMvcConfigurer {

    private final HeliumWebSecurityProperties heliumWebSecurityProperties;


    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var cors = heliumWebSecurityProperties.getCors();
        String[] origins = CollUtil.defaultIfEmpty(
                cors.getAllowedOrigins(), java.util.List.of("http://localhost:*"))
                .toArray(String[]::new);
        boolean allowAnyOrigin = CollUtil.contains(cors.getAllowedOrigins(), "*");

        registry
                // 设置允许跨域的路由规则
                .addMapping("/**")
                // 设置允许跨域请求的域名
                .allowedOriginPatterns(origins)
                // 是否允许证书（cookies）；任意来源时禁止携带凭据
                .allowCredentials(!allowAnyOrigin)
                // 设置允许的方法
                .allowedMethods("*")
                // 跨域允许时间
                .maxAge(3600);
    }
}
