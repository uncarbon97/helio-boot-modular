package cc.uncarbon.module.adminapi.event.listener;

import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.module.adminapi.event.ExitTenantSwitchEvent;
import cc.uncarbon.module.adminapi.event.KickOutSysUsersEvent;
import cc.uncarbon.module.adminapi.event.RefreshRolePermissionCacheEvent;
import cc.uncarbon.module.adminapi.event.RefreshSysUserSessionEvent;
import cc.uncarbon.module.adminapi.helper.RolePermissionCacheHelper;
import cc.uncarbon.module.adminapi.helper.TenantSwitchHelper;
import cc.uncarbon.module.adminapi.model.internal.TenantSwitchInfo;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.service.AdminLoginService;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import cn.dev33.satoken.session.SaSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.task.TaskExecutor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

/**
 * {@link AdminApiEventListener} 事件监听分支测试
 * <p>
 * sa-token 会话通过 {@link SaTokenContextMockUtil} Mock 上下文 + 内存版 Dao 模拟；
 * 异步任务以同步执行器代替，保证断言可在当前线程生效
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminApiEventListenerTest {

    private static final Long USER_ID = 9L;

    @Mock
    private TaskExecutor taskExecutor;
    @Mock
    private RolePermissionCacheHelper rolePermissionCacheHelper;
    @Mock
    private AdminLoginService adminLoginService;
    @Mock
    private TenantFacade tenantFacade;
    @Mock
    private TenantUserRoleFacade tenantUserRoleFacade;
    @Mock
    private TenantSwitchHelper tenantSwitchHelper;

    private AdminApiEventListener listener;

    @BeforeEach
    void setUp() {
        // 同步执行器：任务在提交线程内联执行
        Mockito.doAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(0)).run();
                    return null;
                })
                .when(taskExecutor).execute(Mockito.any(Runnable.class));
        listener = new AdminApiEventListener(taskExecutor, rolePermissionCacheHelper,
                adminLoginService, tenantFacade, tenantUserRoleFacade, tenantSwitchHelper);
    }

    @AfterEach
    void tearDown() {
        SaTokenContextMockUtil.clearContext();
        StpKit.ADMIN.logout(USER_ID);
    }


    /*
    ----------------------------------------------------------------
                        强制登出 KickOutSysUsersEvent
    ----------------------------------------------------------------
     */

    @Test
    void kickOutSkipsEmptyUserIds() {
        listener.handle(new KickOutSysUsersEvent(new KickOutSysUsersEvent.EventData(List.of())));

        Mockito.verify(taskExecutor, Mockito.never()).execute(Mockito.any(Runnable.class));
    }

    @Test
    void kickOutInvalidatesOnlineToken() {
        SaTokenContextMockUtil.setMockContext(() -> {
            StpKit.ADMIN.login(USER_ID);
            Assertions.assertNotNull(StpKit.ADMIN.getTokenValueByLoginId(USER_ID));

            listener.handle(new KickOutSysUsersEvent(new KickOutSysUsersEvent.EventData(List.of(USER_ID))));

            // token 被标记踢出，无法再反查
            Assertions.assertNull(StpKit.ADMIN.getTokenValueByLoginId(USER_ID));
        });
    }

    @Test
    void kickOutPartitionsLargeUserBatch() {
        // 101 人 > 单批 100，覆盖分批与批次间隔逻辑
        List<Long> ids = LongStream.rangeClosed(1, 101).boxed().toList();

        Assertions.assertDoesNotThrow(() ->
                listener.handle(new KickOutSysUsersEvent(new KickOutSysUsersEvent.EventData(ids))));

        Mockito.verify(taskExecutor).execute(Mockito.any(Runnable.class));
    }


    /*
    ----------------------------------------------------------------
                        强制退出租户切换 ExitTenantSwitchEvent
    ----------------------------------------------------------------
     */

    @Test
    void exitSwitchSkipsEmptyUserIds() {
        listener.handle(new ExitTenantSwitchEvent(new ExitTenantSwitchEvent.EventData(5L, List.of())));

        Mockito.verify(taskExecutor, Mockito.never()).execute(Mockito.any(Runnable.class));
    }

    @Test
    void exitSwitchUnregistersWhenSessionAbsent() {
        // 用户不在线（残留登记），清理登记即可
        Assertions.assertNull(StpKit.ADMIN.getSessionByLoginId(USER_ID, false));

        listener.handle(new ExitTenantSwitchEvent(new ExitTenantSwitchEvent.EventData(5L, List.of(USER_ID))));

        Mockito.verify(tenantSwitchHelper).unregister(5L, USER_ID);
    }

    @Test
    void exitSwitchUnregistersWhenNotSwitching() {
        // 在线但未处于切换态
        SaSession session = StpKit.ADMIN.getSessionByLoginId(USER_ID, true);
        Assertions.assertNotNull(session);

        listener.handle(new ExitTenantSwitchEvent(new ExitTenantSwitchEvent.EventData(5L, List.of(USER_ID))));

        Mockito.verify(tenantSwitchHelper).unregister(5L, USER_ID);
        Mockito.verifyNoInteractions(adminLoginService, tenantFacade);
    }

    @Test
    void exitSwitchRestoresOriginalTenantContext() {
        // 切换切入后原上下文完整：直接退回切换前视角（活动租户 5 → 原租户 0）
        SaSession session = StpKit.ADMIN.getSessionByLoginId(USER_ID, true);
        TenantContext original = new SimpleTenantContext(0L, "平台", "platform");
        session.set(TenantContext.CAMEL_NAME, new SimpleTenantContext(5L, "租户五", "t5"));
        session.set(TenantSwitchInfo.CAMEL_NAME, new TenantSwitchInfo()
                .setOriginalTenantContext(original).setSwitchedAt(Instant.now()));
        UserContext fresh = new SimpleUserContext().setUserId(USER_ID);
        Mockito.when(adminLoginService.buildSessionUserContext(Mockito.eq(USER_ID), Mockito.any()))
                .thenReturn(fresh);

        listener.handle(new ExitTenantSwitchEvent(new ExitTenantSwitchEvent.EventData(5L, List.of(USER_ID))));

        Assertions.assertSame(original, session.get(TenantContext.CAMEL_NAME));
        Assertions.assertSame(fresh, session.get(UserContext.CAMEL_NAME));
        Assertions.assertNull(session.get(TenantSwitchInfo.CAMEL_NAME));
        Mockito.verify(tenantSwitchHelper).unregister(5L, USER_ID);
        Mockito.verify(tenantSwitchHelper).register(0L, USER_ID);
        Mockito.verify(tenantUserRoleFacade).rememberActiveTenant(USER_ID, 0L);
        // 原上下文存在时不查默认租户
        Mockito.verifyNoInteractions(tenantFacade);
    }

    @Test
    void exitSwitchResolvesDefaultTenantWhenOriginalMissing() {
        // 切换前为平台/个人视角（originalTenantContext=null）→ 回退登录默认租户
        SaSession session = StpKit.ADMIN.getSessionByLoginId(USER_ID, true);
        session.set(TenantSwitchInfo.CAMEL_NAME, new TenantSwitchInfo().setSwitchedAt(Instant.now()));
        TenantContext defaultTenant = new SimpleTenantContext(7L, "租户七", "t7");
        Mockito.when(tenantFacade.resolveDefaultTenant(USER_ID)).thenReturn(defaultTenant);
        UserContext fresh = new SimpleUserContext().setUserId(USER_ID);
        Mockito.when(adminLoginService.buildSessionUserContext(USER_ID, defaultTenant)).thenReturn(fresh);

        listener.handle(new ExitTenantSwitchEvent(new ExitTenantSwitchEvent.EventData(5L, List.of(USER_ID))));

        Assertions.assertSame(defaultTenant, session.get(TenantContext.CAMEL_NAME));
        Mockito.verify(tenantSwitchHelper).register(7L, USER_ID);
        Mockito.verify(tenantUserRoleFacade).rememberActiveTenant(USER_ID, 7L);
    }

    @Test
    void exitSwitchKicksOutWhenRebuildFails() {
        // 用户已不存在/被禁用：兜底强制登出
        SaSession session = StpKit.ADMIN.getSessionByLoginId(USER_ID, true);
        session.set(TenantSwitchInfo.CAMEL_NAME, new TenantSwitchInfo()
                .setOriginalTenantContext(new SimpleTenantContext(0L, "平台", "platform"))
                .setSwitchedAt(Instant.now()));
        Mockito.when(adminLoginService.buildSessionUserContext(Mockito.eq(USER_ID), Mockito.any()))
                .thenReturn(null);

        listener.handle(new ExitTenantSwitchEvent(new ExitTenantSwitchEvent.EventData(5L, List.of(USER_ID))));

        Mockito.verify(tenantSwitchHelper).unregister(5L, USER_ID);
        Assertions.assertNull(session.get(TenantContext.CAMEL_NAME));
        Mockito.verify(tenantSwitchHelper, Mockito.never()).register(Mockito.any(), Mockito.any());
    }


    /*
    ----------------------------------------------------------------
                        刷新角色权限缓存 RefreshRolePermissionCacheEvent
    ----------------------------------------------------------------
     */

    @Test
    void refreshRoleCacheDelegatesNonEmptyRoleIds() {
        listener.handle(new RefreshRolePermissionCacheEvent(
                new RefreshRolePermissionCacheEvent.EventData(List.of(5L), null)));

        Mockito.verify(rolePermissionCacheHelper).delayedDoubleDelete(List.of(5L));
    }

    @Test
    void refreshRoleCacheSkipsEmptyRoleIds() {
        listener.handle(new RefreshRolePermissionCacheEvent(
                new RefreshRolePermissionCacheEvent.EventData(List.of(), null)));

        Mockito.verify(rolePermissionCacheHelper, Mockito.never())
                .delayedDoubleDelete(Mockito.anyCollection());
    }


    /*
    ----------------------------------------------------------------
                        原位刷新会话 RefreshSysUserSessionEvent
    ----------------------------------------------------------------
     */

    @Test
    void refreshSessionSkipsEmptyUserIds() {
        listener.handle(new RefreshSysUserSessionEvent(new RefreshSysUserSessionEvent.EventData(List.of())));

        Mockito.verify(taskExecutor, Mockito.never()).execute(Mockito.any(Runnable.class));
    }

    @Test
    void refreshSessionSkipsOfflineUser() {
        Assertions.assertNull(StpKit.ADMIN.getSessionByLoginId(USER_ID, false));

        listener.handle(new RefreshSysUserSessionEvent(new RefreshSysUserSessionEvent.EventData(List.of(USER_ID))));

        Mockito.verifyNoInteractions(adminLoginService);
    }

    @Test
    void refreshSessionRebuildsWithoutTenantContext() {
        // 会话无生效租户 → 按归属租户（一参版本）重建
        SaSession session = StpKit.ADMIN.getSessionByLoginId(USER_ID, true);
        UserContext fresh = new SimpleUserContext().setUserId(USER_ID);
        Mockito.when(adminLoginService.buildSessionUserContext(USER_ID)).thenReturn(fresh);

        listener.handle(new RefreshSysUserSessionEvent(new RefreshSysUserSessionEvent.EventData(List.of(USER_ID))));

        Assertions.assertSame(fresh, session.get(UserContext.CAMEL_NAME));
        Mockito.verify(adminLoginService, Mockito.never())
                .buildSessionUserContext(Mockito.anyLong(), Mockito.any());
    }

    @Test
    void refreshSessionRebuildsWithActiveTenantContext() {
        // 处于切换租户视角（会话带 TenantContext）→ 按当前生效租户重建
        SaSession session = StpKit.ADMIN.getSessionByLoginId(USER_ID, true);
        TenantContext active = new SimpleTenantContext(5L, "租户五", "t5");
        session.set(TenantContext.CAMEL_NAME, active);
        UserContext fresh = new SimpleUserContext().setUserId(USER_ID);
        Mockito.when(adminLoginService.buildSessionUserContext(USER_ID, active)).thenReturn(fresh);

        listener.handle(new RefreshSysUserSessionEvent(new RefreshSysUserSessionEvent.EventData(List.of(USER_ID))));

        Assertions.assertSame(fresh, session.get(UserContext.CAMEL_NAME));
    }

    @Test
    void refreshSessionKicksOutWhenRebuildFails() {
        SaTokenContextMockUtil.setMockContext(() -> {
            StpKit.ADMIN.login(USER_ID);
            Mockito.when(adminLoginService.buildSessionUserContext(USER_ID)).thenReturn(null);

            listener.handle(new RefreshSysUserSessionEvent(new RefreshSysUserSessionEvent.EventData(List.of(USER_ID))));

            // 用户已不存在或被禁用 → 兜底强制登出
            Assertions.assertNull(StpKit.ADMIN.getTokenValueByLoginId(USER_ID));
        });
    }

    @Test
    void refreshSessionPartitionsLargeUserBatch() {
        List<Long> ids = new ArrayList<>(LongStream.rangeClosed(1, 101).boxed().toList());

        Assertions.assertDoesNotThrow(() ->
                listener.handle(new RefreshSysUserSessionEvent(new RefreshSysUserSessionEvent.EventData(ids))));

        Mockito.verify(taskExecutor).execute(Mockito.any(Runnable.class));
    }
}
