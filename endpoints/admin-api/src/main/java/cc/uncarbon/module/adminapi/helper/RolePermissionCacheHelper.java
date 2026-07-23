package cc.uncarbon.module.adminapi.helper;

import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.module.sys.service.SysMenuService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 将角色对应权限，缓存至 Redis
 * 参考文章: <a href="https://sa-token.cc/doc.html#/fun/jur-cache">...</a>
 *
 * @author Uncarbon
 */
@Component
@RequiredArgsConstructor
public class RolePermissionCacheHelper {

    /**
     * 角色权限缓存键前缀
     */
    private static final String CACHE_KEY_ROLE_PERMISSIONS = "Authorization:role-perm:%s";

    /**
     * 延时双删毫秒数
     */
    private static final long DELAYED_DELETE_MILLIS = 1000L;

    /**
     * 角色权限缓存 TTL 秒数
     * 默认为 6h
     */
    private static final long CACHE_TTL = 6 * 60 * 60;


    private final RedisTemplate<String, Collection<String>> stringSetRedisTemplate;
    private final SysMenuService sysMenuService;

    /**
     * 虚拟线程执行器
     */
    private final TaskExecutor taskExecutor;


    /**
     * 从缓存中取得当前用户拥有的所有权限名集合
     *
     * @return List<String>
     */
    public List<String> getCurrentUserPermissions() {
        var context = UserContextHolder.getContext();
        if (context == null) {
            return List.of();
        }

        var roleIds = context.getRoleIds();
        if (CollUtil.isEmpty(roleIds)) {
            return List.of();
        }

        List<Long> roleIdList = new ArrayList<>(roleIds);
        // 批量查询缓存
        List<String> cacheKeys = roleIdList.stream().map(this::determineCacheKey).toList();
        List<Collection<String>> cacheValues = stringSetRedisTemplate.opsForValue().multiGet(cacheKeys);
        if (cacheValues == null || cacheValues.size() != roleIdList.size()) {
            cacheValues = new ArrayList<>(Collections.nCopies(roleIdList.size(), null));
        }

        // 找出缓存中不存在的角色 ID
        Set<Long> missingRoleIds = new HashSet<>();
        for (int i = 0; i < roleIdList.size(); i++) {
            if (cacheValues.get(i) == null) {
                missingRoleIds.add(roleIdList.get(i));
            }
        }

        // 从 DB 加载缓存中不存在的角色 ID 的权限
        Map<Long, Set<String>> loadedPermissions = Map.of();
        if (CollUtil.isNotEmpty(missingRoleIds)) {
            loadedPermissions = sysMenuService.getPermissionsByRole(missingRoleIds);
            putCache(loadedPermissions);
        }

        // 合并
        List<String> permissions = new ArrayList<>();
        Set<String> emptySet = Set.of();
        for (int i = 0; i < roleIdList.size(); i++) {
            Collection<String> cached = cacheValues.get(i);
            permissions.addAll(cached != null
                    ? cached
                    : loadedPermissions.getOrDefault(roleIdList.get(i), emptySet));
        }
        return permissions;
    }

    /**
     * 延时双删角色权限缓存
     */
    public void delayedDoubleDelete(Collection<Long> roleIds) {
        if (CollUtil.isNotEmpty(roleIds)) {
            Set<Long> roleIdSet = Set.copyOf(roleIds);
            deleteCache(roleIdSet);
            CompletableFuture.runAsync(
                    () -> deleteCache(roleIdSet),
                    CompletableFuture.delayedExecutor(DELAYED_DELETE_MILLIS, TimeUnit.MILLISECONDS, taskExecutor)
            );
        }
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 将角色权限缓存至 Redis
     */
    private void putCache(Map<Long, Set<String>> mapping) {
        mapping.forEach((roleId, permissions) ->
                stringSetRedisTemplate.opsForValue().set(determineCacheKey(roleId), permissions,
                        // 随机增加几秒，避免缓存雪崩
                        Expiration.seconds(CACHE_TTL + RandomUtil.randomLong(0, 10))
                ));
    }

    /**
     * 删除角色权限缓存
     */
    private void deleteCache(Collection<Long> roleIds) {
        List<String> cacheKeys = roleIds.stream().map(this::determineCacheKey).toList();
        stringSetRedisTemplate.delete(cacheKeys);
    }

    /**
     * 确定角色权限缓存键
     */
    private String determineCacheKey(Long roleId) {
        return String.format(CACHE_KEY_ROLE_PERMISSIONS, roleId);
    }
}
