package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.enums.BaseEnum;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.enumdict.EnumDict;
import cc.uncarbon.module.commons.enumdict.EnumDictSpec;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.AuditNicknameFillerTestSupport;
import cc.uncarbon.module.sys.MybatisPlusTestSupport;
import cc.uncarbon.module.sys.dal.entity.SysDictCategoryEntity;
import cc.uncarbon.module.sys.dal.entity.SysDictItemEntity;
import cc.uncarbon.module.sys.dal.mapper.SysDictCategoryMapper;
import cc.uncarbon.module.sys.dal.mapper.SysDictItemMapper;
import cc.uncarbon.module.sys.enums.DictStatusEnum;
import cc.uncarbon.module.sys.enums.MenuTypeEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.query.AdminSysDictCategoryListQuery;
import cc.uncarbon.module.sys.model.query.AdminSysDictItemListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysDictCategoryUpsertRequest;
import cc.uncarbon.module.sys.model.request.AdminSysDictItemUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysDictBuiltinDTO;
import cc.uncarbon.module.sys.model.valueobj.SysDictCategoryDTO;
import cc.uncarbon.module.sys.model.valueobj.SysDictItemDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

/**
 * {@link SysDictServiceImpl} 字典 CRUD 校验分支测试
 * （@PostConstruct 扫描依赖 Spring 容器，不在单测范围内）
 */
class SysDictServiceImplTest {

    private SysDictCategoryMapper categoryMapper;
    private SysDictItemMapper itemMapper;

    private SysDictServiceImpl service;


    /**
     * 纯 Mockito 环境未启动 MyBatis-Plus，需要手动初始化实体元数据缓存，
     * 否则 LambdaQueryWrapper#select 解析列名时报 can not find lambda cache
     */
    @BeforeAll
    static void initTableInfo() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @BeforeEach
    void setUp() {
        categoryMapper = Mockito.mock(SysDictCategoryMapper.class);
        itemMapper = Mockito.mock(SysDictItemMapper.class);
        service = new SysDictServiceImpl(categoryMapper, itemMapper,
                Mockito.mock(ApplicationContext.class), List.of(),
                AuditNicknameFillerTestSupport.passThrough());
    }

    /**
     * 取出被测类内置字典静态缓存（可变 Map），供用例注入数据
     */
    @SuppressWarnings("unchecked")
    private static Map<String, SysDictBuiltinDTO> builtinCache() throws Exception {
        var field = SysDictServiceImpl.class.getDeclaredField("BUILTIN_DICT_CACHE");
        field.setAccessible(true);
        return (Map<String, SysDictBuiltinDTO>) field.get(null);
    }

    @Test
    void adminListBuiltinOnEmptyCache() {
        var ret = service.adminListBuiltin(new AdminSysDictCategoryListQuery()
                .setPageParam(new PageParam(1, 10)));

        Assertions.assertEquals(0, ret.getTotal());
        Assertions.assertTrue(ret.getRecords().isEmpty());
    }

    @Test
    void adminListCategoryPaging() {
        Mockito.when(categoryMapper.selectPage(Mockito.any(), Mockito.any())).thenAnswer(inv -> {
            Page<SysDictCategoryEntity> page = inv.getArgument(0);
            page.setRecords(List.of(new SysDictCategoryEntity().setId(1L).setCode("c1")));
            page.setTotal(1);
            return page;
        });

        PageResult<?> ret = service.adminListCategory(new AdminSysDictCategoryListQuery()
                .setPageParam(new PageParam(1, 10)));

        Assertions.assertEquals(1, ret.getTotal());
    }

    @Test
    void createCategoryDuplicateRejected() {
        Mockito.when(categoryMapper.selectOne(Mockito.any())).thenReturn(new SysDictCategoryEntity().setId(1L));

        var ex = Assertions.assertThrows(HasRepeatRecordException.class,
                () -> service.adminCreateCategory(category(null, "c1")));
        Assertions.assertEquals(SysErrorCodeEnum.A01033, ex.getErrorCode());
    }

