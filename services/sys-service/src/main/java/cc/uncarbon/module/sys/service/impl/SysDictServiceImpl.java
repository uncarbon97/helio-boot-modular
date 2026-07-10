package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.commons.enumdict.EnumDict;
import cc.uncarbon.module.commons.enumdict.EnumDictContributor;
import cc.uncarbon.module.commons.enumdict.EnumDictSpec;
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
import cc.uncarbon.module.sys.model.valueobj.SysDictBuiltinDTO;
import cc.uncarbon.module.sys.model.valueobj.SysDictCategoryDTO;
import cc.uncarbon.module.sys.model.valueobj.SysDictItemDTO;
import cc.uncarbon.module.sys.service.SysDictService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ClassUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;


/**
 * 字典
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysDictServiceImpl implements SysDictService {

    private static final String LOG_PREFIX = "[系统管理][字典]";

    /**
     * 内置枚举字典缓存；key=字典编码，value=字典值对象
     */
    private static final Map<String, SysDictBuiltinDTO> BUILTIN_DICT_CACHE = new ConcurrentSkipListMap<>();

    private final SysDictCategoryMapper sysDictCategoryMapper;
    private final SysDictItemMapper sysDictItemMapper;
    private final ApplicationContext applicationContext;
    private final List<EnumDictContributor> contributors;

    /**
     * 缓存内置枚举字典
     * <p>
     * 扫描 {@link ApplicationContext} 自动解析出的基包（即 {@code @SpringBootApplication} 所在包）下，
     * 所有 {@link BaseEnum} 子类中，标了 {@link EnumDict} 且 {@code enabled=true} 的枚举类
     */
    @PostConstruct
    public void initBuiltinDictCache() {
        log.info(LOG_PREFIX + "开始缓存内置枚举字典到内存");
        Set<Class<?>> classSet = new HashSet<>();
        for (String basePackage : AutoConfigurationPackages.get(applicationContext)) {
            classSet.addAll(ClassUtil.scanPackageBySuper(basePackage, BaseEnum.class));
        }

        // 从类路径下扫描出的枚举类中获取内置字典
        for (Class<?> cls : classSet) {
            EnumDict ann = cls.getAnnotation(EnumDict.class);
            if (ann == null || !ann.enabled()) {
                continue;
            }
            SysDictBuiltinDTO dto = toBuiltinDict(cls, ann);
            BUILTIN_DICT_CACHE.put(dto.getCode(), dto);
        }

        // 从 EnumDictContributor 中获取自定义字典
        for (EnumDictContributor contributor : contributors) {
            for (EnumDictSpec spec : contributor.contribute()) {
                SysDictBuiltinDTO dto = toBuiltinDict(spec);
                BUILTIN_DICT_CACHE.put(dto.getCode(), dto);
            }
        }
        log.info(LOG_PREFIX + "内置枚举字典已缓存到内存，共 {} 个", BUILTIN_DICT_CACHE.size());
    }

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

    /**
     * 内存态分页查询内置枚举字典，按 code/name 模糊匹配
     */
    @Override
    public PageResult<SysDictBuiltinDTO> adminListBuiltin(AdminSysDictCategoryListQuery query) {
        String codeKeyword = CharSequenceUtil.cleanBlank(query.getCode());
        String nameKeyword = CharSequenceUtil.cleanBlank(query.getName());

        List<SysDictBuiltinDTO> filtered = BUILTIN_DICT_CACHE.values().stream()
                // values() 已按 code 自然有序（ConcurrentSkipListMap），过滤保序，直接分页
                .filter(d -> CharSequenceUtil.isBlank(codeKeyword)
                        || CharSequenceUtil.containsIgnoreCase(d.getCode(), codeKeyword))
                .filter(d -> CharSequenceUtil.isBlank(nameKeyword)
                        || CharSequenceUtil.containsIgnoreCase(d.getName(), nameKeyword))
                .toList();

        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();
        return new PageResult<SysDictBuiltinDTO>()
                .setCurrent(pageNum)
                .setSize(pageSize)
                .setTotal(filtered.size())
                .setRecords(CollUtil.page(pageNum - 1, pageSize, filtered));
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

    @Override
    public SysDictCategoryDTO getCategoryNonnullById(Long id) throws NoRecordException {
        if (id == null) {
            throw new NoRecordException();
        }
        return NoRecordException.throwIfNull(convertEntity(sysDictCategoryMapper.selectById(id)));
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

    @Override
    public SysDictItemDTO getItemNonnullById(Long id) throws NoRecordException {
        if (id == null) {
            throw new NoRecordException();
        }
        return NoRecordException.throwIfNull(convertEntity(sysDictItemMapper.selectById(id)));
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

    /**
     * 枚举类 -> 内置字典值对象
     */
    private static SysDictBuiltinDTO toBuiltinDict(Class<?> cls, EnumDict ann) {
        // code 缺省时，去掉 Enum 后缀再下划线小写，如 UserStatusEnum -> user_status
        String simple = cls.getSimpleName();
        if (simple.endsWith("Enum")) {
            simple = simple.substring(0, simple.length() - 4);
        }
        String code = CharSequenceUtil.isBlank(ann.code())
                ? CharSequenceUtil.toUnderlineCase(simple).toLowerCase()
                : ann.code();

        List<SysDictItemDTO> items = new ArrayList<>();
        int sort = 0;
        for (Object constant : cls.getEnumConstants()) {
            BaseEnum<?> be = (BaseEnum<?>) constant;
            items.add(new SysDictItemDTO()
                    .setCode(((Enum<?>) constant).name())
                    .setLabel(be.getLabel())
                    // 归一为 String
                    .setValue(String.valueOf(be.getValue()))
                    .setSort(sort++)
                    .setStatus(EnabledStatusEnum.ENABLED)
            );
        }

        return new SysDictBuiltinDTO()
                .setCode(code)
                .setName(ann.name())
                .setDescription(ann.description())
                .setItems(items);
    }

    /**
     * {@link cc.uncarbon.module.commons.enumdict.EnumDictSpec} -> 内置字典值对象
     */
    private static SysDictBuiltinDTO toBuiltinDict(EnumDictSpec source) {
        List<SysDictItemDTO> items = source.items().stream()
                .map(item -> new SysDictItemDTO()
                        .setCode(item.code())
                        .setLabel(item.label())
                        .setValue(item.value())
                        .setSort(item.sort())
                        .setStatus(EnabledStatusEnum.ENABLED))
                .toList();

        return new SysDictBuiltinDTO()
                .setCode(source.code())
                .setName(source.name())
                .setDescription(source.description())
                .setItems(items);
    }

}
