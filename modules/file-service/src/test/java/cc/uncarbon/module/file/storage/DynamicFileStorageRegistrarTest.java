package cc.uncarbon.module.file.storage;

import cc.uncarbon.module.file.dal.entity.FileStorageEntity;
import cc.uncarbon.module.file.dal.mapper.FileStorageMapper;
import cc.uncarbon.module.file.enums.StoragePlatformTypeEnum;
import cc.uncarbon.module.file.storage.event.FileStorageChangedEvent;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * {@link DynamicFileStorageRegistrar} 动态存储点注册器分支测试
 * （全量重建/注销/非法配置跳过/云端平台构建/初始化与事件入口）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DynamicFileStorageRegistrarTest {

    private static final String LOCAL_SETTING_JSON =
            "{\"storagePath\":\"./target/it-registrar-test\",\"basePath\":\"/\",\"domain\":\"\"}";

    @Mock
    private FileStorageMapper fileStorageMapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private FileStorageSyncBroadcaster fileStorageSyncBroadcaster;

    @InjectMocks
    private DynamicFileStorageRegistrar registrar;

    /**
     * 底层存储平台列表须可变且多次调用返回同一实例
     */
    private CopyOnWriteArrayList<FileStorage> fileStorageList;

    private static FileStorageEntity localEntity(Long tenantId, String code) {
        return new FileStorageEntity()
                .setId(1L)
                .setTenantId(tenantId)
                .setCode(code)
                .setPlatformType(StoragePlatformTypeEnum.LOCAL)
                .setSettingJson(LOCAL_SETTING_JSON);
    }

    private static FileStorageEntity cloudEntity(StoragePlatformTypeEnum platformType, String code) {
        return new FileStorageEntity()
                .setId(1L)
                .setTenantId(null)
                .setCode(code)
                .setPlatformType(platformType)
                .setSettingJson("{}");
    }

    @BeforeEach
    void setUp() {
        fileStorageList = new CopyOnWriteArrayList<>();
        Mockito.when(fileStorageService.getFileStorageList()).thenReturn(fileStorageList);
    }

    @Test
    void formatFullPlatformBranches() {
        // 非租户环境退化为纯编码
        Assertions.assertEquals("st", DynamicFileStorageRegistrar.formatFullPlatform(null, "st"));
        // 租户环境拼为 租户ID_编码
        Assertions.assertEquals("1_st", DynamicFileStorageRegistrar.formatFullPlatform(1L, "st"));
    }

    @Test
    void reloadAllRegistersTenantScopedLocalPlatform() {
        Mockito.when(fileStorageMapper.selectList(Mockito.any()))
                .thenReturn(List.of(localEntity(1L, "st")));

        registrar.reloadAll();

        Assertions.assertEquals(1, fileStorageList.size());
        Assertions.assertEquals("1_st", fileStorageList.get(0).getPlatform());
    }

    @Test
    void reloadAllRegistersBareCodeWithoutTenant() {
        Mockito.when(fileStorageMapper.selectList(Mockito.any()))
                .thenReturn(List.of(localEntity(null, "st")));

        registrar.reloadAll();

        Assertions.assertEquals(List.of("st"), platforms());
    }

    @Test
    void reloadAllReplacesPreviouslyRegisteredPlatform() {
        Mockito.when(fileStorageMapper.selectList(Mockito.any()))
                .thenReturn(List.of(localEntity(1L, "st")));

        registrar.reloadAll();
        FileStorage first = fileStorageList.get(0);

        registrar.reloadAll();

        // 先注销旧的再注册新的，列表不重复堆积
        Assertions.assertEquals(1, fileStorageList.size());
        Assertions.assertNotSame(first, fileStorageList.get(0));
        Assertions.assertEquals("1_st", fileStorageList.get(0).getPlatform());
    }

    @Test
    void reloadAllUnregistersDisappearedPlatform() {
        Mockito.when(fileStorageMapper.selectList(Mockito.any()))
                .thenReturn(List.of(localEntity(1L, "st")));
        registrar.reloadAll();

        Mockito.when(fileStorageMapper.selectList(Mockito.any()))
                .thenReturn(List.of(localEntity(1L, "other")));
        registrar.reloadAll();

        // 旧平台已注销，只保留最新 DB 记录对应的平台
        Assertions.assertEquals(List.of("1_other"), platforms());
    }

    @Test
    void reloadAllToleratesExternallyRemovedPlatform() {
        Mockito.when(fileStorageMapper.selectList(Mockito.any()))
                .thenReturn(List.of(localEntity(1L, "st")));
        registrar.reloadAll();

        // 模拟底层列表被外部清空，注销时找不到对应平台也不应报错
        fileStorageList.clear();

        Assertions.assertDoesNotThrow(registrar::reloadAll);

        Assertions.assertEquals(List.of("1_st"), platforms());
    }

    @Test
    void reloadAllSkipsEntitiesWithInvalidSettingJson() {
        Mockito.when(fileStorageMapper.selectList(Mockito.any())).thenReturn(List.of(
                localEntity(1L, "blank").setSettingJson(""),
                localEntity(1L, "notJson").setSettingJson("abc"),
                localEntity(1L, "jsonArray").setSettingJson("[1,2]"),
                localEntity(1L, "st")));

        registrar.reloadAll();

        // 单条失败跳过，不阻断其余注册
        Assertions.assertEquals(List.of("1_st"), platforms());
    }

    @Test
    void reloadAllBuildsCloudPlatformTypes() {
        Mockito.when(fileStorageMapper.selectList(Mockito.any())).thenReturn(List.of(
                cloudEntity(StoragePlatformTypeEnum.AMAZON_S3, "s3c"),
                cloudEntity(StoragePlatformTypeEnum.AMAZON_S3_V2, "s3v2c"),
                cloudEntity(StoragePlatformTypeEnum.ALIYUN_OSS, "ossc")));

        Assertions.assertDoesNotThrow(registrar::reloadAll);

        // 云端 SDK 客户端为惰性构建，此处仅注册平台对象
        Assertions.assertEquals(List.of("s3c", "s3v2c", "ossc"), platforms());
    }

    @Test
    void reloadAllStopsWhenReadFails() {
        Mockito.when(fileStorageMapper.selectList(Mockito.any()))
                .thenThrow(new RuntimeException("db down"));

        Assertions.assertDoesNotThrow(registrar::reloadAll);

        // 读取失败直接返回，不再触碰底层平台列表
        Mockito.verify(fileStorageService, Mockito.never()).getFileStorageList();
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    @Test
    void initTriggersFullReload() {
        Mockito.when(fileStorageMapper.selectList(Mockito.any()))
                .thenReturn(List.of(localEntity(1L, "st")));

        registrar.init();

        Assertions.assertEquals(List.of("1_st"), platforms());
    }

    @Test
    void onFileStorageChangedReloadsThenBroadcasts() {
        Mockito.when(fileStorageMapper.selectList(Mockito.any()))
                .thenReturn(List.of(localEntity(1L, "st")));

        registrar.onFileStorageChanged(new FileStorageChangedEvent(
                new FileStorageChangedEvent.EventData(FileStorageChangedEvent.ChangeType.UPDATE)));

        // 先本机全量重建，再向集群其他实例广播
        Assertions.assertEquals(List.of("1_st"), platforms());
        Mockito.verify(fileStorageSyncBroadcaster)
                .broadcast(FileStorageChangedEvent.ChangeType.UPDATE);
    }

    private List<String> platforms() {
        return fileStorageList.stream().map(FileStorage::getPlatform).collect(Collectors.toList());
    }
}