    @Test
    void createCategoryHappyPath() {
        Mockito.when(categoryMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(categoryMapper.insert(Mockito.any(SysDictCategoryEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysDictCategoryEntity.class).setId(9L);
            return 1;
        });

        Assertions.assertEquals(9L, service.adminCreateCategory(category(null, "c1")));
    }

    @Test
    void updateCategoryMissingRejected() {
        Mockito.when(categoryMapper.exists(Mockito.any())).thenReturn(false);

        Assertions.assertThrows(NoRecordException.class,
                () -> service.adminUpdateCategory(category(9L, "c1")));
    }

    @Test
    void deleteCategoryWithItemsRejected() {
        Mockito.when(itemMapper.exists(Mockito.any())).thenReturn(true);

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminDeleteCategory(List.of(9L)));
        Assertions.assertEquals(SysErrorCodeEnum.A01043, ex.getErrorCode());
    }

    @Test
    void deleteCategoryWithoutItemsProceeds() {
        Mockito.when(itemMapper.exists(Mockito.any())).thenReturn(false);

        service.adminDeleteCategory(List.of(9L));

        Mockito.verify(categoryMapper).deleteByIds(List.of(9L));
    }

    @Test
    void createItemDuplicateRejected() {
        Mockito.when(itemMapper.selectOne(Mockito.any())).thenReturn(new SysDictItemEntity().setId(1L));

        var ex = Assertions.assertThrows(HasRepeatRecordException.class,
                () -> service.adminCreateItem(item(null, 5L, "i1")));
        Assertions.assertEquals(SysErrorCodeEnum.A01034, ex.getErrorCode());
    }

