package cc.uncarbon.module.adminapi.event;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * admin-api 事件对象薄测（事件数据承载 + ApplicationEvent 契约）
 */
class AdminApiEventsTest {

    @Test
    void kickOutSysUsersEventCarriesData() {
        var data = new KickOutSysUsersEvent.EventData(List.of(1L, 2L));
        var event = new KickOutSysUsersEvent(data);

        Assertions.assertEquals(List.of(1L, 2L), event.getData().sysUserIds());
        Assertions.assertSame(data, event.getSource());
    }

    @Test
    void exitTenantSwitchEventCarriesData() {
        var data = new ExitTenantSwitchEvent.EventData(5L, List.of(9L));
        var event = new ExitTenantSwitchEvent(data);

        Assertions.assertEquals(5L, event.getData().tenantId());
        Assertions.assertEquals(List.of(9L), event.getData().sysUserIds());
        Assertions.assertSame(data, event.getSource());
    }

    @Test
    void refreshRolePermissionCacheEventCarriesData() {
        // 租户环境：带 tenantId
        var withTenant = new RefreshRolePermissionCacheEvent(
                new RefreshRolePermissionCacheEvent.EventData(List.of(5L), 7L));
        Assertions.assertEquals(List.of(5L), withTenant.getData().roleIds());
        Assertions.assertEquals(7L, withTenant.getData().tenantId());

        // 非租户环境：tenantId 传 null
        var withoutTenant = new RefreshRolePermissionCacheEvent(
                new RefreshRolePermissionCacheEvent.EventData(List.of(5L), null));
        Assertions.assertNull(withoutTenant.getData().tenantId());
    }

    @Test
    void refreshSysUserSessionEventCarriesData() {
        var data = new RefreshSysUserSessionEvent.EventData(List.of(1L));
        var event = new RefreshSysUserSessionEvent(data);

        Assertions.assertEquals(List.of(1L), event.getData().sysUserIds());
        Assertions.assertSame(data, event.getSource());
    }
}
