package cc.uncarbon.module.adminapi.helper;

import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.module.sys.service.SysMenuService;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.types.Expiration;

import java.util.*;

import static java.lang.ScopedValue.where;

/**
 * {@link RolePermissionCacheHelper} 角色权限缓存读写分支测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RolePermissionCacheHelperTest {

    @Mock
    private TaskExecutor taskExecutor;
    @Mock
    private RedisTemplate<String, Collection<String>> stringSetRedisTemplate;
    @Mock
    private ValueOperations<String, Collection<String>> valueOperations;
    @Mock
    private SysMenuService sysMenuService;

    private RolePermissionCacheHelper helper;

    @BeforeEach
    void setUp() {
        Mockito.doReturn(valueOperations).when(stringSetRedisTemplate).opsForValue();
        helper = new RolePermissionCacheHelper(taskExecutor, stringSetRedisTemplate, sysMenuService);
    }


    /*
    ----------------------------------------------------------------
                        getCurrentUserPermissions
    ----------------------------------------------------------------
     */

    @Test
    void returnsEmptyWhenUserContextMissing() {
        Assertions.assertEquals(List.of(), helper.getCurrentUserPermissions());

        Mockito.verifyNoInteractions(stringSetRedisTemplate, sysMenuService);
    }

    @Test
    void returnsEmptyWhenRoleIdsEmpty() {
        bindRoles(List.of(), () ->
                Assertions.assertEquals(List.of(), helper.getCurrentUserPermissions()));

        Mockito.verifyNoInteractions(stringSetRedisTemplate, sysMenuService);
    }

    @Test
    void loadsAllFromDbWhenCacheTotallyMissing() {
        // multiGet 返回 null → 全部角色视为未缓存
        Mockito.when(valueOperations.multiGet(Mockito.anyList())).thenReturn(null);
        Mockito.when(sysMenuService.getPermissionsByRole(Mockito.anyCollection()))
                .thenReturn(Map.of(1L, Set.of("p1"), 2L, Set.of("p2")));

        bindRoles(List.of(1L, 2L), () ->
                Assertions.assertEquals(List.of("p1", "p2"), helper.getCurrentUserPermissions()));

        // 未命中角色回填缓存
        Mockito.verify(valueOperations).set(
                Mockito.eq("admin:role-perm:1"), Mockito.eq(Set.of("p1")), Mockito.any(Expiration.class));
        Mockito.verify(valueOperations).set(
                Mockito.eq("admin:role-perm:2"), Mockito.eq(Set.of("p2")), Mockito.any(Expiration.class));
    }

    @Test
    void loadsOnlyMissingRolesWhenCachePartiallyHit() {
        Mockito.when(valueOperations.multiGet(Mockito.anyList()))
                .thenReturn(Arrays.asList(null, (Collection<String>) Set.of("p2")));
        Mockito.when(sysMenuService.getPermissionsByRole(Mockito.anyCollection()))
                .thenReturn(Map.of(1L, Set.of("p1")));

        bindRoles(List.of(1L, 2L), () ->
                // 合并：角色1 取 DB 回填值，角色2 取缓存值
                Assertions.assertEquals(List.of("p1", "p2"), helper.getCurrentUserPermissions()));

        Mockito.verify(valueOperations).set(
                Mockito.eq("admin:role-perm:1"), Mockito.eq(Set.of("p1")), Mockito.any(Expiration.class));
        Mockito.verify(valueOperations, Mockito.never()).set(
                Mockito.eq("admin:role-perm:2"), Mockito.any(), Mockito.any(Expiration.class));
        // 仅未命中的角色1 触发 DB 查询
        Mockito.verify(sysMenuService)
                .getPermissionsByRole(Mockito.argThat(ids -> Set.of(1L).equals(ids)));
    }

    @Test
    void reloadsAllWhenMultiGetSizeMismatch() {
        // multiGet 返回条数与角色数不符 → 视为全部未缓存
        Mockito.when(valueOperations.multiGet(Mockito.anyList()))
                .thenReturn(List.of(Set.of("p2")));
        Mockito.when(sysMenuService.getPermissionsByRole(Mockito.anyCollection()))
                .thenReturn(Map.of(1L, Set.of("p1"), 2L, Set.of("p2b")));

        bindRoles(List.of(1L, 2L), () ->
                Assertions.assertEquals(List.of("p1", "p2b"), helper.getCurrentUserPermissions()));
    }

    @Test
    void mergesEmptyCollectionsGracefully() {
        // 缓存命中空集合 + 未命中角色 DB 亦无权限 → 空结果但不报错
        Mockito.when(valueOperations.multiGet(Mockito.anyList()))
                .thenReturn(Arrays.asList(Set.of(), null));
        Mockito.when(sysMenuService.getPermissionsByRole(Mockito.anyCollection()))
                .thenReturn(Map.of());

        bindRoles(List.of(1L, 2L), () ->
                Assertions.assertEquals(List.of(), helper.getCurrentUserPermissions()));
    }


    /*
    ----------------------------------------------------------------
                        delayedDoubleDelete
    ----------------------------------------------------------------
     */

    @Test
    void delayedDoubleDeleteDeletesTwiceWithDelay() throws InterruptedException {
        Mockito.doAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(0)).run();
                    return null;
                })
                .when(taskExecutor).execute(Mockito.any(Runnable.class));

        helper.delayedDoubleDelete(List.of(5L, 6L));

        // 第一次立即删除（同步）；key 顺序取决于集合迭代序，按集合比较
        Mockito.verify(stringSetRedisTemplate)
                .delete(Mockito.argThat((List<String> keys) ->
                        new HashSet<>(keys).equals(Set.of("admin:role-perm:5", "admin:role-perm:6"))));

        // 第二次延时 1 秒后删除
        Thread.sleep(1500);

        Mockito.verify(stringSetRedisTemplate, Mockito.times(2))
                .delete(Mockito.argThat((List<String> keys) ->
                        new HashSet<>(keys).equals(Set.of("admin:role-perm:5", "admin:role-perm:6"))));
    }

    @Test
    void delayedDoubleDeleteSkipsEmptyRoleIds() {
        helper.delayedDoubleDelete(List.of());

        Mockito.verify(stringSetRedisTemplate, Mockito.never()).delete(Mockito.anyList());
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    private void bindRoles(List<Long> roleIds, Runnable op) {
        UserContext user = new SimpleUserContext().setUserId(9L).setRoleIds(roleIds);
        where(UserContextHolder.scoped(), user).run(op);
    }
}
