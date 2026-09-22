package cc.uncarbon.module.file.service.impl;


import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.file.dal.entity.FileMetaEntity;
import cc.uncarbon.module.file.dal.mapper.FileMetaMapper;
import cc.uncarbon.module.file.model.internal.FacadeUploadOptions;
import cc.uncarbon.module.file.model.query.AdminFileMetaListQuery;
import cc.uncarbon.module.file.model.request.FileAttrExtraRequest;
import cc.uncarbon.module.file.model.valueobj.FileMetaDTO;
import cc.uncarbon.module.file.model.valueobj.FileStorageDTO;
import cc.uncarbon.module.file.service.FileMetaService;
import cc.uncarbon.module.file.storage.DynamicFileStorageRegistrar;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 文件元数据
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class FileMetaServiceImpl implements FileMetaService {

    private final FileMetaMapper fileMetaMapper;
    private final FileStorageService fileStorageService;


    @Override
    public PageResult<FileMetaDTO> adminList(AdminFileMetaListQuery query) {
        Page<FileMetaEntity> entityPage = fileMetaMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<FileMetaEntity>()
                        // 存储点编码
                        .eq(CharSequenceUtil.isNotBlank(query.getStorageCode()), FileMetaEntity::getStorageCode, CharSequenceUtil.cleanBlank(query.getStorageCode()))
                        // 扩展名
                        .eq(CharSequenceUtil.isNotBlank(query.getExtendName()), FileMetaEntity::getExtendName, CharSequenceUtil.cleanBlank(query.getExtendName()))
                        // 文件主分类
                        .like(CharSequenceUtil.isNotBlank(query.getCategory()), FileMetaEntity::getCategory, CharSequenceUtil.cleanBlank(query.getCategory()))
                        // 时间区间
                        .between(Objects.nonNull(query.getBeginAt()) && Objects.nonNull(query.getEndAt()), FileMetaEntity::getCreatedAt, query.getBeginAt(), query.getEndAt())
                        // 排序
                        .orderByDesc(FileMetaEntity::getId)
        );
        return convertPage(entityPage);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        // 尽力删除底层物理文件；失败仅告警，不阻断元数据删除
        for (Long id : ids) {
            FileMetaEntity entity = fileMetaMapper.selectById(id);
            if (entity == null) {
                continue;
            }
            try {
                fileStorageService.delete(toFileInfo(entity));
            } catch (Exception e) {
                log.warn("[文件元数据][删除] 底层物理文件删除失败, id={}, msg={}", id, e.getMessage());
            }
        }
        fileMetaMapper.deleteByIds(ids);
    }

    @Override
    public FileMetaDTO getById(Long id) {
        if (id == null) return null;
        var entity = fileMetaMapper.selectById(id);
        return convertEntity(entity);
    }

    @Override
    public FileMetaDTO getNonnullById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(getById(id));
    }

    @Override
    public FileMetaDTO findByDigestSha256(String sha256) {
        if (CharSequenceUtil.isBlank(sha256)) return null;
        var entity = fileMetaMapper.selectOne(new LambdaQueryWrapper<FileMetaEntity>()
                .eq(FileMetaEntity::getDigestSha256, sha256)
                .last(SQLSegment.LIMIT_1)
        );
        return convertEntity(entity);
    }

    @Override
    public FileMetaDTO save(@NonNull FileInfo fileInfo, @NonNull FileStorageDTO storage,
                            @NonNull FacadeUploadOptions options, @NonNull FileAttrExtraRequest attr) {
        var entity = new FileMetaEntity()
                .setStorageId(storage.getId())
                .setStorageCode(storage.getCode())
                .setStorageBasePath(fileInfo.getBasePath())
                .setSubDirPath(fileInfo.getPath())
                // 落库文件名不含扩展名，读取时经 xxxFull 拼回
                .setStorageFilename(FileNameUtil.mainName(fileInfo.getFilename()))
                .setOriginalFilename(FileNameUtil.mainName(fileInfo.getOriginalFilename()))
                .setExtendName(fileInfo.getExt())
                .setFileSize(fileInfo.getSize())
                .setDigestSha256(options.getDigestSha256())
                .setCategory(attr.getCategory())
                .setDirectUrl(fileInfo.getUrl());
        fileMetaMapper.insert(entity);
        return convertEntity(entity);
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 按元数据重建底层存储 FileInfo，用于物理删除
     * 平台完整名与上传侧约定一致：tenantId_storageCode
     */
    private FileInfo toFileInfo(FileMetaEntity entity) {
        String filename = entity.getStorageFilename()
                + (CharSequenceUtil.isBlank(entity.getExtendName()) ? "" : "." + entity.getExtendName());
        return new FileInfo()
                .setPlatform(DynamicFileStorageRegistrar.formatFullPlatform(entity.getTenantId(), entity.getStorageCode()))
                .setBasePath(entity.getStorageBasePath())
                .setPath(entity.getSubDirPath())
                .setFilename(filename)
                .setUrl(entity.getDirectUrl());
    }

    /**
     * 实体转值对象
     */
    private FileMetaDTO convertEntity(FileMetaEntity entity) {
        if (entity == null) return null;

        var ret = new FileMetaDTO();
        BeanUtil.copyProperties(entity, ret);
        // 按需改写字段
        return ret;
    }

    /**
     * 实体转值对象
     */
    private List<FileMetaDTO> convertList(List<FileMetaEntity> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }
        return entityList.stream().map(this::convertEntity).toList();
    }

    /**
     * 实体转值对象
     */
    private PageResult<FileMetaDTO> convertPage(Page<FileMetaEntity> entityPage) {
        return new PageResult<FileMetaDTO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                .setRecords(convertList(entityPage.getRecords()));
    }

}
