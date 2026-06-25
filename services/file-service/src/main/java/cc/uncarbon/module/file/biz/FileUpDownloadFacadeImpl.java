package cc.uncarbon.module.file.biz;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.file.errorcode.FileErrorCodeEnum;
import cc.uncarbon.module.file.facade.FileUpDownloadFacade;
import cc.uncarbon.module.file.model.internal.FacadeUploadOptions;
import cc.uncarbon.module.file.model.request.FileAttrExtraRequest;
import cc.uncarbon.module.file.model.response.FileDownloadReply;
import cc.uncarbon.module.file.model.valueobj.FileMetaDTO;
import cc.uncarbon.module.file.model.valueobj.FileStorageDTO;
import cc.uncarbon.module.file.service.FileMetaService;
import cc.uncarbon.module.file.service.FileStorageDataService;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cc.uncarbon.module.tenant.model.valueobj.TenantValidateResult;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;


/**
 * 文件上传下载门面
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUpDownloadFacadeImpl implements FileUpDownloadFacade {

    private static final String LOG_PREFIX = "[文件上传下载门面]";

    private final FileMetaService fileMetaService;
    private final FileStorageDataService fileStorageDataService;
    private final FileStorageService fileStorageService;
    private final TenantFacade tenantFacade;


    @Override
    public FileMetaDTO findByDigestSha256(String sha256) {
        return fileMetaService.findByDigestSha256(sha256);
    }

    @Override
    public FileMetaDTO upload(byte[] fileBytes,
                              @NonNull FacadeUploadOptions options,
                              @Nullable FileAttrExtraRequest attr) throws BusinessException {
        // 要上传到的平台名
        String platform = ObjectUtil.defaultIfNull(options.getPlatform(),
                fileStorageService.getProperties()::getDefaultPlatform);
        // 找对应的存储点
        FileStorageDTO storage = fileStorageDataService.getByStorageCode(platform);
        FileErrorCodeEnum.B02003.throwIfNull(storage);

        // 如果需要缩略图: .setSaveThFilename().setThContentType()
        UploadPretreatment uploadPretreatment = fileStorageService
                .of(fileBytes)
                .setOriginalFilename(options.getOriginalFilename())
                // 不手动指定，由框架自动生成存储文件名
                .setSaveFilename(null)
                .setContentType(options.getContentType())
                .setPlatform(storage.getCode())
                .setPath(formatDatePath(LocalDateTime.now()));
        if (options.isUseOriginalFilenameAsDownloadFileName() && fileStorageService.isSupportMetadata(platform)) {
            String downFileName = URLEncoder.encode(options.getOriginalFilename(), StandardCharsets.UTF_8);
            uploadPretreatment.putMetadata(Constant.Metadata.CONTENT_DISPOSITION, "attachment;filename=" + downFileName);
        }

        FileInfo fileInfo;
        try {
            fileInfo = uploadPretreatment.upload();
        } catch (FileStorageRuntimeException fsre) {
            log.error(LOG_PREFIX + "[上传] FileStorageRuntimeException >> ", fsre);
            throw new BusinessException(FileErrorCodeEnum.B02001);
        }

        log.info(LOG_PREFIX + "[上传] 正常上传成功 >> successFileInfo={}", fileInfo);
        if (attr == null) {
            attr = new FileAttrExtraRequest();
        }
        return fileMetaService.save(fileInfo, storage, options, attr);
    }

    @Override
    public @NonNull FileDownloadReply downloadById(@Nullable String tenantCode, Long fileMetaId) {
        TenantValidateResult tenant = tenantFacade.validateByCode(tenantCode);
        // 不要直接返回前端关于租户的校验结果，隐藏技术细节
        if (!tenant.isValid()) {
            return new FileDownloadReply(FileErrorCodeEnum.A02006);
        }

        try {
            return TenantContextHolder.callWithContext(
                    new SimpleTenantContext(tenant.getTenantId(), tenant.getTenantName(), tenantCode),
                    /*
                    这里请根据实际业务性质调整
                    有的业务出于安全目的，不能暴露直链，只能通过服务端代理下载后，返回 byte[]
                    有的业务没有限制，上传后文件完全可以直接通过对象存储直链下载，如此还能节约服务端上行带宽
                    有的业务有安全要求，只能通过预签名地址下载
                    但本地存储又没有直链，只能通过文件ID；
                    默认地，此处按【直链为空，使用服务端代理下载；有直链，则302到直链】返回
                     */
                    () -> {
                        FileMetaDTO fileMeta = fileMetaService.getNonnullById(fileMetaId);
                        boolean redirect2DirectUrl = false;
                        byte[] fileBytes = null;
                        if (CharSequenceUtil.isEmpty(fileMeta.getDirectUrl())) {
                            // 没有直链，使用服务端代理下载
                            FileInfo fileInfo = toFileInfo(tenant.getTenantId(), fileMeta);
                            try {
                                fileBytes = fileStorageService.download(fileInfo).bytes();
                            } catch (FileStorageRuntimeException fsre) {
                                log.error(LOG_PREFIX + "[下载] FileStorageRuntimeException >> ", fsre);
                                return new FileDownloadReply(FileErrorCodeEnum.B02002);
                            }
                        } else {
                            /*
                            如果需要使用预签名地址
                            if (fileStorageService.isSupportPresignedUrl(
                                    formatFullPlatform(tenant.getTenantId(), fileMeta.getStorageCode()))) {
                                // 采用预签名地址下载
                                FileInfo fileInfo = toFileInfo(tenant.getTenantId(), fileMeta);
                                DateTime oneHourLater = DateUtil.offsetHour(DateUtil.date(), 1);
                                String preSignedUrl = fileStorageService.generatePresignedUrl(fileInfo, oneHourLater);
                                fileMeta.setDirectUrl(preSignedUrl);
                            }
                             */
                            redirect2DirectUrl = true;
                        }
                        return new FileDownloadReply(FileErrorCodeEnum.OK)
                                .setRedirect2DirectUrl(redirect2DirectUrl)
                                .setFileBytes(fileBytes)
                                .setDirectUrl(fileMeta.getDirectUrl())
                                .setStorageFilename(fileMeta.getStorageFilenameFull());
                    }
            );
        } catch (Exception e) {
            if (e instanceof BusinessException be) throw be;
            log.error(LOG_PREFIX + "[下载] 未知异常 >> ", e);
            return new FileDownloadReply(FileErrorCodeEnum.B02002);
        }
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 格式化日期为路径形式，如：2024/01/01/
     *
     * @param date 任意日期，一般取今日
     */
    private static String formatDatePath(LocalDateTime date) {
        return String.format("%d/%02d/%02d/", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    /**
     * 格式化出完整平台名
     *
     * @param storageCode 存储点编码
     */
    private static String formatFullPlatform(Long tenantId, String storageCode) {
        if (tenantId == null) {
            return storageCode;
        }
        return tenantId + "_" + storageCode;
    }

    /**
     * 转换为 FileInfo 对象
     */
    private static FileInfo toFileInfo(Long tenantId, FileMetaDTO source) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setPlatform(formatFullPlatform(tenantId, source.getStorageCode()));
        fileInfo.setBasePath(source.getStorageBasePath());
        fileInfo.setPath(source.getSubDirPath());
        fileInfo.setFilename(source.getStorageFilenameFull());
        fileInfo.setSize(source.getFileSize());
        fileInfo.setOriginalFilename(source.getOriginalFilenameFull());
        return fileInfo;
    }
}
