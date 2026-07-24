package cc.uncarbon.module.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;


/**
 * 异步任务配置器
 *
 * @author Uncarbon
 */
@EnableAsync
@Configuration
@Slf4j
public class LocalAsyncConfigurer implements AsyncConfigurer {

    /**
     * 虚拟线程不必乱注入什么 Bean 了，用 Spring Boot 自带的即可。
     * 加个日志打印
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("异步任务执行失败: {}#{}", method.getDeclaringClass().getSimpleName(),
                        method.getName(), ex);
    }
}
