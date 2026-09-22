package cc.uncarbon.module.file.storage;

import cc.uncarbon.module.file.storage.event.FileStorageChangedEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 文件存储点变更集群广播器
 *
 * <p>多JVM实例部署时，进程内事件无法触达其他实例；
 * 本组件通过 Redisson RTopic 广播轻量变更通知（不携带配置内容，接收方自行全量重读DB），
 * 并维护集群级版本号，供 {@link FileStorageSyncListener} 定时比对兜底，防漏收广播。</p>
 *
 * @see DynamicFileStorageRegistrar
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class FileStorageSyncBroadcaster {

    private static final String LOG_PREFIX = "[文件存储点集群同步广播器]";

    /**
     * 集群广播频道
     */
    public static final String TOPIC_NAME = "helium:file:storage:changed";

    /**
     * 集群级版本号键，每次存储点变更后自增
     */
    public static final String VERSION_KEY = "helium:file:storage:sync:version";

    private final RedissonClient redissonClient;

    /**
     * 本实例标识，接收方用于跳过自身（本机已通过进程内事件同步）
     */
    @Getter
    private final String instanceId = UUID.randomUUID().toString();

    /**
     * 当前集群版本号
     */
    public long currentVersion() {
        return redissonClient.getAtomicLong(VERSION_KEY).get();
    }

    /**
     * 自增集群版本号，并向所有实例广播变更通知；广播失败仅告警，不影响业务与本机已完成的同步
     */
    public void broadcast(FileStorageChangedEvent.ChangeType type) {
        try {
            RAtomicLong version = redissonClient.getAtomicLong(VERSION_KEY);
            long newVersion = version.incrementAndGet();

            RTopic topic = redissonClient.getTopic(TOPIC_NAME);
            topic.publish(instanceId + "|" + type + "|" + newVersion);
        } catch (Exception e) {
            log.warn(LOG_PREFIX + " 广播存储点变更失败，其他实例将由定时兜底对齐 >> type={}", type, e);
        }
    }
}