    @Test
    void createItemMissingCategoryRejected() {
        Mockito.when(itemMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(categoryMapper.selectById(5L)).thenReturn(null);

        Assertions.assertThrows(NoRecordException.class,
                () -> service.adminCreateItem(item(null, 5L, "i1")));
    }

    @Test
    void createItemHappyPath() {
        Mockito.when(itemMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(categoryMapper.selectById(5L)).thenReturn(new SysDictCategoryEntity().setId(5L));
        Mockito.when(itemMapper.insert(Mockito.any(SysDictItemEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysDictItemEntity.class).setId(9L);
            return 1;
        });

        Assertions.assertEquals(9L, service.adminCreateItem(item(null, 5L, "i1")));
    }

    @Test
    void updateItemMissingRejected() {
        Mockito.when(itemMapper.exists(Mockito.any())).thenReturn(false);

        Assertions.assertThrows(NoRecordException.class,
                () -> service.adminUpdateItem(item(9L, 5L, "i1")));
    }

    @Test
    void nullIdLookupsRejected() {
        Assertions.assertThrows(NoRecordException.class, () -> service.getCategoryNonnullById(null));
        Assertions.assertThrows(NoRecordException.class, () -> service.getItemNonnullById(null));
    }

    @Test
    void listItemsByCategoryMissingCategoryYieldsEmpty() {
        Mockito.when(categoryMapper.selectByCodeAndStatus(Mockito.eq("nope"), Mockito.anyCollection()))
                .thenReturn(null);

        Assertions.assertEquals(List.of(), service.listItemsByCategory("nope", null));
    }

    @Test
    void listItemsByCategoryFromDb() {
        Mockito.when(categoryMapper.selectByCodeAndStatus(Mockito.eq("c1"), Mockito.anyCollection()))
                .thenReturn(new SysDictCategoryEntity().setId(5L));
        Mockito.when(itemMapper.selectList(Mockito.any())).thenReturn(List.of(
                new SysDictItemEntity().setId(1L).setCategoryId(5L).setCode("i1")
                        .setValue("v1").setLabel("项一").setSort(0).setStatus(DictStatusEnum.ENABLED)));

        var ret = service.listItemsByCategory("c1", List.of(DictStatusEnum.ENABLED));

        Assertions.assertEquals(1, ret.size());
        Assertions.assertEquals("v1", ret.get(0).getValue());
    }

    @Test
    void deleteItems() {
        service.adminDeleteItem(List.of(1L, 2L));

        Mockito.verify(itemMapper).deleteByIds(List.of(1L, 2L));
    }

    /**
     * 反射调用私有静态方法 {@code toBuiltinDict(Class, EnumDict)}
     */
    private static SysDictBuiltinDTO toBuiltinDict(Class<?> cls, EnumDict ann) throws Exception {
        var method = SysDictServiceImpl.class.getDeclaredMethod("toBuiltinDict", Class.class, EnumDict.class);
        method.setAccessible(true);
        return (SysDictBuiltinDTO) method.invoke(null, cls, ann);
    }

    /**
     * 反射调用私有静态方法 {@code toBuiltinDict(EnumDictSpec)}
     */
    private static SysDictBuiltinDTO toBuiltinDict(EnumDictSpec spec) throws Exception {
        var method = SysDictServiceImpl.class.getDeclaredMethod("toBuiltinDict", EnumDictSpec.class);
        method.setAccessible(true);
        return (SysDictBuiltinDTO) method.invoke(null, spec);
    }

    /**
     * 反射注入的内置字典缓存为 JVM 级静态共享，用例结束后清空还原，
     * 避免污染 {@link #adminListBuiltinOnEmptyCache} 等依赖空缓存的既有用例
     */
    @AfterEach
    void clearBuiltinCache() throws Exception {
        builtinCache().clear();
    }

    @Test
    void adminListCategoryWithCodeCondition() {
        Mockito.when(categoryMapper.selectPage(Mockito.any(), Mockito.any())).thenAnswer(inv -> {
            Page<SysDictCategoryEntity> page = inv.getArgument(0);
            page.setRecords(List.of(new SysDictCategoryEntity().setId(1L).setCode("c1")));
            page.setTotal(1);
            return page;
        });

        // 分类编码条件非空时走 eq 分支
        PageResult<SysDictCategoryDTO> ret = service.adminListCategory(new AdminSysDictCategoryListQuery()
                .setPageParam(new PageParam(1, 10)).setCode(" c1 "));

        Assertions.assertEquals(1, ret.getTotal());
        Assertions.assertEquals("c1", ret.getRecords().get(0).getCode());
    }

    @Test
    void adminListBuiltinFiltersAndPages() throws Exception {
        Map<String, SysDictBuiltinDTO> cache = builtinCache();
        cache.put("tt_a", new SysDictBuiltinDTO().setCode("tt_a").setName("甲"));
        cache.put("tt_b", new SysDictBuiltinDTO().setCode("tt_b").setName("乙"));
        cache.put("tt_c", new SysDictBuiltinDTO().setCode("tt_c").setName("丙乙"));

        // 无条件全量
        Assertions.assertEquals(3, service.adminListBuiltin(query(null, null)).getTotal());
        // 按编码模糊命中
        Assertions.assertEquals(1, service.adminListBuiltin(query("tt_a", null)).getTotal());
        // 按名称模糊命中多个
        Assertions.assertEquals(2, service.adminListBuiltin(query(null, "乙")).getTotal());
        // 组合条件
        Assertions.assertEquals(1, service.adminListBuiltin(query("tt_c", "丙")).getTotal());
        // 内存分页：第 2 页每页 2 条 → 仅 1 条
        var page2 = service.adminListBuiltin(new AdminSysDictCategoryListQuery()
                .setPageParam(new PageParam(2, 2)));
        Assertions.assertEquals(3, page2.getTotal());
        Assertions.assertEquals(1, page2.getRecords().size());
    }

    @Test
    void listItemsByCategoryBuiltinCacheHitAndEmptyFallback() throws Exception {
        Map<String, SysDictBuiltinDTO> cache = builtinCache();
        cache.put("tt_hit", new SysDictBuiltinDTO().setCode("tt_hit").setName("内置")
                .setItems(List.of(new SysDictItemDTO().setCode("i1").setValue("v1"))));
        cache.put("tt_empty", new SysDictBuiltinDTO().setCode("tt_empty").setName("空"));

        // 缓存命中且有字典项 → 直接返回，不查库
        Assertions.assertEquals(1, service.listItemsByCategory("tt_hit", null).size());
        Mockito.verify(categoryMapper, Mockito.never()).selectByCodeAndStatus(Mockito.eq("tt_hit"), Mockito.anyCollection());

        // 缓存命中但无字典项 → 回落到数据库，分类不存在时返回空列表
        Mockito.when(categoryMapper.selectByCodeAndStatus(Mockito.eq("tt_empty"), Mockito.anyCollection()))
                .thenReturn(null);
        Assertions.assertEquals(List.of(), service.listItemsByCategory("tt_empty", null));
    }

    @Test
    void listItemsByCategoryWithoutStatusFilter() {
        Mockito.when(categoryMapper.selectByCodeAndStatus(Mockito.eq("c1"), Mockito.anyCollection()))
                .thenReturn(new SysDictCategoryEntity().setId(5L));
        Mockito.when(itemMapper.selectList(Mockito.any())).thenReturn(List.of(
                new SysDictItemEntity().setId(1L).setCategoryId(5L).setCode("i1").setStatus(DictStatusEnum.DEPRECATED)));

        // itemStatuses 为空 → 跳过状态 in 条件
        var ret = service.listItemsByCategory("c1", null);

        Assertions.assertEquals(1, ret.size());
        Assertions.assertEquals(DictStatusEnum.DEPRECATED, ret.get(0).getStatus());
    }

    @Test
    void updateCategoryDuplicateRejected() {
        Mockito.when(categoryMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(categoryMapper.selectOne(Mockito.any())).thenReturn(new SysDictCategoryEntity().setId(1L));

        var ex = Assertions.assertThrows(HasRepeatRecordException.class,
                () -> service.adminUpdateCategory(category(9L, "c1")));
        Assertions.assertEquals(SysErrorCodeEnum.A01033, ex.getErrorCode());
    }

    @Test
    void updateCategoryHappyPath() {
        Mockito.when(categoryMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(categoryMapper.selectOne(Mockito.any())).thenReturn(null);

        service.adminUpdateCategory(category(9L, "c1"));

        Mockito.verify(categoryMapper).updateById(Mockito.any(SysDictCategoryEntity.class));
    }

    @Test
    void updateItemDuplicateRejected() {
        Mockito.when(itemMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(itemMapper.selectOne(Mockito.any())).thenReturn(new SysDictItemEntity().setId(1L));

        var ex = Assertions.assertThrows(HasRepeatRecordException.class,
                () -> service.adminUpdateItem(item(9L, 5L, "i1")));
        Assertions.assertEquals(SysErrorCodeEnum.A01034, ex.getErrorCode());
    }

    @Test
    void updateItemHappyPath() {
        Mockito.when(itemMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(itemMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(categoryMapper.selectById(5L)).thenReturn(new SysDictCategoryEntity().setId(5L));

        service.adminUpdateItem(item(9L, 5L, "i1"));

        Mockito.verify(itemMapper).updateById(Mockito.any(SysDictItemEntity.class));
    }

    @Test
    void adminListItemPaging() {
        Mockito.when(itemMapper.selectPage(Mockito.any(), Mockito.any())).thenAnswer(inv -> {
            Page<SysDictItemEntity> page = inv.getArgument(0);
            page.setRecords(List.of(new SysDictItemEntity().setId(1L).setCategoryId(5L).setCode("i1")));
            page.setTotal(1);
            return page;
        });

        var ret = service.adminListItem(new AdminSysDictItemListQuery()
                .setPageParam(new PageParam(1, 10)).setCategoryId(5L));

        Assertions.assertEquals(1, ret.getTotal());
        Assertions.assertEquals("i1", ret.getRecords().get(0).getCode());
    }

    @Test
    void getNonnullLookupsFoundAndMissing() {
        Mockito.when(categoryMapper.selectById(5L)).thenReturn(new SysDictCategoryEntity().setId(5L).setCode("c1"));
        Mockito.when(itemMapper.selectById(9L)).thenReturn(null);

        // 命中 → 转换返回
        Assertions.assertEquals("c1", service.getCategoryNonnullById(5L).getCode());
        // 字典项缺失 → NoRecordException
        Assertions.assertThrows(NoRecordException.class, () -> service.getItemNonnullById(9L));
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    @Test
    void toBuiltinDictFromAnnotatedEnum() throws Exception {
        // 走 sys-contract 真实注解枚举：code 缺省从类名推导 user_status 类似规则
        var dto = toBuiltinDict(MenuTypeEnum.class, MenuTypeEnum.class.getAnnotation(EnumDict.class));

        Assertions.assertEquals("menu_type", dto.getCode());
        Assertions.assertEquals(4, dto.getItems().size());
        SysDictItemDTO first = dto.getItems().get(0);
        Assertions.assertEquals("DIR", first.getCode());
        Assertions.assertEquals("目录", first.getLabel());
        Assertions.assertEquals("0", first.getValue());
        Assertions.assertEquals(DictStatusEnum.ENABLED, first.getStatus());
    }

    @Test
    void toBuiltinDictDeprecatedConstantMarksDeprecated() throws Exception {
        var dto = toBuiltinDict(TstStatusEnum.class, TstStatusEnum.class.getAnnotation(EnumDict.class));

        Assertions.assertEquals("tst_status", dto.getCode());
        Assertions.assertEquals(DictStatusEnum.ENABLED, dto.getItems().get(0).getStatus());
        // 标了 @Deprecated 的枚举常量归一为过时状态
        Assertions.assertEquals(DictStatusEnum.DEPRECATED, dto.getItems().get(1).getStatus());
    }

    @Test
    void toBuiltinDictFromSpec() throws Exception {
        var spec = new EnumDictSpec("sp_code", "规格", "描述", List.of(
                new EnumDictSpec.Item("c1", "v1", "标签一", 0),
                new EnumDictSpec.Item("c2", "v2", "标签二", 1)));

        var dto = toBuiltinDict(spec);

        Assertions.assertEquals("sp_code", dto.getCode());
        Assertions.assertEquals("规格", dto.getName());
        Assertions.assertEquals(2, dto.getItems().size());
        Assertions.assertEquals("v2", dto.getItems().get(1).getValue());
        Assertions.assertEquals(DictStatusEnum.ENABLED, dto.getItems().get(0).getStatus());
    }

    private AdminSysDictCategoryListQuery query(String code, String name) {
        return new AdminSysDictCategoryListQuery()
                .setPageParam(new PageParam(1, 10)).setCode(code).setName(name);
    }

    /**
     * 测试用标注字典枚举：第二个常量标了 {@link Deprecated}
     */
    @EnumDict(code = "tst_status", name = "测试状态")
    private enum TstStatusEnum implements BaseEnum<Integer> {

        OK(1, "正常"),

        @Deprecated
        OLD(2, "旧");

        private final Integer value;
        private final String label;

        TstStatusEnum(Integer value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override
        public Integer getValue() {
            return value;
        }

        @Override
        public String getLabel() {
            return label;
        }
    }

    private AdminSysDictCategoryUpsertRequest category(Long id, String code) {
        return new AdminSysDictCategoryUpsertRequest()
                .setId(id).setCode(code).setName("分类").setStatus(DictStatusEnum.ENABLED);
    }

    private AdminSysDictItemUpsertRequest item(Long id, Long categoryId, String code) {
        return new AdminSysDictItemUpsertRequest()
                .setId(id).setCategoryId(categoryId).setCode(code).setValue("v").setLabel("l")
                .setStatus(DictStatusEnum.ENABLED).setSort(0);
    }
}
