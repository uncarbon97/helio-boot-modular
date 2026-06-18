package cc.uncarbon.module.file.service;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.file.model.query.AdminFileStorageListQuery;
import cc.uncarbon.module.file.model.request.AdminFileStorageUpsertRequest;
import cc.uncarbon.module.file.model.valueobj.FileStorageDTO;

import java.util.Collection;

/**
 * 文件存储点
 */
public interface FileStorageDataService {

    /**
     * 后台管理-分页查询
     */
    PageResult<FileStorageDTO> adminList(AdminFileStorageListQuery query);

    /**
     * 后台管理-新增
     */
    Long adminCreate(AdminFileStorageUpsertRequest request);

    /**
     * 后台管理-修改
     */
    void adminUpdate(AdminFileStorageUpsertRequest request);

    /**
     * 后台管理-删除
     */
    void adminDelete(Collection<Long> ids);

    /**
     * 根据 ID 取详情
     */
    FileStorageDTO getById(Long id);

    /**
     * 根据 ID 取详情，未取到会抛出 {@link NoRecordException}
     */
    FileStorageDTO getNonnullById(Long id) throws NoRecordException;

    /**
     * 根据存储点代码取详情
     */
    FileStorageDTO getByStorageCode(String storageCode);

}
