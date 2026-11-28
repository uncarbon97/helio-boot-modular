package cc.uncarbon.module.adminapi.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;

/**
 * 强制退出租户切换事件
 * 目标租户被禁用等场景，将切入该租户视角的会话（如超管切入后停留）退回原视角，而非整会话强制登出
 */
@Getter
public final class ExitTenantSwitchEvent extends ApplicationEvent {

    private final transient EventData data;

    public ExitTenantSwitchEvent(EventData data) {
        super(data);
        this.data = data;
    }

    /**
     * @param tenantId   被退出的租户ID
     * @param sysUserIds 需要被强制退出租户切换的系统用户IDs
     */
    public record EventData(Long tenantId, Collection<Long> sysUserIds) {

    }
}
