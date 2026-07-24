package cc.uncarbon.module.config.satoken;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * SA-Token MVC 拦截器
 *
 * @author Uncarbon
 */
@Configuration
public class SaTokenInterceptorConfigurer implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注解拦截器
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }
}
