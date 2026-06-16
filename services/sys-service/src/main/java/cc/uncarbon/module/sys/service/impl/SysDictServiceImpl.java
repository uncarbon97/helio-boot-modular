package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.constant.SQLSegment;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.dal.entity.SysDictCategoryEntity;
import cc.uncarbon.module.sys.dal.entity.SysDictItemEntity;
import cc.uncarbon.module.sys.dal.mapper.SysDictCategoryMapper;
import cc.uncarbon.module.sys.dal.mapper.SysDictItemMapper;
import cc.uncarbon.module.sys.model.query.AdminSysDictCategoryListQuery;
import cc.uncarbon.module.sys.model.query.AdminSysDictItemListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysDictCategoryUpsertRequest;
import cc.uncarbon.module.sys.model.request.AdminSysDictItemUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysDictCategoryDTO;
import cc.uncarbon.module.sys.model.valueobj.SysDictItemDTO;
import cc.uncarbon.module.sys.service.SysDictService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;


/**
 * 字典
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysDictServiceImpl implements SysDictService {

    private static final String LOG_PREFIX = "[系统管理][字典]";

    private final SysDictCategoryMapper sysDictCategoryMapper;
    private final SysDictItemMapper sysDictItemMapper;


    @Override
    public PageResult<SysDictCategoryDTO> adminListCategory(AdminSysDictCategoryListQuery query) {
        Page<SysDictCategoryEntity> entityPage = sysDictCategoryMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysDictCategoryEntity>()
                        // 分类编码
                        .eq(CharSequenceUtil.isNotBlank(query.getCode()), SysDictCategoryEntity::getCode, CharSequenceUtil.cleanBlank(query.getCode()))
                        // 排序
                        .orderByDesc(SysDictCategoryEntity::getId)
        );

        return convertPage(entityPage);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreateCategory(AdminSysDictCategoryUpsertRequest request) {
        log.info(LOG_PREFIX + "新增分类 >> {}", request);
        checkRepeat(request);

        request.setId(null);
        var entity = new SysDictCategoryEntity();
        BeanUtil.copyProperties(request, entity);

        sysDictCategoryMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdateCategory(AdminSysDictCategoryUpsertRequest request) {
        log.info(LOG_PREFIX + "修改分类 >> {}", request);
        checkCategoryExistence(request.getId());
        checkRepeat(request);

        var entity = new SysDictCategoryEntity();
        BeanUtil.copyProperties(request, entity);

        sysDictCategoryMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDeleteCategory(Collection<Long> ids) {
        log.info(LOG_PREFIX + "删除分类 >> {}", ids);
        sysDictCategoryMapper.deleteByIds(ids);
    }

    /**
     * 后台管理-分页查询字典分类下的字典项
     */
    @Override
    public PageResult<SysDictItemDTO> adminListItem(AdminSysDictItemListQuery query) {
        Page<SysDictItemEntity> entityPage = sysDictItemMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysDictItemEntity>()
                        // 分类ID
                        .eq(SysDictItemEntity::getCategoryId, query.getCategoryId())
                        // 排序
                        .orderByAsc(SysDictItemEntity::getSort)
        );

        return convertPage(entityPage);
    }

    /**
     * 后台管理-新增字典项
     *
     * @return 主键ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreateItem(AdminSysDictItemUpsertRequest request) {
        log.info(LOG_PREFIX + "新增字典项 >> {}", request);
        checkRepeat(request);

        request.setId(null);
        var entity = new SysDictItemEntity();
        BeanUtil.copyProperties(request, entity);

        sysDictItemMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 后台管理-修改字典项
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdateItem(AdminSysDictItemUpsertRequest request) {
        log.info(LOG_PREFIX + "修改字典项 >> {}", request);
        checkItemExistence(request.getId());
        checkRepeat(request);

        var entity = new SysDictItemEntity();
        BeanUtil.copyProperties(request, entity);

        sysDictItemMapper.updateById(entity);
    }

    /**
     * 后台管理-删除字典项
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDeleteItem(Collection<Long> ids) {
        log.info(LOG_PREFIX + "删除字典项 >> {}", ids);
        sysDictItemMapper.delete(new LambdaQueryWrapper<SysDictItemEntity>()
                .in(SysDictItemEntity::getId, ids)
        );
    }

    /**
     * 列举指定分类编码下的所有启用的字典项
     *
     * @return 存在则返回字典项列表；不存在或没有符合的字典项，均返回空列表
     */
    @Override
    public List<SysDictItemDTO> listItemsByCategory(@Nonnull String categoryCode, @Nullable EnabledStatusEnum itemStatus) {
        SysDictCategoryEntity category =
                sysDictCategoryMapper.selectByCodeAndStatus(categoryCode, EnabledStatusEnum.ENABLED);
        if (Objects.isNull(category)) {
            return List.of();
        }
        return convertList(
                sysDictItemMapper.selectList(new LambdaQueryWrapper<SysDictItemEntity>()
                        // 分类ID
                        .eq(SysDictItemEntity::getCategoryId, category.getId())
                        // 状态
                        .eq(SysDictItemEntity::getStatus, EnabledStatusEnum.ENABLED)
                        // 排序
                        .orderByAsc(SysDictItemEntity::getSort)
                )
        );
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 实体转值对象
     */
    private SysDictCategoryDTO convertEntity(SysDictCategoryEntity entity) {
        if (entity == null) return null;

        var ret = new SysDictCategoryDTO();
        BeanUtil.copyProperties(entity, ret);
        // 按需改写字段

        return ret;
    }

    /**
     * 实体转值对象
     */
    private SysDictItemDTO convertEntity(SysDictItemEntity entity) {
        if (entity == null) return null;

        var ret = new SysDictItemDTO();
        BeanUtil.copyProperties(entity, ret);
        // 按需改写字段

        return ret;
    }

    /**
     * 实体转值对象
     */
    private List<SysDictCategoryDTO> convertList(List<SysDictCategoryEntity> entityList, SysDictCategoryEntity... ignored) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }
        return entityList.stream().map(this::convertEntity).toList();
    }

    /**
     * 实体转值对象
     */
    private List<SysDictItemDTO> convertList(List<SysDictItemEntity> entityList, SysDictItemEntity... ignored) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }
        return entityList.stream().map(this::convertEntity).toList();
    }

    /**
     * 实体转值对象
     */
    private PageResult<SysDictCategoryDTO> convertPage(Page<SysDictCategoryEntity> entityPage, SysDictCategoryEntity... ignored) {
        return new PageResult<SysDictCategoryDTO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                .setRecords(convertList(entityPage.getRecords()));
    }

    /**
     * 实体转值对象
     */
    private PageResult<SysDictItemDTO> convertPage(Page<SysDictItemEntity> entityPage, SysDictItemEntity... ignored) {
        return new PageResult<SysDictItemDTO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                .setRecords(convertList(entityPage.getRecords()));
    }

    /**
     * 检查是否存在重复
     */
    private void checkRepeat(AdminSysDictCategoryUpsertRequest request) throws BusinessException {
        var entity = sysDictCategoryMapper.selectOne(new LambdaQueryWrapper<SysDictCategoryEntity>()
                // 仅取主键ID
                .select(SysDictCategoryEntity::getId)
                // 并非原地更新
                .ne(Objects.nonNull(request.getId()), SysDictCategoryEntity::getId, request.getId())
                // 分类编码相同
                .eq(SysDictCategoryEntity::getCode, request.getCode())
                .last(SQLSegment.LIMIT_1)
        );

        if (entity != null) {
            throw new HasRepeatRecordException("已存在相同的字典分类编码");
        }
    }

    /**
     * 检查是否存在重复
     */
    private void checkRepeat(AdminSysDictItemUpsertRequest request) throws BusinessException {
        var entity = sysDictItemMapper.selectOne(new LambdaQueryWrapper<SysDictItemEntity>()
                .select(SysDictItemEntity::getId)
                // 并非原地更新
                .ne(Objects.nonNull(request.getId()), SysDictItemEntity::getId, request.getId())
                // 分类ID相同
                .eq(SysDictItemEntity::getCategoryId, request.getCategoryId())
                // 字典项编码相同
                .eq(SysDictItemEntity::getCode, request.getCode())
                .last(SQLSegment.LIMIT_1)
        );

        if (entity != null) {
            throw new HasRepeatRecordException("同一分类下，已存在相同的字典项编码");
        }
    }

    /**
     * 检查分类是否存在
     */
    private void checkCategoryExistence(Long id) {
        boolean exists = sysDictCategoryMapper.exists(
                new LambdaQueryWrapper<SysDictCategoryEntity>()
                        .select(SysDictCategoryEntity::getId)
                        .eq(SysDictCategoryEntity::getId, id)
                        .last(SQLSegment.LIMIT_1)
        );
        NoRecordException.throwIfFalse(exists);
    }

    /**
     * 检查字典项是否存在
     */
    private void checkItemExistence(Long id) {
        boolean exists = sysDictItemMapper.exists(
                new LambdaQueryWrapper<SysDictItemEntity>()
                        .select(SysDictItemEntity::getId)
                        .eq(SysDictItemEntity::getId, id)
                        .last(SQLSegment.LIMIT_1)
        );
        NoRecordException.throwIfFalse(exists);
    }

}
