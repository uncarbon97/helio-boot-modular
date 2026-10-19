package cc.uncarbon.module.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;


/**
 * 异步任务配置
 *
 * @author Uncarbon
 */
@EnableAsync
@Component
@Slf4j
public class ApplicationAsyncConfigurer implements AsyncConfigurer {

    /**
     * 虚拟线程不必乱注入什么 Bean 了，用 Spring Boot 自带的即可。
     * 日志打印格式自定义
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("异步任务执行失败: {}#{}", method.getDeclaringClass().getSimpleName(),
                        method.getName(), ex);
    }
}
