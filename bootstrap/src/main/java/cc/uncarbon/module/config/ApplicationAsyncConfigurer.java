package cc.uncarbon.module.config;

import cc.uncarbon.module.context.ContextBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
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
public class ApplicationAsyncConfigurer implements AsyncConfigurer {

    /**
     * 全量上下文任务装饰器
     *
     * <p>Spring Boot 自动装配的 applicationTaskExecutor（虚拟线程模式下为 SimpleAsyncTaskExecutor）
     * 会自动应用容器内唯一的 {@link TaskDecorator} Bean，覆盖 @Async、显式 taskExecutor.execute()
     * 及 CompletableFuture 延迟执行器提交的子线程；上下文捕获与恢复见
     * {@link ContextBinder#wrapWithContext(Runnable)}。</p>
     */
    @Bean
    public TaskDecorator taskDecorator() {
        return ContextBinder::wrapWithContext;
    }

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
