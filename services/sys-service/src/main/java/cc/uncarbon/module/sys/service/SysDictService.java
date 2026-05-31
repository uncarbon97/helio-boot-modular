package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.model.query.AdminSysDictCategoryListQuery;
import cc.uncarbon.module.sys.model.query.AdminSysDictItemListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysDictCategoryUpsertRequest;
import cc.uncarbon.module.sys.model.request.AdminSysDictItemUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysDictCategoryDTO;
import cc.uncarbon.module.sys.model.valueobj.SysDictItemDTO;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * 字典
 */
public interface SysDictService {

    /**
     * 系统管理-分页查询字典分类
     */
    PageResult<SysDictCategoryDTO> adminListCategory(AdminSysDictCategoryListQuery query);

    /**
     * 系统管理-新增字典分类
     */
    Long adminCreateCategory(AdminSysDictCategoryUpsertRequest request);

    /**
     * 系统管理-修改字典分类
     */
    void adminUpdateCategory(AdminSysDictCategoryUpsertRequest request);

    /**
     * 系统管理-删除字典分类
     */
    void adminDeleteCategory(Collection<Long> ids);

    /**
     * 系统管理-分页查询字典项
     */
    PageResult<SysDictItemDTO> adminListItem(AdminSysDictItemListQuery query);

    /**
     * 系统管理-新增字典项
     */
    Long adminCreateItem(AdminSysDictItemUpsertRequest request);

    /**
     * 系统管理-修改字典项
     */
    void adminUpdateItem(AdminSysDictItemUpsertRequest request);

    /**
     * 系统管理-删除字典项
     */
    void adminDeleteItem(Collection<Long> ids);

    /**
     * 根据分类编码，查询下属字典项
     *
     * @param itemStatus 可以指定字典项状态
     */
    List<SysDictItemDTO> listItemsByCategory(@NonNull String categoryCode, @Nullable EnabledStatusEnum itemStatus);

}
