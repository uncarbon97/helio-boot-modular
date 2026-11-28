package cc.uncarbon.module.adminapi.event;

import cc.uncarbon.framework.helium.base.context.UserContext;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;

/**
 * 原位刷新系统用户会话快照事件
 * <p>
 * 用于用户-角色关系变化后（分配角色、角色禁用/启用、角色删除、角色编码变更），
 * 重建 sa-token 会话中的 {@link UserContext}，
 * 使角色IDs、角色编码集合立即生效，用户无需重新登录
 *
 * @author Uncarbon
 */
@Getter
public final class RefreshSysUserSessionEvent extends ApplicationEvent {

    private final transient EventData data;

    public RefreshSysUserSessionEvent(EventData data) {
        super(data);
        this.data = data;
    }

    /**
     * @param sysUserIds 需要刷新会话快照的系统用户IDs
     */
    public record EventData(Collection<Long> sysUserIds) {

    }
}
