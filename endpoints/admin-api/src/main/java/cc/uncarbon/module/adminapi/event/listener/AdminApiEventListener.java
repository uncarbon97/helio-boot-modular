package cc.uncarbon.module.adminapi.event.listener;

import cc.uncarbon.module.adminapi.event.KickOutSysUsersEvent;
import cc.uncarbon.module.adminapi.event.RefreshRolePermissionCacheEvent;
import cc.uncarbon.module.adminapi.helper.RolePermissionCacheHelper;
import cc.uncarbon.module.commons.satoken.StpKit;
import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * admin-api 模块事件监听器
 */
@Component
@RequiredArgsConstructor
public class AdminApiEventListener {

    /**
     * 虚拟线程执行器
     */
    private final AsyncTaskExecutor taskExecutor;
    private final RolePermissionCacheHelper rolePermissionCacheHelper;


    @EventListener(value = KickOutSysUsersEvent.class)
    public void handle(KickOutSysUsersEvent event) {
        Collection<Long> sysUserIds = event.getData().sysUserIds();
        if (CollUtil.isNotEmpty(sysUserIds)) {
            // 异步强制登出；同一时间大量登出，会操作大量Redis键，可能存在缓存雪崩的风险
            taskExecutor.execute(() -> sysUserIds.forEach(StpKit.ADMIN::kickout));
        }
    }

    @EventListener(value = RefreshRolePermissionCacheEvent.class)
    public void handle(RefreshRolePermissionCacheEvent event) {
        Collection<Long> roleIds = event.getData().roleIds();
        if (CollUtil.isNotEmpty(roleIds)) {
            rolePermissionCacheHelper.delayedDoubleDelete(roleIds);
        }
    }
}
