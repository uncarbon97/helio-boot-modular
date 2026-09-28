package cc.uncarbon.module.adminapi.helper;

import cn.dev33.satoken.config.SaTokenConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Set;

/**
 * {@link TenantSwitchHelper} 租户在会话登记簿分支测试
 * （isSwitching 依赖 sa-token 会话，见集成测试）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantSwitchHelperTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private SetOperations<String, String> setOperations;

    private TenantSwitchHelper helper;


    @BeforeEach
    void setUp() {
        Mockito.doReturn(setOperations).when(stringRedisTemplate).opsForSet();
        SaTokenConfig config = new SaTokenConfig();
        config.setTimeout(1800);
        helper = new TenantSwitchHelper(config, stringRedisTemplate);
        helper.init();
    }

    @Test
    void registerSkipsNullArgs() {
        helper.register(null, 1L);
        helper.register(5L, null);

        Mockito.verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void registerAddsMemberWithExpiry() {
        Mockito.when(setOperations.add(Mockito.anyString(), Mockito.anyString())).thenReturn(1L);

        helper.register(5L, 9L);

        Mockito.verify(setOperations).add(Mockito.contains("tenant-logins"), Mockito.eq("9"));
        // TTL = sa-token timeout + 1 天冗余
        Mockito.verify(stringRedisTemplate).expire(Mockito.contains("tenant-logins"),
                Mockito.eq(Duration.ofSeconds(1800 + 24 * 60 * 60)));
    }

    @Test
    void unregisterRemovesMember() {
        helper.unregister(5L, 9L);

        Mockito.verify(setOperations).remove(Mockito.contains("tenant-logins"), Mockito.eq("9"));
    }

    @Test
    void listUserIdsBranches() {
        Assertions.assertEquals(Set.of(), helper.listUserIds(null));

        Mockito.when(setOperations.members(Mockito.anyString())).thenReturn(null);
        Assertions.assertEquals(Set.of(), helper.listUserIds(5L));

        Mockito.when(setOperations.members(Mockito.anyString())).thenReturn(Set.of("1", "2"));
        Assertions.assertEquals(Set.of(1L, 2L), helper.listUserIds(5L));
    }
}
