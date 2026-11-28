package cc.uncarbon.module.context;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;


/**
 * 用户态、租户态等上下文任务装饰器
 *
 * @author Uncarbon
 */
@Configuration
public class ContextTaskDecorator {

    @Bean
    public TaskDecorator taskDecorator() {
        return ContextBinder::wrapWithContext;
    }
}
