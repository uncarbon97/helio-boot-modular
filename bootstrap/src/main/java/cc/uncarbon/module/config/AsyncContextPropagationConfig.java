package cc.uncarbon.module.config;

import cc.uncarbon.framework.helium.tenant.async.TenantContextTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

/**
 * 异步任务上下文透传配置
 *
 * <p>Spring Boot 自动装配的 applicationTaskExecutor 会自动应用容器内唯一的 {@link TaskDecorator} Bean，
 * 覆盖 @Async 与显式 taskExecutor.execute() 提交的子线程；
 * 虚拟线程/结构化并发天然继承 ScopedValue，本装饰器仅兜底不继承的场景（R14）。</p>
 *
 * @author Uncarbon
 */
@Configuration
public class AsyncContextPropagationConfig {

    @Bean
    public TaskDecorator taskDecorator() {
        return new TenantContextTaskDecorator();
    }
}
