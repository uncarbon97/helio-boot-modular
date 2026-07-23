package cc.uncarbon.module.support;

import lombok.extern.slf4j.Slf4j;
import org.redisson.config.Config;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executors;


/**
 * 虚拟线程相关配置
 *
 * @author Uncarbon
 */
@EnableAsync
@Configuration
@Slf4j
public class VirtualThreadConfigurer implements AsyncConfigurer, RedissonAutoConfigurationCustomizer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("异步任务执行失败: {}#{}", method.getDeclaringClass().getSimpleName(),
                        method.getName(), ex);
    }

    /**
     * 让 RTopic 监听器、RExecutorService 任务、RemoteService invocation
     * 全部跑在虚拟线程上，避免占用 Redisson 内部固定池的 16 个平台线程。
     * <p>
     * ⚠ 不要给 setNettyExecutor / setEventLoopGroup 传虚拟线程：
     * Netty EventLoop 要求线程亲和性 + 长期占用，虚拟线程会被 pin 到载体线程，
     * 造成载体线程耗尽 + 事件循环卡死。
     */
    @Override
    public void customize(Config configuration) {
        configuration.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}
