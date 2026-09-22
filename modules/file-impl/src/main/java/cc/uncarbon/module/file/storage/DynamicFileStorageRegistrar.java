package cc.uncarbon.module.file.storage;

import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.file.dal.entity.FileStorageEntity;
import cc.uncarbon.module.file.dal.mapper.FileStorageMapper;
import cc.uncarbon.module.file.storage.event.FileStorageChangedEvent;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.FileStorageServiceBuilder;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 动态文件存储点注册器
 *
 * <p>应用启动时、以及存储点记录发生变化（事务提交后）时，
 * 将 {@link FileStorageEntity} 表中的存储点全量同步注册到底层 {@link FileStorageService}，
 * 使其可以动态增减，无需重启应用。</p>
 *
 * <p>完整平台名约定为 {@code tenantId_code}（租户ID为空时退化为纯 code），
 * 与上传、下载路径保持一致；YAML 静态配置的平台不受影响，与DB平台共存。</p>
 *
 * <p>多JVM实例部署时，配合 {@link FileStorageSyncBroadcaster} 集群广播 +
 * {@link FileStorageSyncListener} 订阅与定时兜底，保证各实例内存态一致。</p>
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class DynamicFileStorageRegistrar {

    private static final String LOG_PREFIX = "[动态文件存储点注册器]";
    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    private final FileStorageMapper fileStorageMapper;
    private final FileStorageService fileStorageService;
    private final FileStorageSyncBroadcaster fileStorageSyncBroadcaster;

    /**
     * 当前已注册到底层的DB存储点完整平台名，用于注销时区分DB平台与YAML平台
     */
    private final Set<String> registeredPlatforms = ConcurrentHashMap.newKeySet();


    @PostConstruct
    public void init() {
        // 全量注册DB存储点
        reloadAll();
    }

    /**
     * 格式化出完整平台名
     *
     * @param tenantId    租户ID，非租户环境传null
     * @param storageCode 存储点编码
     */
    public static String formatFullPlatform(Long tenantId, String storageCode) {
        if (tenantId == null) {
            return storageCode;
        }
        return tenantId + "_" + storageCode;
    }

    /**
     * 存储点记录发生变化，事务提交后再同步底层，避免注册了未落库的数据
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onFileStorageChanged(FileStorageChangedEvent event) {
        log.info(LOG_PREFIX + " 存储点记录发生变化 >> type={}", event.getData().type());
        reloadAll();
        // 多JVM实例部署时，向其他实例广播变更（本机已同步，接收方自行重读DB）
        fileStorageSyncBroadcaster.broadcast(event.getData().type());
    }

    /**
     * 全量重建底层DB存储平台：先注销已注册的，再按最新DB记录逐条注册；单条失败跳过，不阻断其余
     */
    public synchronized void reloadAll() {
        List<FileStorageEntity> entityList;
        try {
            // 需要注册所有租户的存储点，忽略行级租户隔离
            entityList = TenantContextHolder.callIgnored(() -> fileStorageMapper.selectList(null));
        } catch (Exception e) {
            log.error(LOG_PREFIX + " 读取存储点列表失败 >> ", e);
            return;
        }

        CopyOnWriteArrayList<FileStorage> fileStorageList = fileStorageService.getFileStorageList();
        unregisterAll(fileStorageList);

        for (FileStorageEntity entity : entityList) {
            try {
                register(entity, fileStorageList);
            } catch (Exception e) {
                log.error(LOG_PREFIX + " 注册存储平台失败 >> id={}, code={}", entity.getId(), entity.getCode(), e);
            }
        }
        log.info(LOG_PREFIX + " 同步完成 >> 共从 DB 载入并注册 {} 个存储平台: {}", registeredPlatforms.size(), registeredPlatforms);
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 存储点配置属性JSON 转换为底层存储平台配置对象，Setting 字段为对应 Config 字段的子集
     */
    private static <T extends FileStorageProperties.BaseConfig> T toConfig(
            FileStorageEntity entity, String fullPlatform, Class<T> configClass) throws Exception {
        T config = JSON_MAPPER.readValue(entity.getSettingJson(), configClass);
        config.setPlatform(fullPlatform);
        return config;
    }

    /**
     * 注销所有已注册的DB存储平台，并释放资源；YAML 静态配置的平台不受影响
     */
    private void unregisterAll(CopyOnWriteArrayList<FileStorage> fileStorageList) {
        for (String fullPlatform : registeredPlatforms) {
            fileStorageList.stream()
                    .filter(fs -> fullPlatform.equals(fs.getPlatform()))
                    .findFirst()
                    .ifPresent(fs -> {
                        fileStorageList.remove(fs);
                        fs.close();
                        log.info(LOG_PREFIX + " 已注销存储平台 >> {}", fullPlatform);
                    });
        }
        registeredPlatforms.clear();
    }

    /**
     * 将单条存储点记录注册到底层
     */
    private void register(FileStorageEntity entity, CopyOnWriteArrayList<FileStorage> fileStorageList) throws Exception {
        String fullPlatform = formatFullPlatform(entity.getTenantId(), entity.getCode());
        if (CharSequenceUtil.isBlank(entity.getSettingJson()) || !JSONUtil.isTypeJSONObject(entity.getSettingJson())) {
            throw new IllegalStateException("配置属性为空或不是合法的JSON对象");
        }

        List<? extends FileStorage> storages = switch (entity.getPlatformType()) {
            case LOCAL -> FileStorageServiceBuilder.buildLocalPlusFileStorage(
                    List.of(toConfig(entity, fullPlatform, FileStorageProperties.LocalPlusConfig.class)));
            case AMAZON_S3 -> FileStorageServiceBuilder.buildAmazonS3FileStorage(
                    List.of(toConfig(entity, fullPlatform, FileStorageProperties.AmazonS3Config.class)), null);
            case AMAZON_S3_V2 -> FileStorageServiceBuilder.buildAmazonS3V2FileStorage(
                    List.of(toConfig(entity, fullPlatform, FileStorageProperties.AmazonS3V2Config.class)), null);
            case ALIYUN_OSS -> FileStorageServiceBuilder.buildAliyunOssFileStorage(
                    List.of(toConfig(entity, fullPlatform, FileStorageProperties.AliyunOssConfig.class)), null);
        };
        fileStorageList.addAll(storages);
        registeredPlatforms.add(fullPlatform);
    }
}
