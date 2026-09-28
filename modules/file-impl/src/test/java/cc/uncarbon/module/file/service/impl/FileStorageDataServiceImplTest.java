package cc.uncarbon.module.file.service.impl;

import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.file.dal.entity.FileMetaEntity;
import cc.uncarbon.module.file.dal.entity.FileStorageEntity;
import cc.uncarbon.module.file.dal.mapper.FileMetaMapper;
import cc.uncarbon.module.file.dal.mapper.FileStorageMapper;
import cc.uncarbon.module.file.enums.StoragePlatformTypeEnum;
import cc.uncarbon.module.file.errorcode.FileErrorCodeEnum;
import cc.uncarbon.module.file.model.request.AdminFileStorageUpsertRequest;
import cc.uncarbon.module.file.model.setting.storage.LocalSetting;
import cc.uncarbon.module.file.storage.event.FileStorageChangedEvent;
import cc.uncarbon.module.file.MybatisPlusTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cc.uncarbon.module.file.model.query.AdminFileStorageListQuery;
import cc.uncarbon.framework.helium.base.page.PageParam;

/**
 * {@link FileStorageDataServiceImpl} 存储点管理分支测试（主存储点唯一/引用保护/配置序列化）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileStorageDataServiceImplTest {

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @Mock
    private FileStorageMapper fileStorageMapper;
    @Mock
    private FileMetaMapper fileMetaMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FileStorageDataServiceImpl service;


    @Test
    void adminCreateDuplicateCodeRejected() {
        Mockito.when(fileStorageMapper.selectOne(Mockito.any())).thenReturn(new FileStorageEntity().setId(1L));

        var ex = Assertions.assertThrows(HasRepeatRecordException.class,
                () -> service.adminCreate(upsert(null, "st", YesOrNoEnum.NO)));
        Assertions.assertEquals(FileErrorCodeEnum.A02007, ex.getErrorCode());
    }

    @Test
    void adminCreateDuplicatePrimaryRejected() {
        Mockito.when(fileStorageMapper.selectOne(Mockito.any())).thenReturn(null);
        // 第二次 selectOne（主存储点检查）命中已有主存储点
        Mockito.when(fileStorageMapper.selectOne(Mockito.any())).thenReturn(null)
                .thenReturn(new FileStorageEntity().setId(1L));

        var ex = Assertions.assertThrows(HasRepeatRecordException.class,
                () -> service.adminCreate(upsert(null, "st", YesOrNoEnum.YES)));
        Assertions.assertEquals(FileErrorCodeEnum.A02008, ex.getErrorCode());
    }

    @Test
    void adminCreateSerializesSettingAndPublishesEvent() {
        Mockito.when(fileStorageMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(fileStorageMapper.insert(Mockito.any(FileStorageEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, FileStorageEntity.class).setId(3L);
            return 1;
        });

        Map<String, Object> settingBody = new HashMap<>();
        settingBody.put("basePath", "/");
        settingBody.put("storagePath", "./target/it-file-storage");
        settingBody.put("domain", "");
        Long id = service.adminCreate(upsert(null, "st", YesOrNoEnum.YES).setSettingBody(settingBody));

        Assertions.assertEquals(3L, id);
        ArgumentCaptor<FileStorageEntity> captor = ArgumentCaptor.forClass(FileStorageEntity.class);
        Mockito.verify(fileStorageMapper).insert(captor.capture());
        Assertions.assertTrue(captor.getValue().getSettingJson().contains("storagePath"));
        Mockito.verify(eventPublisher).publishEvent(Mockito.any(FileStorageChangedEvent.class));
    }

    @Test
    void adminUpdateMissingRejected() {
        Mockito.when(fileStorageMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(fileStorageMapper.selectById(9L)).thenReturn(null);

        Assertions.assertThrows(NoRecordException.class,
                () -> service.adminUpdate(upsert(9L, "st", YesOrNoEnum.NO)));
    }

    @Test
    void adminUpdateCodeChangeWhileInUseRejected() {
        Mockito.when(fileStorageMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(fileStorageMapper.selectById(9L)).thenReturn(
                new FileStorageEntity().setId(9L).setCode("old"));
        Mockito.when(fileMetaMapper.exists(Mockito.any())).thenReturn(true);

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminUpdate(upsert(9L, "new", YesOrNoEnum.NO)));
        Assertions.assertEquals(FileErrorCodeEnum.A02010, ex.getErrorCode());
    }

    @Test
    void adminUpdateSameCodeSkipsInUseCheck() {
        Mockito.when(fileStorageMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(fileStorageMapper.selectById(9L)).thenReturn(
                new FileStorageEntity().setId(9L).setCode("st"));

        service.adminUpdate(upsert(9L, "st", YesOrNoEnum.NO));

        Mockito.verify(fileStorageMapper).updateById(Mockito.any(FileStorageEntity.class));
        Mockito.verify(fileMetaMapper, Mockito.never()).exists(Mockito.any());
        Mockito.verify(eventPublisher).publishEvent(Mockito.any(FileStorageChangedEvent.class));
    }

    @Test
    void adminDeletePrimaryRejected() {
        Mockito.when(fileStorageMapper.selectOne(Mockito.any())).thenReturn(new FileStorageEntity().setId(9L));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminDelete(List.of(9L)));
        Assertions.assertEquals(FileErrorCodeEnum.A02009, ex.getErrorCode());
    }

    @Test
    void adminDeleteInUseRejected() {
        Mockito.when(fileStorageMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(fileStorageMapper.selectList(Mockito.any()))
                .thenReturn(List.of(new FileStorageEntity().setId(9L).setCode("st")));
        Mockito.when(fileMetaMapper.exists(Mockito.any())).thenReturn(true);

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminDelete(List.of(9L)));
        Assertions.assertEquals(FileErrorCodeEnum.A02010, ex.getErrorCode());
    }

    @Test
    void adminDeleteHappyPath() {
        Mockito.when(fileStorageMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(fileStorageMapper.selectList(Mockito.any()))
                .thenReturn(List.of(new FileStorageEntity().setId(9L).setCode("st")));
        Mockito.when(fileMetaMapper.exists(Mockito.any())).thenReturn(false);

        service.adminDelete(List.of(9L));

        Mockito.verify(fileStorageMapper).deleteByIds(List.of(9L));
        Mockito.verify(eventPublisher).publishEvent(Mockito.any(FileStorageChangedEvent.class));
    }

    @Test
    void getByStorageCodeDeserializesSetting() {
        Mockito.when(fileStorageMapper.selectOne(Mockito.any())).thenReturn(
                new FileStorageEntity().setId(9L).setCode("st")
                        .setPlatformType(StoragePlatformTypeEnum.LOCAL)
                        .setSettingJson("{\"storagePath\":\"/x\",\"basePath\":\"/\",\"domain\":\"\"}"));

        var ret = service.getByStorageCode("st");

        Assertions.assertNotNull(ret.getSettingBody());
        LocalSetting setting = (LocalSetting) ret.getSettingBody();
        Assertions.assertEquals("/x", setting.getStoragePath());
    }

    @Test
    void getPrimaryDelegates() {
        Mockito.when(fileStorageMapper.selectOne(Mockito.any())).thenReturn(
                new FileStorageEntity().setId(9L).setPrimaryFlag(YesOrNoEnum.YES));

        Assertions.assertEquals(YesOrNoEnum.YES, service.getPrimary().getPrimaryFlag());

        Mockito.when(fileStorageMapper.selectOne(Mockito.any())).thenReturn(null);
        Assertions.assertNull(service.getPrimary());
    }

    @Test
    void adminListStripsSettingJson() {
        Mockito.when(fileStorageMapper.selectPage(Mockito.any(), Mockito.any())).thenAnswer(inv -> {
            Page<FileStorageEntity> page = inv.getArgument(0);
            FileStorageEntity entity = new FileStorageEntity().setId(9L).setSettingJson("{}");
            page.setRecords(List.of(entity));
            page.setTotal(1);
            return page;
        });

        var ret = service.adminList(new AdminFileStorageListQuery()
                .setPageParam(new PageParam(1, 10)));

        Assertions.assertNull(ret.getRecords().get(0).getSettingBody());
    }

    @Test
    void getByIdBranches() {
        Assertions.assertNull(service.getById(null));

        Mockito.when(fileStorageMapper.selectById(9L)).thenReturn(null);
        Assertions.assertThrows(NoRecordException.class, () -> service.getNonnullById(9L));
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private AdminFileStorageUpsertRequest upsert(Long id, String code, YesOrNoEnum primaryFlag) {
        return new AdminFileStorageUpsertRequest()
                .setId(id).setCode(code).setName("存储点")
                .setPlatformType(StoragePlatformTypeEnum.LOCAL)
                .setSettingBody(new HashMap<>())
                .setPrimaryFlag(primaryFlag);
    }
}
