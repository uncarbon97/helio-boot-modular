package cc.uncarbon.module.file.facade;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.file.model.internal.FacadeUploadOptions;
import cc.uncarbon.module.file.model.request.FileAttrExtraRequest;
import cc.uncarbon.module.file.model.response.FileDownloadReply;
import cc.uncarbon.module.file.model.valueobj.FileMetaDTO;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 文件上传、下载门面
 */
public interface FileUpDownloadFacade {

    /**
     * 根据 SHA256，查找是否已有文件
     */
    FileMetaDTO findByDigestSha256(String sha256);

    /**
     * 服务器代理上传
     *
     * @param fileBytes 文件数据
     * @param options   上传选项
     * @param attr      附加属性
     */
    FileMetaDTO upload(byte[] fileBytes, @NonNull FacadeUploadOptions options, @Nullable FileAttrExtraRequest attr)
            throws BusinessException;

    /**
     * 根据文件ID下载，支持租户切换
     */
    @NonNull
    FileDownloadReply downloadById(@Nullable String tenantCode, Long fileMetaId);

    /**
     * 是否为本地存储平台
     *
     * @param storageCode 存储点编码
     */
    boolean isLocalPlatform(String storageCode);

}
