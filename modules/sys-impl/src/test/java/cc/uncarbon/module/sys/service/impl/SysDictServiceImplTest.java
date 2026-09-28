package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.dal.entity.SysDictCategoryEntity;
import cc.uncarbon.module.sys.dal.entity.SysDictItemEntity;
import cc.uncarbon.module.sys.dal.mapper.SysDictCategoryMapper;
import cc.uncarbon.module.sys.dal.mapper.SysDictItemMapper;
import cc.uncarbon.module.sys.enums.DictStatusEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.query.AdminSysDictCategoryListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysDictCategoryUpsertRequest;
import cc.uncarbon.module.sys.model.request.AdminSysDictItemUpsertRequest;
import cc.uncarbon.module.sys.MybatisPlusTestSupport;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import java.util.List;

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
                Mockito.mock(ApplicationContext.class), List.of());
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

        Mockito.verify(itemMapper).delete(Mockito.any());
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

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
