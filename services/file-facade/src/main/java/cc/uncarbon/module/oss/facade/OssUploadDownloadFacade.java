package cc.uncarbon.module.oss.facade;

import cc.uncarbon.framework.core.exception.BusinessException;
import cc.uncarbon.module.oss.model.request.UploadFileAttributeDTO;
import cc.uncarbon.module.oss.model.response.FileMetaBO;
import cc.uncarbon.module.oss.model.response.OssFileDownloadReplyBO;
import lombok.NonNull;

/**
 * 文件上传下载门面
 */
public interface OssUploadDownloadFacade {

    /**
     * 根据哈希值，查找是否已有文件
     */
    FileMetaBO findByHash(String digestSha256);

    /**
     * 正常上传文件到服务端
     *
     * @param fileBytes 文件数据
     * @param attr      附加属性
     */
    FileMetaBO upload(byte[] fileBytes, @NonNull UploadFileAttributeDTO attr) throws BusinessException;

    /**
     * 根据文件ID下载
     *
     * @throws BusinessException 业务异常，如：文件ID无效；原始文件不存在
     */
    OssFileDownloadReplyBO downloadById(Long fileInfoId) throws BusinessException;

    /**
     * 是否为本地存储平台
     * @param storageCode 存储点编码
     */
    boolean isLocalPlatform(String storageCode);

}
