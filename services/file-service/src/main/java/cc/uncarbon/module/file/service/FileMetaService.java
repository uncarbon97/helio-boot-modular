package cc.uncarbon.module.file.service;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.file.model.internal.FacadeUploadOptions;
import cc.uncarbon.module.file.model.query.AdminFileMetaListQuery;
import cc.uncarbon.module.file.model.request.AdminFileMetaUpsertRequest;
import cc.uncarbon.module.file.model.request.FileAttrExtraRequest;
import cc.uncarbon.module.file.model.valueobj.FileMetaDTO;
import cc.uncarbon.module.file.model.valueobj.FileStorageDTO;
import org.dromara.x.file.storage.core.FileInfo;
import org.jspecify.annotations.NonNull;

import java.util.Collection;

/**
 * 文件元数据
 */
public interface FileMetaService {

    /**
     * 后台管理-分页查询
     */
    PageResult<FileMetaDTO> adminList(AdminFileMetaListQuery query);

    /**
     * 后台管理-新增
     */
    Long adminCreate(AdminFileMetaUpsertRequest request);

    /**
     * 后台管理-修改
     */
    void adminUpdate(AdminFileMetaUpsertRequest request);

    /**
     * 后台管理-删除
     */
    void adminDelete(Collection<Long> ids);

    /**
     * 根据 ID 取详情
     */
    FileMetaDTO getById(Long id);

    /**
     * 根据 ID 取详情，未取到会抛出 {@link NoRecordException}
     */
    FileMetaDTO getNonnullById(Long id) throws NoRecordException;

    /**
     * 根据 SHA256，查找是否已有文件
     */
    FileMetaDTO findByDigestSha256(String sha256);

    /**
     * 上传成功后，保存文件元数据
     */
    FileMetaDTO save(@NonNull FileInfo fileInfo, @NonNull FileStorageDTO storage,
                     @NonNull FacadeUploadOptions options, @NonNull FileAttrExtraRequest attr);

}
