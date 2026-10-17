package cc.uncarbon.module.file.service.impl;

import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.file.MybatisPlusTestSupport;
import cc.uncarbon.module.file.dal.entity.FileMetaEntity;
import cc.uncarbon.module.file.dal.mapper.FileMetaMapper;
import cc.uncarbon.module.file.model.internal.FacadeUploadOptions;
import cc.uncarbon.module.file.model.query.AdminFileMetaListQuery;
import cc.uncarbon.module.file.model.request.FileAttrExtraRequest;
import cc.uncarbon.module.file.model.valueobj.FileMetaDTO;
import cc.uncarbon.module.file.model.valueobj.FileStorageDTO;
import cc.uncarbon.module.file.storage.DynamicFileStorageRegistrar;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
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

import java.time.Instant;
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

    @Test
    void getNonnullByIdBranches() {
        Mockito.when(fileMetaMapper.selectById(7L)).thenReturn(new FileMetaEntity().setId(7L).setExtendName("txt"));
        Assertions.assertEquals(7L, service.getNonnullById(7L).getId());

        Mockito.when(fileMetaMapper.selectById(8L)).thenReturn(null);
        Assertions.assertThrows(NoRecordException.class, () -> service.getNonnullById(8L));
    }

    @Test
    void adminListConvertsPageWithBlankFilters() {
        stubSelectPage(2L,
                new FileMetaEntity().setId(1L).setExtendName("txt"),
                new FileMetaEntity().setId(2L).setExtendName("png"));

        PageResult<FileMetaDTO> ret = service.adminList(new AdminFileMetaListQuery()
                .setPageParam(new PageParam(1, 10)));

        Assertions.assertEquals(2L, ret.getTotal());
        Assertions.assertEquals(2, ret.getRecords().size());
        Assertions.assertEquals("txt", ret.getRecords().get(0).getExtendName());
        Assertions.assertEquals("png", ret.getRecords().get(1).getExtendName());
    }

    @Test
    void adminListConvertsPageWithAllFilters() {
        stubSelectPage(1L, new FileMetaEntity().setId(1L).setExtendName("txt"));

        // 全部筛选条件齐备，覆盖 eq/like/between 组装分支
        PageResult<FileMetaDTO> ret = service.adminList(new AdminFileMetaListQuery()
                .setPageParam(new PageParam(1, 10))
                .setStorageCode("st")
                .setExtendName("txt")
                .setCategory("avatar")
                .setBeginAt(Instant.parse("2026-01-01T00:00:00Z"))
                .setEndAt(Instant.parse("2026-12-31T23:59:59Z")));

        Assertions.assertEquals(1, ret.getRecords().size());

        // beginAt 为空时短路，不应用时间区间
        ret = service.adminList(new AdminFileMetaListQuery()
                .setPageParam(new PageParam(1, 10))
                .setEndAt(Instant.parse("2026-12-31T23:59:59Z")));
        Assertions.assertEquals(1, ret.getRecords().size());
    }

    @Test
    void adminListEmptyRecordsYieldsEmptyPage() {
        stubSelectPage(0L);

        PageResult<FileMetaDTO> ret = service.adminList(new AdminFileMetaListQuery()
                .setPageParam(new PageParam(1, 10)));

        Assertions.assertTrue(ret.getRecords().isEmpty());
        Assertions.assertEquals(0L, ret.getTotal());
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    /**
     * 模拟分页查询返回
     */
    private void stubSelectPage(long total, FileMetaEntity... entities) {
        Mockito.when(fileMetaMapper.selectPage(Mockito.any(), Mockito.any())).thenAnswer(inv -> {
            Page<FileMetaEntity> page = inv.getArgument(0);
            page.setRecords(List.of(entities));
            page.setTotal(total);
            return page;
        });
    }
}
