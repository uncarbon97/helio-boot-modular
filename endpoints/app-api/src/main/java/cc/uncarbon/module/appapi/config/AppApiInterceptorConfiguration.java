package cc.uncarbon.module.appapi.config;

import cc.uncarbon.module.appapi.props.AppApiProperties;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpKit;
import cn.dev33.satoken.interceptor.SaInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * app-api 专用拦截器
 * /app/** 开头的 API 端点，默认都需要登录，单独放行则需要在配置文件的 "app-api.no-auth-paths" 中设置
 *
 * @author Uncarbon
 */
@EnableConfigurationProperties(value = AppApiProperties.class)
@RequiredArgsConstructor
@Configuration
public class AppApiInterceptorConfiguration implements WebMvcConfigurer {

    private final AppApiProperties appApiProperties;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                .addInterceptor(new SaInterceptor(_ -> StpKit.APP.checkLogin()))
                .addPathPatterns(ApiPathPrefix.APP_PATTERN)
                .excludePathPatterns(appApiProperties.getNoAuthPaths());
    }
}
