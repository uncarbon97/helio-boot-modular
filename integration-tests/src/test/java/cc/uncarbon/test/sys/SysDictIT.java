package cc.uncarbon.test.sys;

import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.enums.DictStatusEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.request.AdminSysDictCategoryUpsertRequest;
import cc.uncarbon.module.sys.model.request.AdminSysDictItemUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysDictItemDTO;
import cc.uncarbon.module.sys.service.SysDictService;
import cc.uncarbon.test.base.BaseIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import cc.uncarbon.module.sys.dal.mapper.SysDictCategoryMapper;
import cc.uncarbon.module.sys.dal.mapper.SysDictItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.uncarbon.module.sys.dal.entity.SysDictItemEntity;
import cc.uncarbon.module.sys.dal.entity.SysDictCategoryEntity;

/**
 * 字典集成测试：DB 字典 CRUD、重复编码拦截、孤儿字典项拦截、内置枚举字典
 */
@Tag("sys")
class SysDictIT extends BaseIntegrationTest {

    @Resource
    private SysDictService sysDictService;

    @Resource
    private SysDictCategoryMapper categoryMapper;
    @Resource
    private SysDictItemMapper itemMapper;


    @Test
    void dictCategoryAndItemLifecycle() {
        String code = "itc" + (System.nanoTime() % 1_000_000_000L);

        Long categoryId = createCategory(code);
        Assertions.assertNotNull(categoryId);

        // 分类编码重复 → A01033
        var dupCat = Assertions.assertThrows(HasRepeatRecordException.class, () -> createCategory(code));
        Assertions.assertEquals(SysErrorCodeEnum.A01033, ((HasRepeatRecordException) dupCat).getErrorCode());

        // 字典项所属分类必须存在
        Assertions.assertThrows(NoRecordException.class, () -> createItem(99_999_999L, code + "i"));

        Long itemId = createItem(categoryId, code + "i");
        Assertions.assertNotNull(itemId);

        // 同分类下字典项编码重复 → A01034
        var dupItem = Assertions.assertThrows(HasRepeatRecordException.class,
                () -> createItem(categoryId, code + "i"));
        Assertions.assertEquals(SysErrorCodeEnum.A01034, ((HasRepeatRecordException) dupItem).getErrorCode());

        // 分类下仍有字典项不能删 → A01043
        var delCat = Assertions.assertThrows(BusinessException.class,
                () -> sysDictService.adminDeleteCategory(List.of(categoryId)));
        Assertions.assertEquals(SysErrorCodeEnum.A01043, ((BusinessException) delCat).getErrorCode());

        // 先删项再删分类
        sysDictService.adminDeleteItem(List.of(itemId));
        sysDictService.adminDeleteCategory(List.of(categoryId));
        Assertions.assertThrows(NoRecordException.class, () -> sysDictService.getCategoryNonnullById(categoryId));
    }

    @Test
    void builtinEnumDictServedFromCache() {
        // SysUserStatusEnum 标注 @EnumDict → 内置字典 user_status
        List<SysDictItemDTO> items = sysDictService.listItemsByCategory("user_status", null);

        Assertions.assertFalse(items.isEmpty());
        Assertions.assertTrue(items.stream().anyMatch(i -> "DISABLED".equals(i.getCode())));
        Assertions.assertTrue(items.stream().anyMatch(i -> "ENABLED".equals(i.getCode())));
    }

    @Test
    void unknownCategoryYieldsEmpty() {
        Assertions.assertEquals(List.of(),
                sysDictService.listItemsByCategory("it-no-such-category", null));
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private Long createCategory(String code) {
        return sysDictService.adminCreateCategory(new AdminSysDictCategoryUpsertRequest()
                .setCode(code).setName("IT分类").setStatus(DictStatusEnum.ENABLED));
    }

    private Long createItem(Long categoryId, String code) {
        return sysDictService.adminCreateItem(new AdminSysDictItemUpsertRequest()
                .setCategoryId(categoryId).setCode(code).setValue("v").setLabel("IT项")
                .setStatus(DictStatusEnum.ENABLED).setSort(99));
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.runIgnored(() -> {
            itemMapper.selectList(new LambdaQueryWrapper<SysDictItemEntity>()
                            .likeRight(SysDictItemEntity::getCode, "itc"))
                    .forEach(i -> itemMapper.deleteById(i.getId()));
            categoryMapper.selectList(new LambdaQueryWrapper<SysDictCategoryEntity>()
                            .likeRight(SysDictCategoryEntity::getCode, "itc"))
                    .forEach(c -> categoryMapper.deleteById(c.getId()));
        });
    }
}
