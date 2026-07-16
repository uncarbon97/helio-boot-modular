package cc.uncarbon.module.adminapi.event;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;

/**
 * 刷新角色权限缓存事件
 */
@Getter
public final class RefreshRolePermissionCacheEvent extends ApplicationEvent {

    private final transient EventData data;

    public RefreshRolePermissionCacheEvent(EventData data) {
        super(data);
        this.data = data;
    }

    /**
     * @param roleIds 角色ID集合
     * @param tenantId 租户ID；非租户环境传入null
     */
    public record EventData(@NonNull Collection<Long> roleIds, @Nullable Long tenantId) {

    }
}
