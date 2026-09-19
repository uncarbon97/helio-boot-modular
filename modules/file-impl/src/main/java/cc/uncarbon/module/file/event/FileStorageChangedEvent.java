package cc.uncarbon.module.file.event;

import cc.uncarbon.module.file.support.DynamicFileStorageRegistrar;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationEvent;

/**
 * 文件存储点发生变化事件
 *
 * @see DynamicFileStorageRegistrar 事务提交后动态同步底层存储平台
 */
@Getter
public final class FileStorageChangedEvent extends ApplicationEvent {

    private final transient EventData data;

    public FileStorageChangedEvent(@NonNull EventData data) {
        super(data);
        this.data = data;
    }

    /**
     * 变更类型
     */
    public enum ChangeType {
        /**
         * 新增
         */
        CREATE,
        /**
         * 修改
         */
        UPDATE,
        /**
         * 删除
         */
        DELETE
    }

    public record EventData(@NonNull ChangeType type) {

    }
}
