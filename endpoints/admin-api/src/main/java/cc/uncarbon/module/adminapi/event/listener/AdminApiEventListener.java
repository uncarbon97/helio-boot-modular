package cc.uncarbon.module.adminapi.event.listener;

import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.module.adminapi.event.KickOutSysUsersEvent;
import cc.uncarbon.module.adminapi.event.RefreshRolePermissionCacheEvent;
import cc.uncarbon.module.adminapi.event.RefreshSysUserSessionEvent;
import cc.uncarbon.module.adminapi.helper.RolePermissionCacheHelper;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.sys.service.AdminLoginService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * admin-api 模块事件监听器
 */
@Component
@RequiredArgsConstructor
public class AdminApiEventListener {

    /**
     * 强制登出分批大小
     */
    private static final int KICK_OUT_BATCH_SIZE = 100;
    /**
     * 强制登出批次间隔（毫秒）
     */
    private static final long KICK_OUT_INTERVAL_MILLIS = 500;

    /**
     * 虚拟线程执行器
     */
    private final AsyncTaskExecutor taskExecutor;
    private final RolePermissionCacheHelper rolePermissionCacheHelper;
    private final AdminLoginService adminLoginService;

    @EventListener(value = KickOutSysUsersEvent.class)
    public void handle(KickOutSysUsersEvent event) {
        Collection<Long> sysUserIds = event.getData().sysUserIds();
        if (CollUtil.isNotEmpty(sysUserIds)) {
            // 异步强制登出；分批+间隔执行，避免同一时间大量操作Redis键，降低缓存雪崩风险
            taskExecutor.execute(() -> {
                List<List<Long>> batches = ListUtil.partition(new ArrayList<>(sysUserIds), KICK_OUT_BATCH_SIZE);
                for (int i = 0; i < batches.size(); i++) {
                    batches.get(i).forEach(StpKit.ADMIN::kickout);
                    if (i < batches.size() - 1) {
                        ThreadUtil.safeSleep(KICK_OUT_INTERVAL_MILLIS);
                    }
                }
            });
        }
    }

    @EventListener(value = RefreshRolePermissionCacheEvent.class)
    public void handle(RefreshRolePermissionCacheEvent event) {
        Collection<Long> roleIds = event.getData().roleIds();
        if (CollUtil.isNotEmpty(roleIds)) {
            rolePermissionCacheHelper.delayedDoubleDelete(roleIds);
        }
    }

    @EventListener(value = RefreshSysUserSessionEvent.class)
    public void handle(RefreshSysUserSessionEvent event) {
        Collection<Long> sysUserIds = event.getData().sysUserIds();
        if (CollUtil.isEmpty(sysUserIds)) {
            return;
        }
        // 异步原位刷新会话快照；分批+间隔执行，避免同一时间大量操作Redis键
        taskExecutor.execute(() -> {
            List<List<Long>> batches = ListUtil.partition(new ArrayList<>(sysUserIds), KICK_OUT_BATCH_SIZE);
            for (int i = 0; i < batches.size(); i++) {
                batches.get(i).forEach(this::refreshOneUserSession);
                if (i < batches.size() - 1) {
                    ThreadUtil.safeSleep(KICK_OUT_INTERVAL_MILLIS);
                }
            }
        });
    }

    /**
     * 原位刷新单个用户的会话快照，token 保持不变，下一次请求即新权限
     */
    private void refreshOneUserSession(Long userId) {
        var session = StpKit.ADMIN.getSessionByLoginId(userId, false);
        if (session == null) {
            // 用户不在线，无需刷新
            return;
        }
        var freshContext = adminLoginService.buildSessionUserContext(userId);
        if (freshContext == null) {
            // 用户已不存在或被禁用，兜底强制登出
            StpKit.ADMIN.kickout(userId);
            return;
        }
        session.set(UserContext.CAMEL_NAME, freshContext);
    }
}
