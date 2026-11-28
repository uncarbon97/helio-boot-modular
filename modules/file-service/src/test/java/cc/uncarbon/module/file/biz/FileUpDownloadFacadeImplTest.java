package cc.uncarbon.module.file.biz;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.file.errorcode.FileErrorCodeEnum;
import cc.uncarbon.module.file.model.internal.FacadeUploadOptions;
import cc.uncarbon.module.file.model.request.FileAttrExtraRequest;
import cc.uncarbon.module.file.model.response.FileDownloadReply;
import cc.uncarbon.module.file.model.valueobj.FileMetaDTO;
import cc.uncarbon.module.file.model.valueobj.FileStorageDTO;
import cc.uncarbon.module.file.service.FileMetaService;
import cc.uncarbon.module.file.service.FileStorageDataService;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cc.uncarbon.module.tenant.model.valueobj.TenantValidateResult;
import org.dromara.x.file.storage.core.Downloader;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * {@link FileUpDownloadFacadeImpl} 文件上传下载门面编排分支测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileUpDownloadFacadeImplTest {

    @Mock
    private FileMetaService fileMetaService;
    @Mock
    private FileStorageDataService fileStorageDataService;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private TenantFacade tenantFacade;

    @InjectMocks
    private FileUpDownloadFacadeImpl facade;


    /*
    ----------------------------------------------------------------
                        下载相关
    ----------------------------------------------------------------
     */

    /**
     * 链式 UploadPretreatment 模拟对象，setter 均返回自身
     */
    private static UploadPretreatment fluentPretreatment() {
        return Mockito.mock(UploadPretreatment.class, Mockito.RETURNS_SELF);
    }

    private static TenantValidateResult validTenant() {
        return TenantValidateResult.pass()
                .setTenantId(1L)
                .setTenantName("租户甲")
                .setTenantCode("tc");
    }

    private static FileMetaDTO metaWithoutDirectUrl() {
        return new FileMetaDTO()
                .setStorageCode("st")
                .setStorageBasePath("/")
                .setSubDirPath("p/")
                .setStorageFilename("f")
                .setOriginalFilename("orig")
                .setExtendName("txt")
                .setFileSize(5L);
    }

    private static FileStorageDTO storageDto(String code) {
        return new FileStorageDTO().setId(1L).setCode(code);
    }

    private static FacadeUploadOptions options(String originalFilename) {
        return new FacadeUploadOptions()
                .setOriginalFilename(originalFilename)
                .setContentType("text/plain");
    }

    @Test
    void findByDigestSha256Delegates() {
        Mockito.when(fileMetaService.findByDigestSha256("abc")).thenReturn(new FileMetaDTO().setId(7L));

        Assertions.assertEquals(7L, facade.findByDigestSha256("abc").getId());
    }

    @Test
    void downloadInvalidTenantHiddenAsFileMissing() {
        Mockito.when(tenantFacade.validateByCode("ghost"))
                .thenReturn(new TenantValidateResult(false, FileErrorCodeEnum.A02006));

        FileDownloadReply reply = facade.downloadByTenantAndId("ghost", 7L);

        // 不直接透出租户校验失败细节，统一按“文件不存在或无权限访问”返回
        Assertions.assertEquals(FileErrorCodeEnum.A02006, reply.getErrorCode());
        Mockito.verifyNoInteractions(fileMetaService, fileStorageService);
    }


    /*
    ----------------------------------------------------------------
                        上传相关
    ----------------------------------------------------------------
     */

    @Test
    void downloadByProxyBytesWithoutDirectUrl() {
        Mockito.when(tenantFacade.validateByCode("tc")).thenReturn(validTenant());
        Mockito.when(fileMetaService.getNonnullById(7L)).thenReturn(metaWithoutDirectUrl());
        byte[] bytes = {1, 2, 3};
        Downloader downloader = Mockito.mock(Downloader.class);
        Mockito.when(downloader.bytes()).thenReturn(bytes);
        Mockito.when(fileStorageService.download(Mockito.any(FileInfo.class))).thenReturn(downloader);

        FileDownloadReply reply = facade.downloadByTenantAndId("tc", 7L);

        Assertions.assertTrue(reply.isSuccess());
        Assertions.assertFalse(reply.isRedirect2DirectUrl());
        Assertions.assertArrayEquals(bytes, reply.getFileBytes());
        Assertions.assertEquals("f.txt", reply.getStorageFilename());

        // 重建的底层 FileInfo 平台名须为 租户ID_存储点编码
        ArgumentCaptor<FileInfo> fiCaptor = ArgumentCaptor.forClass(FileInfo.class);
        Mockito.verify(fileStorageService).download(fiCaptor.capture());
        Assertions.assertEquals("1_st", fiCaptor.getValue().getPlatform());
        Assertions.assertEquals("f.txt", fiCaptor.getValue().getFilename());
        Assertions.assertEquals("/", fiCaptor.getValue().getBasePath());
        Assertions.assertEquals("p/", fiCaptor.getValue().getPath());
    }

    @Test
    void downloadProxyFailureYieldsB02002() {
        Mockito.when(tenantFacade.validateByCode("tc")).thenReturn(validTenant());
        Mockito.when(fileMetaService.getNonnullById(7L)).thenReturn(metaWithoutDirectUrl());
        Mockito.when(fileStorageService.download(Mockito.any(FileInfo.class)))
                .thenThrow(new FileStorageRuntimeException("disk gone"));

        FileDownloadReply reply = facade.downloadByTenantAndId("tc", 7L);

        Assertions.assertEquals(FileErrorCodeEnum.B02002, reply.getErrorCode());
    }

    @Test
    void downloadRedirectsWhenDirectUrlPresent() {
        Mockito.when(tenantFacade.validateByCode("tc")).thenReturn(validTenant());
        Mockito.when(fileMetaService.getNonnullById(7L))
                .thenReturn(metaWithoutDirectUrl().setDirectUrl("https://cdn.example.com/f.txt"));

        FileDownloadReply reply = facade.downloadByTenantAndId("tc", 7L);

        Assertions.assertTrue(reply.isSuccess());
        Assertions.assertTrue(reply.isRedirect2DirectUrl());
        Assertions.assertEquals("https://cdn.example.com/f.txt", reply.getDirectUrl());
        Assertions.assertNull(reply.getFileBytes());
        // 有直链时不触发服务端代理下载
        Mockito.verify(fileStorageService, Mockito.never()).download(Mockito.any(FileInfo.class));
    }

    @Test
    void downloadBusinessExceptionRethrownAsIs() {
        Mockito.when(tenantFacade.validateByCode("tc")).thenReturn(validTenant());
        Mockito.when(fileMetaService.getNonnullById(7L)).thenThrow(new NoRecordException());

        Assertions.assertThrows(NoRecordException.class, () -> facade.downloadByTenantAndId("tc", 7L));
    }

    @Test
    void downloadUnknownExceptionYieldsB02002() {
        Mockito.when(tenantFacade.validateByCode("tc")).thenReturn(validTenant());
        Mockito.when(fileMetaService.getNonnullById(7L)).thenThrow(new IllegalStateException("db down"));

        FileDownloadReply reply = facade.downloadByTenantAndId("tc", 7L);

        Assertions.assertEquals(FileErrorCodeEnum.B02002, reply.getErrorCode());
    }

    @Test
    void uploadBytesUsesPrimaryStorageWithoutTenantContext() {
        UploadPretreatment pretreatment = fluentPretreatment();
        byte[] data = {1};
        Mockito.when(fileStorageService.of(data)).thenReturn(pretreatment);
        FileStorageDTO storage = storageDto("st");
        Mockito.when(fileStorageDataService.getPrimary()).thenReturn(storage);
        Mockito.when(pretreatment.upload()).thenReturn(new FileInfo());
        FileMetaDTO saved = new FileMetaDTO().setId(9L);
        Mockito.when(fileMetaService.save(Mockito.any(FileInfo.class), Mockito.eq(storage),
                Mockito.any(FacadeUploadOptions.class), Mockito.any(FileAttrExtraRequest.class))).thenReturn(saved);

        FileMetaDTO ret = facade.upload(data, options("o.txt"), new FileAttrExtraRequest().setCategory("avatar"));

        Assertions.assertEquals(9L, ret.getId());
        // 无租户上下文时平台名退化为纯存储点编码
        Mockito.verify(pretreatment).setPlatform("st");
        Mockito.verify(pretreatment).setOriginalFilename("o.txt");
        Mockito.verify(pretreatment).setContentType("text/plain");
        Mockito.verify(pretreatment).setSaveFilename(Mockito.isNull());
        // 存储子路径按日期组织，如 2026/10/16/
        Mockito.verify(pretreatment).setPath(Mockito.matches("\\d{4}/\\d{2}/\\d{2}/"));
    }

    @Test
    void uploadBytesUnderTenantContextBuildsTenantPlatform() throws Exception {
        UploadPretreatment pretreatment = fluentPretreatment();
        byte[] data = {1};
        Mockito.when(fileStorageService.of(data)).thenReturn(pretreatment);
        Mockito.when(fileStorageDataService.getPrimary()).thenReturn(storageDto("st"));
        Mockito.when(pretreatment.upload()).thenReturn(new FileInfo());
        Mockito.when(fileMetaService.save(Mockito.any(FileInfo.class), Mockito.any(FileStorageDTO.class),
                Mockito.any(FacadeUploadOptions.class), Mockito.any(FileAttrExtraRequest.class)))
                .thenReturn(new FileMetaDTO().setId(9L));

        FileMetaDTO ret = TenantContextHolder.callWithContext(
                new SimpleTenantContext(5L, "租户甲", "tc"),
                () -> facade.upload(data, options("o.txt"), null));

        Assertions.assertNotNull(ret);
        Mockito.verify(pretreatment).setPlatform("5_st");
        // attr 为 null 时内部兜底为空附加属性对象
        ArgumentCaptor<FileAttrExtraRequest> attrCaptor = ArgumentCaptor.forClass(FileAttrExtraRequest.class);
        Mockito.verify(fileMetaService).save(Mockito.any(FileInfo.class), Mockito.any(FileStorageDTO.class),
                Mockito.any(FacadeUploadOptions.class), attrCaptor.capture());
        Assertions.assertNotNull(attrCaptor.getValue());
    }

    @Test
    void uploadSpecifiedPlatformMissingRejected() {
        UploadPretreatment pretreatment = fluentPretreatment();
        Mockito.when(fileStorageService.of(Mockito.any())).thenReturn(pretreatment);
        Mockito.when(fileStorageDataService.getByStorageCode("nope")).thenReturn(null);

        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> facade.upload(new byte[]{1}, options("o.txt").setPlatform("nope"), null));

        Assertions.assertEquals(FileErrorCodeEnum.B02003, ex.getErrorCode());
        Mockito.verify(pretreatment, Mockito.never()).upload();
    }

    @Test
    void uploadPrimaryMissingRejected() {
        UploadPretreatment pretreatment = fluentPretreatment();
        Mockito.when(fileStorageService.of(Mockito.any())).thenReturn(pretreatment);
        Mockito.when(fileStorageDataService.getPrimary()).thenReturn(null);

        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> facade.upload(new byte[]{1}, options("o.txt"), null));

        Assertions.assertEquals(FileErrorCodeEnum.B02003, ex.getErrorCode());
        Mockito.verify(pretreatment, Mockito.never()).upload();
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    @Test
    void uploadOriginalFilenameWrittenAsMetadata() throws Exception {
        UploadPretreatment pretreatment = fluentPretreatment();
        byte[] data = {1};
        Mockito.when(fileStorageService.of(data)).thenReturn(pretreatment);
        Mockito.when(fileStorageDataService.getPrimary()).thenReturn(storageDto("st"));
        Mockito.when(fileStorageService.isSupportMetadata("5_st")).thenReturn(true);
        Mockito.when(pretreatment.upload()).thenReturn(new FileInfo());
        Mockito.when(fileMetaService.save(Mockito.any(FileInfo.class), Mockito.any(FileStorageDTO.class),
                Mockito.any(FacadeUploadOptions.class), Mockito.any(FileAttrExtraRequest.class)))
                .thenReturn(new FileMetaDTO());

        FacadeUploadOptions options = options("a b.txt").setUseOriginalFilenameAsDownloadFileName(true);
        TenantContextHolder.runWithContext(new SimpleTenantContext(5L, "租户甲", "tc"),
                () -> facade.upload(data, options, null));

        Mockito.verify(pretreatment).putMetadata(Mockito.eq(Constant.Metadata.CONTENT_DISPOSITION),
                Mockito.eq("attachment;filename=" + URLEncoder.encode("a b.txt", StandardCharsets.UTF_8)));
    }

    @Test
    void uploadOriginalFilenameSkippedWhenMetadataUnsupported() {
        UploadPretreatment pretreatment = fluentPretreatment();
        Mockito.when(fileStorageService.of(Mockito.any())).thenReturn(pretreatment);
        Mockito.when(fileStorageDataService.getPrimary()).thenReturn(storageDto("st"));
        Mockito.when(fileStorageService.isSupportMetadata(Mockito.anyString())).thenReturn(false);
        Mockito.when(pretreatment.upload()).thenReturn(new FileInfo());
        Mockito.when(fileMetaService.save(Mockito.any(FileInfo.class), Mockito.any(FileStorageDTO.class),
                Mockito.any(FacadeUploadOptions.class), Mockito.any(FileAttrExtraRequest.class)))
                .thenReturn(new FileMetaDTO());

        facade.upload(new byte[]{1},
                options("o.txt").setUseOriginalFilenameAsDownloadFileName(true), null);

        Mockito.verify(pretreatment, Mockito.never()).putMetadata(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void uploadWithoutDownloadNameOptionSkipsMetadataCheck() {
        UploadPretreatment pretreatment = fluentPretreatment();
        Mockito.when(fileStorageService.of(Mockito.any())).thenReturn(pretreatment);
        Mockito.when(fileStorageDataService.getPrimary()).thenReturn(storageDto("st"));
        Mockito.when(pretreatment.upload()).thenReturn(new FileInfo());
        Mockito.when(fileMetaService.save(Mockito.any(FileInfo.class), Mockito.any(FileStorageDTO.class),
                Mockito.any(FacadeUploadOptions.class), Mockito.any(FileAttrExtraRequest.class)))
                .thenReturn(new FileMetaDTO());

        facade.upload(new byte[]{1}, options("o.txt"), null);

        Mockito.verify(fileStorageService, Mockito.never()).isSupportMetadata(Mockito.anyString());
    }

    @Test
    void uploadInputStreamDelegatesToPretreatment() {
        UploadPretreatment pretreatment = fluentPretreatment();
        InputStream in = new ByteArrayInputStream(new byte[]{1});
        Mockito.when(fileStorageService.of(Mockito.any(), Mockito.eq("o.txt"),
                Mockito.eq("text/plain"), Mockito.eq(10L))).thenReturn(pretreatment);
        Mockito.when(fileStorageDataService.getPrimary()).thenReturn(storageDto("st"));
        Mockito.when(pretreatment.upload()).thenReturn(new FileInfo());
        Mockito.when(fileMetaService.save(Mockito.any(FileInfo.class), Mockito.any(FileStorageDTO.class),
                Mockito.any(FacadeUploadOptions.class), Mockito.any(FileAttrExtraRequest.class)))
                .thenReturn(new FileMetaDTO().setId(3L));

        FileMetaDTO ret = facade.upload(in, 10L, options("o.txt"), null);

        Assertions.assertEquals(3L, ret.getId());
        Mockito.verify(pretreatment).setOriginalFilename("o.txt");
    }

    @Test
    void uploadStorageFailureYieldsB02001() {
        UploadPretreatment pretreatment = fluentPretreatment();
        Mockito.when(fileStorageService.of(Mockito.any())).thenReturn(pretreatment);
        Mockito.when(fileStorageDataService.getPrimary()).thenReturn(storageDto("st"));
        Mockito.when(pretreatment.upload()).thenThrow(new FileStorageRuntimeException("io error"));

        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> facade.upload(new byte[]{1}, options("o.txt"), null));

        Assertions.assertEquals(FileErrorCodeEnum.B02001, ex.getErrorCode());
        Mockito.verify(fileMetaService, Mockito.never()).save(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());
    }
}
