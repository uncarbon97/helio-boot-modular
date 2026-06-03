package cc.uncarbon.module.adminapi.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;

/**
 * 强制登出系统用户事件
 */
@Getter
public final class KickOutSysUsersEvent extends ApplicationEvent {

    private final transient EventData data;

    public KickOutSysUsersEvent(EventData data) {
        super(data);
        this.data = data;
    }

    @Getter
    @RequiredArgsConstructor
    public static final class EventData {

        /**
         * 需要被强制登出的系统用户IDs
         */
        private final Collection<Long> sysUserIds;

    }
}
