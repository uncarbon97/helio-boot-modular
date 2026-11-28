package cc.uncarbon.module.config;

import org.redisson.config.Config;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;


/**
 * Redisson 配置器
 *
 * @author Uncarbon
 */
@Configuration
public class RedissonConfigurer implements RedissonAutoConfigurationCustomizer {

    /**
     * 让 RTopic 监听器、RExecutorService 任务、RemoteService invocation
     * 全部跑在虚拟线程上，避免占用 Redisson 内部固定池的 16 个平台线程。
     * <p>
     * ⚠ 不要给 setNettyExecutor / setEventLoopGroup 传虚拟线程：
     * Netty EventLoop 要求线程亲和性 + 长期占用，虚拟线程会被 pin 到载体线程，
     * 造成载体线程耗尽 + 事件循环卡死。
     */
    @Override
    public void customize(Config config) {
        config.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}
