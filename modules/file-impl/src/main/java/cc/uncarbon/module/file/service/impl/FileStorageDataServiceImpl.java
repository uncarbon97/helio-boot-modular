package cc.uncarbon.module.file.service.impl;


import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.file.dal.entity.FileMetaEntity;
import cc.uncarbon.module.file.dal.entity.FileStorageEntity;
import cc.uncarbon.module.file.dal.mapper.FileMetaMapper;
import cc.uncarbon.module.file.dal.mapper.FileStorageMapper;
import cc.uncarbon.module.file.errorcode.FileErrorCodeEnum;
import cc.uncarbon.module.file.model.query.AdminFileStorageListQuery;
import cc.uncarbon.module.file.model.request.AdminFileStorageUpsertRequest;
import cc.uncarbon.module.file.model.valueobj.FileStorageDTO;
import cc.uncarbon.module.file.service.FileStorageDataService;
import cc.uncarbon.module.file.storage.event.FileStorageChangedEvent;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final FileMetaMapper fileMetaMapper;
    private static final JsonMapper JSON_MAPPER = new JsonMapper();
    private final ApplicationEventPublisher eventPublisher;


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
                        // 排序
                        .orderByDesc(FileStorageEntity::getId)
        );
        if (CollUtil.isNotEmpty(entityPage.getRecords())) {
            // 列表查询时不必返回配置属性
            entityPage.getRecords().forEach(item -> item.setSettingJson(null));
        }
        return convertPage(entityPage);
    }

    /**
     * 后台管理-下拉框数据
     */
    @Override
    public List<FileStorageDTO> adminListSelectOption() {
        List<FileStorageEntity> entityList = fileStorageMapper.selectList(new LambdaQueryWrapper<FileStorageEntity>()
                // 只取特定字段
                .select(FileStorageEntity::getId, FileStorageEntity::getName, FileStorageEntity::getCode)
                // 排序
                .orderByAsc(FileStorageEntity::getId)
        );
        return convertList(entityList);
    }

    /**
     * 把配置属性转换成JSON字符串
     */
    @SneakyThrows
    private static void serializeSetting(AdminFileStorageUpsertRequest request, FileStorageEntity entity) {
        if (request.getSettingBody() != null) {
            var settingInstance
                    = BeanUtil.copyProperties(request.getSettingBody(), request.getPlatformType().getSettingClass());
            entity.setSettingJson(JSON_MAPPER.writeValueAsString((settingInstance)));
        }
    }

    /**
     * 从JSON字符串解析出配置属性类
     */
    private static void deserializeSetting(FileStorageDTO ret, FileStorageEntity entity) {
        if (JSONUtil.isTypeJSONObject(entity.getSettingJson())) {
            try {
                var settingInstance = JSON_MAPPER.readValue(
                        entity.getSettingJson(), entity.getPlatformType().getSettingClass());
                ret.setSettingBody(settingInstance);
            } catch (JsonProcessingException jpe) {
                log.error("[文件存储点]反序列化配置属性类失败 >> {}", jpe.getMessage());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreate(AdminFileStorageUpsertRequest request) {
        checkRepeat(request);
        checkPrimaryFlag(request);

        request.setId(null);
        var entity = new FileStorageEntity();
        BeanUtil.copyProperties(request, entity);
        // 按需改写字段
        serializeSetting(request, entity);

        fileStorageMapper.insert(entity);
        publishChangedEvent(FileStorageChangedEvent.ChangeType.CREATE);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdate(AdminFileStorageUpsertRequest request) {
        checkRepeat(request);
        checkPrimaryFlag(request);

        // 已有文件挂载的存储点不可改编码，否则存量文件的 storageCode 全部悬空
        var old = fileStorageMapper.selectById(request.getId());
        NoRecordException.throwIfNull(old);
        if (!CharSequenceUtil.equals(old.getCode(), request.getCode())) {
            checkStorageNotInUse(old.getCode());
        }

        var entity = new FileStorageEntity();
        BeanUtil.copyProperties(request, entity);
        // 按需改写字段
        serializeSetting(request, entity);

        fileStorageMapper.updateById(entity);
        publishChangedEvent(FileStorageChangedEvent.ChangeType.UPDATE);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        checkNotPrimary(ids);
        // 仍有文件引用的存储点不可删除，否则存量文件无法再下载
        List<String> codes = fileStorageMapper.selectList(new LambdaQueryWrapper<FileStorageEntity>()
                        .select(FileStorageEntity::getCode)
                        .in(FileStorageEntity::getId, ids))
                .stream().map(FileStorageEntity::getCode).toList();
        if (CollUtil.isNotEmpty(codes)) {
            checkStorageNotInUse(codes.toArray(String[]::new));
        }
        fileStorageMapper.deleteByIds(ids);
        publishChangedEvent(FileStorageChangedEvent.ChangeType.DELETE);
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

    @Override
    public FileStorageDTO getPrimary() {
        var entity = fileStorageMapper.selectOne(new LambdaQueryWrapper<FileStorageEntity>()
                // 主存储点标识
                .eq(FileStorageEntity::getPrimaryFlag, YesOrNoEnum.YES)
                .last(SQLSegment.LIMIT_1)
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
        deserializeSetting(ret, entity);
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
            throw new HasRepeatRecordException(FileErrorCodeEnum.A02007);
        }
    }

    /**
     * 检查待删除的存储点中是否包含主存储点
     */
    private void checkNotPrimary(Collection<Long> ids) {
        var entity = fileStorageMapper.selectOne(new LambdaQueryWrapper<FileStorageEntity>()
                // 仅取主键ID
                .select(FileStorageEntity::getId)
                // 待删除的记录
                .in(FileStorageEntity::getId, ids)
                // 是主存储点
                .eq(FileStorageEntity::getPrimaryFlag, YesOrNoEnum.YES)
                .last(SQLSegment.LIMIT_1)
        );

        if (entity != null) {
            throw new BusinessException(FileErrorCodeEnum.A02009);
        }
    }

    /**
     * 检查存储点编码是否仍被文件元数据引用
     */
    private void checkStorageNotInUse(String... storageCodes) {
        if (ArrayUtil.isEmpty(storageCodes)) {
            return;
        }
        boolean inUse = fileMetaMapper.exists(new LambdaQueryWrapper<FileMetaEntity>()
                .select(FileMetaEntity::getId)
                .in(FileMetaEntity::getStorageCode, storageCodes)
                .last(SQLSegment.LIMIT_1)
        );
        if (inUse) {
            throw new BusinessException(FileErrorCodeEnum.A02010);
        }
    }

    /**
     * 发布存储点变化事件，事务提交后由 DynamicFileStorageRegistrar 动态同步底层存储平台
     */
    private void publishChangedEvent(FileStorageChangedEvent.ChangeType type) {
        eventPublisher.publishEvent(
                new FileStorageChangedEvent(new FileStorageChangedEvent.EventData(type)));
    }

    /**
     * 检查是否已有其他记录被设置为主存储点
     */
    private void checkPrimaryFlag(AdminFileStorageUpsertRequest request) {
        if (request.getPrimaryFlag() == YesOrNoEnum.YES) {
            var entity = fileStorageMapper.selectOne(new LambdaQueryWrapper<FileStorageEntity>()
                    // 仅取主键ID
                    .select(FileStorageEntity::getId)
                    // 并非原地更新
                    .ne(Objects.nonNull(request.getId()), FileStorageEntity::getId, request.getId())
                    // 已是主存储点
                    .eq(FileStorageEntity::getPrimaryFlag, YesOrNoEnum.YES)
                    .last(SQLSegment.LIMIT_1)
            );

            if (entity != null) {
                throw new HasRepeatRecordException(FileErrorCodeEnum.A02008);
            }
        }
    }
}
