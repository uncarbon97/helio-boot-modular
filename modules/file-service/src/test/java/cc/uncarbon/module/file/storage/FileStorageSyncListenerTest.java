package cc.uncarbon.module.file.storage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

import java.lang.reflect.Field;

/**
 * {@link FileStorageSyncListener} 集群同步监听器分支测试
 * （订阅/自实例跳过/其他实例重建/畸形消息兜底/定时对齐）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileStorageSyncListenerTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private FileStorageSyncBroadcaster broadcaster;
    @Mock
    private DynamicFileStorageRegistrar registrar;
    @Mock
    private RTopic topic;

    @InjectMocks
    private FileStorageSyncListener listener;


    @Test
    void subscribeRegistersListenerAndAlignsVersion() throws Exception {
        MessageListener<String> ml = subscribeOk(5L);

        Assertions.assertNotNull(ml);
        Assertions.assertEquals(5L, syncedVersion());
    }

    @Test
    void subscribeToleratesRedisDown() throws Exception {
        Mockito.when(redissonClient.getTopic(FileStorageSyncBroadcaster.TOPIC_NAME))
                .thenThrow(new RuntimeException("redis down"));

        // Redis 暂不可用不阻断启动，由定时兜底追平
        Assertions.assertDoesNotThrow(listener::subscribe);
        Assertions.assertEquals(0L, syncedVersion());
    }

    @Test
    void messageFromSelfOnlyBumpsVersionWithoutReload() throws Exception {
        MessageListener<String> ml = subscribeOk(5L);
        Mockito.when(broadcaster.getInstanceId()).thenReturn("self");

        ml.onMessage("channel", "self|CREATE|6");

        // 本机已通过进程内事件同步，跳过重建
        Mockito.verify(registrar, Mockito.never()).reloadAll();
        Assertions.assertEquals(6L, syncedVersion());
    }

    @Test
    void messageFromOtherInstanceTriggersReload() throws Exception {
        MessageListener<String> ml = subscribeOk(5L);
        Mockito.when(broadcaster.getInstanceId()).thenReturn("self");

        ml.onMessage("channel", "other|UPDATE|7");

        Mockito.verify(registrar).reloadAll();
        Assertions.assertEquals(7L, syncedVersion());
    }

    @Test
    void messageWithoutPartsDefaultsAndStillReloads() throws Exception {
        MessageListener<String> ml = subscribeOk(0L);
        Mockito.when(broadcaster.getInstanceId()).thenReturn("self");

        // 缺失类型与版本号的畸形短消息按 UNKNOWN/MIN_VALUE 兜底，仍需重建
        ml.onMessage("channel", "other");

        Mockito.verify(registrar).reloadAll();
        Assertions.assertEquals(0L, syncedVersion());
    }

    @Test
    void messageWithBadVersionSwallowed() throws Exception {
        MessageListener<String> ml = subscribeOk(5L);
        Mockito.when(broadcaster.getInstanceId()).thenReturn("self");

        Assertions.assertDoesNotThrow(() -> ml.onMessage("channel", "other|CREATE|oops"));

        Mockito.verify(registrar, Mockito.never()).reloadAll();
        Assertions.assertEquals(5L, syncedVersion());
    }

    @Test
    void reconcileSkipsWhenVersionAligned() throws Exception {
        subscribeOk(5L);

        listener.reconcile();

        Mockito.verify(registrar, Mockito.never()).reloadAll();
        Assertions.assertEquals(5L, syncedVersion());
    }

    @Test
    void reconcileReloadsAndRealignsOnVersionChange() throws Exception {
        subscribeOk(5L);
        Mockito.when(broadcaster.currentVersion()).thenReturn(6L);

        listener.reconcile();

        Mockito.verify(registrar).reloadAll();
        Assertions.assertEquals(6L, syncedVersion());
    }

    @Test
    void reconcileToleratesCounterFailure() throws Exception {
        subscribeOk(5L);
        Mockito.when(broadcaster.currentVersion()).thenThrow(new RuntimeException("redis down"));

        Assertions.assertDoesNotThrow(listener::reconcile);

        Mockito.verify(registrar, Mockito.never()).reloadAll();
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    /**
     * 模拟订阅成功，返回被注册的广播消息监听器
     */
    @SuppressWarnings("unchecked")
    private MessageListener<String> subscribeOk(long version) {
        Mockito.when(redissonClient.getTopic(FileStorageSyncBroadcaster.TOPIC_NAME)).thenReturn(topic);
        Mockito.when(broadcaster.currentVersion()).thenReturn(version);

        listener.subscribe();

        ArgumentCaptor<MessageListener<String>> captor =
                ArgumentCaptor.forClass((Class<MessageListener<String>>) (Class<?>) MessageListener.class);
        Mockito.verify(topic).addListener(Mockito.eq(String.class), captor.capture());
        return captor.getValue();
    }

    /**
     * 反射读取已同步版本号，验证内部对齐状态
     */
    private long syncedVersion() throws Exception {
        Field field = FileStorageSyncListener.class.getDeclaredField("syncedVersion");
        field.setAccessible(true);
        return field.getLong(listener);
    }
}
