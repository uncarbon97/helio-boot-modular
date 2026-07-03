package cc.uncarbon.module.file.service.impl;


import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.file.dal.entity.FileStorageEntity;
import cc.uncarbon.module.file.dal.mapper.FileStorageMapper;
import cc.uncarbon.module.file.model.query.AdminFileStorageListQuery;
import cc.uncarbon.module.file.model.request.AdminFileStorageUpsertRequest;
import cc.uncarbon.module.file.model.valueobj.FileStorageDTO;
import cc.uncarbon.module.file.service.FileStorageDataService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 文件存储点
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class FileStorageDataServiceImpl implements FileStorageDataService {

    private final FileStorageMapper fileStorageMapper;


    @Override
    public PageResult<FileStorageDTO> adminList(AdminFileStorageListQuery query) {
        Page<FileStorageEntity> entityPage = fileStorageMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<FileStorageEntity>()
                        // 存储点编码
                        .like(CharSequenceUtil.isNotBlank(query.getCode()), FileStorageEntity::getCode, CharSequenceUtil.cleanBlank(query.getCode()))
                        // 存储点名称
                        .like(CharSequenceUtil.isNotBlank(query.getName()), FileStorageEntity::getName, CharSequenceUtil.cleanBlank(query.getName()))
                        // 存储平台类型
                        .eq(Objects.nonNull(query.getPlatformType()), FileStorageEntity::getPlatformType, query.getPlatformType())
                        // 主存储点标识
                        .eq(Objects.nonNull(query.getPrimaryFlag()), FileStorageEntity::getPrimaryFlag, query.getPrimaryFlag())
                        // 时间区间
                        .between(Objects.nonNull(query.getBeginAt()) && Objects.nonNull(query.getEndAt()), FileStorageEntity::getCreatedAt, query.getBeginAt(), query.getEndAt())
                        // 排序
                        .orderByDesc(FileStorageEntity::getId)
        );
        return convertPage(entityPage);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreate(AdminFileStorageUpsertRequest request) {
        checkRepeat(request);

        request.setId(null);
        var entity = new FileStorageEntity();
        BeanUtil.copyProperties(request, entity);
        // 按需改写字段
        serializeSetting(request, entity);

        fileStorageMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdate(AdminFileStorageUpsertRequest request) {
        checkRepeat(request);

        var entity = new FileStorageEntity();
        BeanUtil.copyProperties(request, entity);
        // 按需改写字段
        serializeSetting(request, entity);

        fileStorageMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        fileStorageMapper.deleteByIds(ids);
    }

    @Override
    public FileStorageDTO getById(Long id) {
        if (id == null) return null;
        var entity = fileStorageMapper.selectById(id);
        return convertEntity(entity);
    }

    @Override
    public FileStorageDTO getNonnullById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(getById(id));
    }

    @Override
    public FileStorageDTO getByStorageCode(String storageCode) {
        var entity = fileStorageMapper.selectOne(new LambdaQueryWrapper<FileStorageEntity>()
                .eq(FileStorageEntity::getCode, storageCode)
        );
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
    private FileStorageDTO convertEntity(FileStorageEntity entity) {
        if (entity == null) return null;

        var ret = new FileStorageDTO();
        BeanUtil.copyProperties(entity, ret);
        // 按需改写字段
        return ret;
    }

    /**
     * 实体转值对象
     */
    private List<FileStorageDTO> convertList(List<FileStorageEntity> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }
        return entityList.stream().map(this::convertEntity).toList();
    }

    /**
     * 实体转值对象
     */
    private PageResult<FileStorageDTO> convertPage(Page<FileStorageEntity> entityPage) {
        return new PageResult<FileStorageDTO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                .setRecords(convertList(entityPage.getRecords()));
    }

    /**
     * 检查是否存在重复
     */
    private void checkRepeat(AdminFileStorageUpsertRequest request) {
        var entity = fileStorageMapper.selectOne(new LambdaQueryWrapper<FileStorageEntity>()
                // 仅取主键ID
                .select(FileStorageEntity::getId)
                // 并非原地更新
                .ne(Objects.nonNull(request.getId()), FileStorageEntity::getId, request.getId())
                // 编码相同
                .eq(FileStorageEntity::getCode, request.getCode())
                .last(SQLSegment.LIMIT_1)
        );

        if (entity != null) {
            throw new HasRepeatRecordException("已存在相同的存储点编码");
        }
    }

    /**
     * 对设置类进行序列化
     */
    private static void serializeSetting(AdminFileStorageUpsertRequest request, FileStorageEntity entity) {
        var settingInstance
                = BeanUtil.copyProperties(request.getSettingBody(), request.getPlatformType().getSettingClass());
        entity.setSettingJson(JSONUtil.toJsonStr(settingInstance));
    }

}
