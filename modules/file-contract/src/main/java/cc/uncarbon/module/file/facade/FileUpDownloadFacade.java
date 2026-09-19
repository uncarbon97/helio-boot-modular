package cc.uncarbon.module.file.facade;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.file.model.internal.FacadeUploadOptions;
import cc.uncarbon.module.file.model.request.FileAttrExtraRequest;
import cc.uncarbon.module.file.model.response.FileDownloadReply;
import cc.uncarbon.module.file.model.valueobj.FileMetaDTO;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;

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
     * 服务器代理上传（流式）
     * 流只会被读取一次，由底层存储平台直接消费，避免大文件在应用堆内驻留整份副本
     *
     * @param inputStream 文件输入流
     * @param fileSize    文件字节数
     * @param options     上传选项
     * @param attr        附加属性
     */
    FileMetaDTO upload(InputStream inputStream, long fileSize, @NonNull FacadeUploadOptions options,
                       @Nullable FileAttrExtraRequest attr) throws BusinessException;

    /**
     * 根据文件ID下载，支持租户切换
     */
    @NonNull
    FileDownloadReply downloadByTenantAndId(@Nullable String tenantCode, Long fileMetaId);

}
