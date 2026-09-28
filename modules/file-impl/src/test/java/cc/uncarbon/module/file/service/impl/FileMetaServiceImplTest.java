package cc.uncarbon.module.file.service.impl;

import cc.uncarbon.module.file.dal.entity.FileMetaEntity;
import cc.uncarbon.module.file.dal.mapper.FileMetaMapper;
import cc.uncarbon.module.file.model.internal.FacadeUploadOptions;
import cc.uncarbon.module.file.model.request.FileAttrExtraRequest;
import cc.uncarbon.module.file.model.valueobj.FileMetaDTO;
import cc.uncarbon.module.file.model.valueobj.FileStorageDTO;
import cc.uncarbon.module.file.storage.DynamicFileStorageRegistrar;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
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

import java.util.List;

/**
 * {@link FileMetaServiceImpl} 文件元数据分支测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileMetaServiceImplTest {

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @Mock
    private FileMetaMapper fileMetaMapper;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private FileMetaServiceImpl service;


    @Test
    void findByDigestSha256BlankYieldsNull() {
        Assertions.assertNull(service.findByDigestSha256(null));
        Assertions.assertNull(service.findByDigestSha256("  "));

        Mockito.verify(fileMetaMapper, Mockito.never()).selectOne(Mockito.any());
    }

    @Test
    void findByDigestSha256Delegates() {
        Mockito.when(fileMetaMapper.selectOne(Mockito.any())).thenReturn(
                new FileMetaEntity().setId(7L).setDigestSha256("abc"));

        FileMetaDTO ret = service.findByDigestSha256("abc");

        Assertions.assertEquals(7L, ret.getId());
        Assertions.assertEquals("abc", ret.getDigestSha256());
    }

    @Test
    void saveMapsStorageFields() {
        Mockito.when(fileMetaMapper.insert(Mockito.any(FileMetaEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, FileMetaEntity.class).setId(7L);
            return 1;
        });
        FileInfo fileInfo = new FileInfo()
                .setBasePath("/")
                .setPath("2026/09/28/")
                .setFilename("stor.txt")
                .setOriginalFilename("orig.txt")
                .setExt("txt")
                .setSize(5L)
                .setUrl(null);
        FileStorageDTO storage = new FileStorageDTO();
        storage.setId(1L);
        storage.setCode("st");

        FileMetaDTO ret = service.save(fileInfo, storage,
                new FacadeUploadOptions().setDigestSha256("abc"),
                new FileAttrExtraRequest().setCategory("avatar"));

        Assertions.assertEquals(7L, ret.getId());
        Assertions.assertEquals(1L, ret.getStorageId());
        Assertions.assertEquals("st", ret.getStorageCode());
        Assertions.assertEquals("/", ret.getStorageBasePath());
        Assertions.assertEquals("2026/09/28/", ret.getSubDirPath());
        // 落库文件名不含扩展名
        Assertions.assertEquals("stor", ret.getStorageFilename());
        Assertions.assertEquals("orig", ret.getOriginalFilename());
        Assertions.assertEquals("txt", ret.getExtendName());
        Assertions.assertEquals(5L, ret.getFileSize());
        Assertions.assertEquals("abc", ret.getDigestSha256());
        Assertions.assertEquals("avatar", ret.getCategory());
        // 读取时拼回完整文件名
        Assertions.assertEquals("stor.txt", ret.getStorageFilenameFull());
    }

    @Test
    void adminDeleteSwallowsPhysicalFailure() {
        Mockito.when(fileMetaMapper.selectById(1L)).thenReturn(null);
        Mockito.when(fileMetaMapper.selectById(2L)).thenReturn(
                new FileMetaEntity().setId(2L).setStorageCode("st").setTenantId(0L));
        Mockito.doThrow(new RuntimeException("disk gone"))
                .when(fileStorageService).delete(Mockito.any(FileInfo.class));

        Assertions.assertDoesNotThrow(() -> service.adminDelete(List.of(1L, 2L)));

        Mockito.verify(fileStorageService).delete(Mockito.any(FileInfo.class));
        Mockito.verify(fileMetaMapper).deleteByIds(List.of(1L, 2L));
    }

    @Test
    void adminDeleteRebuildsFullPlatformName() {
        Mockito.when(fileMetaMapper.selectById(2L)).thenReturn(new FileMetaEntity()
                .setId(2L).setTenantId(0L).setStorageCode("st")
                .setStorageBasePath("/").setSubDirPath("p/").setStorageFilename("f").setExtendName("txt"));

        service.adminDelete(List.of(2L));

        ArgumentCaptor<FileInfo> captor = ArgumentCaptor.forClass(FileInfo.class);
        Mockito.verify(fileStorageService).delete(captor.capture());
        Assertions.assertEquals(DynamicFileStorageRegistrar.formatFullPlatform(0L, "st"),
                captor.getValue().getPlatform());
        Assertions.assertEquals("f.txt", captor.getValue().getFilename());
    }

    @Test
    void getByIdBranches() {
        Assertions.assertNull(service.getById(null));

        Mockito.when(fileMetaMapper.selectById(7L)).thenReturn(null);
        Assertions.assertNull(service.getById(7L));

        Mockito.when(fileMetaMapper.selectById(7L)).thenReturn(new FileMetaEntity().setId(7L).setExtendName("png"));
        Assertions.assertEquals("png", service.getById(7L).getExtendName());
    }
}
