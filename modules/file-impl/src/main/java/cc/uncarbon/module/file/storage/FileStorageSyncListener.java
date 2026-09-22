package cc.uncarbon.module.file.storage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 文件存储点变更集群同步监听器
 *
 * <p>订阅 {@link FileStorageSyncBroadcaster#TOPIC_NAME}，收到其他实例的变更广播后
 * 全量重建本实例的底层存储平台；另定时比对集群版本号兜底，覆盖广播丢失
 * （如订阅重连窗口、广播时本实例尚未启动完成）的场景。</p>
 *
 * @see DynamicFileStorageRegistrar
 */
@RequiredArgsConstructor
@Component
@Slf4j
@EnableScheduling
public class FileStorageSyncListener {

    private static final String LOG_PREFIX = "[文件存储点集群同步监听器]";

    private final RedissonClient redissonClient;
    private final FileStorageSyncBroadcaster broadcaster;
    private final DynamicFileStorageRegistrar registrar;

    /**
     * 本实例已同步到的集群版本号
     */
    private volatile long syncedVersion;


    @PostConstruct
    public void subscribe() {
        try {
            RTopic topic = redissonClient.getTopic(FileStorageSyncBroadcaster.TOPIC_NAME);
            topic.addListener(String.class, (channel, message) -> onMessage(message));

            // 启动对齐：注册器 @PostConstruct 已全量载入 DB，以当前集群版本号为准
            // （与启动载入之间的毫秒级窗口若有变更漏收，属可接受边界）
            syncedVersion = broadcaster.currentVersion();
            log.info(LOG_PREFIX + " 已订阅集群广播 >> topic={}, 当前集群版本号={}",
                    FileStorageSyncBroadcaster.TOPIC_NAME, syncedVersion);
        } catch (Exception e) {
            // Redis 暂不可用不阻断启动，由定时兜底追平
            log.warn(LOG_PREFIX + " 订阅集群广播失败，等待定时兜底 >> ", e);
        }
    }

    /**
     * 定时兜底：比对集群版本号，不一致则全量重建；默认每 5 分钟
     */
    @Scheduled(initialDelay = 5, fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void reconcile() {
        try {
            long current = broadcaster.currentVersion();
            if (current != syncedVersion) {
                log.info(LOG_PREFIX + " 定时兜底检测到版本变化 >> {} -> {}", syncedVersion, current);
                registrar.reloadAll();
                // 重新读取，防止重建期间又有新变更被吞掉
                syncedVersion = broadcaster.currentVersion();
            }
        } catch (Exception e) {
            log.warn(LOG_PREFIX + " 定时兜底比对失败，下个周期重试 >> ", e);
        }
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private void onMessage(String message) {
        try {
            String[] parts = message.split("\\|");
            String originInstanceId = parts[0];
            String changeType = parts.length > 1 ? parts[1] : "UNKNOWN";
            long version = parts.length > 2 ? Long.parseLong(parts[2]) : Long.MIN_VALUE;

            if (version > syncedVersion) {
                syncedVersion = version;
            }
            // 本机已通过进程内事件同步，跳过
            if (broadcaster.getInstanceId().equals(originInstanceId)) {
                return;
            }
            log.info(LOG_PREFIX + " 收到其他实例广播 >> origin={}, type={}", originInstanceId, changeType);
            registrar.reloadAll();
        } catch (Exception e) {
            log.warn(LOG_PREFIX + " 处理广播消息失败 >> {}", message, e);
        }
    }
}
