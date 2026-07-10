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
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
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


    @Override
    public PageResult<FileMetaDTO> adminList(AdminFileMetaListQuery query) {
        Page<FileMetaEntity> entityPage = fileMetaMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<FileMetaEntity>()
                        // 原始存储点ID
                        .eq(Objects.nonNull(query.getStorageId()), FileMetaEntity::getStorageId, query.getStorageId())
                        // 存储点编码
                        .like(CharSequenceUtil.isNotBlank(query.getStorageCode()), FileMetaEntity::getStorageCode, CharSequenceUtil.cleanBlank(query.getStorageCode()))
                        // 存储文件名
                        .like(CharSequenceUtil.isNotBlank(query.getStorageFilename()), FileMetaEntity::getStorageFilename, CharSequenceUtil.cleanBlank(query.getStorageFilename()))
                        // 原始文件名
                        .like(CharSequenceUtil.isNotBlank(query.getOriginalFilename()), FileMetaEntity::getOriginalFilename, CharSequenceUtil.cleanBlank(query.getOriginalFilename()))
                        // 扩展名
                        .like(CharSequenceUtil.isNotBlank(query.getExtendName()), FileMetaEntity::getExtendName, CharSequenceUtil.cleanBlank(query.getExtendName()))
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
                .setStorageFilename(fileInfo.getFilename())
                .setOriginalFilename(fileInfo.getOriginalFilename())
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
