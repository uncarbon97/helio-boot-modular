package cc.uncarbon.module.appapi.config;

import cc.uncarbon.module.appapi.props.AppApiProperties;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpKit;
import cn.dev33.satoken.interceptor.SaInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * app-api 专用拦截器
 *
 * @author Uncarbon
 */
@Configuration
@RequiredArgsConstructor
public class AppApiInterceptorConfiguration implements WebMvcConfigurer {

    private final AppApiProperties appApiProperties;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        /*
        /app/** 下的几乎所有接口都需要登录
        放行接口，在配置文件的 app-api.no-auth-paths 中设置
         */
        registry
                .addInterceptor(new SaInterceptor(_ -> StpKit.APP.checkLogin()))
                .addPathPatterns(ApiPathPrefix.APP_PATTERN)
                .excludePathPatterns(appApiProperties.getNoAuthPaths());
    }
}
