package cc.uncarbon.module.file.storage;

import cc.uncarbon.module.file.storage.event.FileStorageChangedEvent;
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
import org.redisson.api.RAtomicLong;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

/**
 * {@link FileStorageSyncBroadcaster} 集群广播器分支测试
 * （版本号读取/自增广播/广播失败仅告警）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileStorageSyncBroadcasterTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RAtomicLong versionCounter;
    @Mock
    private RTopic topic;

    @InjectMocks
    private FileStorageSyncBroadcaster broadcaster;


    @Test
    void currentVersionReadsClusterCounter() {
        Mockito.when(redissonClient.getAtomicLong(FileStorageSyncBroadcaster.VERSION_KEY))
                .thenReturn(versionCounter);
        Mockito.when(versionCounter.get()).thenReturn(42L);

        Assertions.assertEquals(42L, broadcaster.currentVersion());
    }

    @Test
    void broadcastPublishesVersionedNotice() {
        Mockito.when(redissonClient.getAtomicLong(FileStorageSyncBroadcaster.VERSION_KEY))
                .thenReturn(versionCounter);
        Mockito.when(versionCounter.incrementAndGet()).thenReturn(7L);
        Mockito.when(redissonClient.getTopic(FileStorageSyncBroadcaster.TOPIC_NAME)).thenReturn(topic);

        broadcaster.broadcast(FileStorageChangedEvent.ChangeType.CREATE);

        // 广播消息格式：实例ID|变更类型|新版本号
        ArgumentCaptor<Object> msgCaptor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(topic).publish(msgCaptor.capture());
        Assertions.assertEquals(broadcaster.getInstanceId() + "|CREATE|7", msgCaptor.getValue());
        Assertions.assertNotNull(broadcaster.getInstanceId());
    }

    @Test
    void broadcastFailureOnlyWarnsWithoutPublishing() {
        Mockito.when(redissonClient.getAtomicLong(FileStorageSyncBroadcaster.VERSION_KEY))
                .thenReturn(versionCounter);
        Mockito.when(versionCounter.incrementAndGet()).thenThrow(new RuntimeException("redis down"));

        // 广播失败不影响本机已完成的同步，仅告警
        Assertions.assertDoesNotThrow(() -> broadcaster.broadcast(FileStorageChangedEvent.ChangeType.DELETE));

        Mockito.verify(redissonClient, Mockito.never()).getTopic(Mockito.anyString());
    }
}
