package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.model.query.AdminSysDictCategoryListQuery;
import cc.uncarbon.module.sys.model.query.AdminSysDictItemListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysDictCategoryUpsertRequest;
import cc.uncarbon.module.sys.model.request.AdminSysDictItemUpsertRequest;
import cc.uncarbon.module.sys.model.response.SysDictCategoryDTO;
import cc.uncarbon.module.sys.model.response.SysDictItemDTO;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * 字典
 */
public interface SysDictService {

    /**
     * 系统管理-分页列表字典分类
     */
    PageResult<SysDictCategoryDTO> adminListCategory(AdminSysDictCategoryListQuery query);

    /**
     * 系统管理-新增字典分类
     */
    Long adminInsertCategory(AdminSysDictCategoryUpsertRequest request);

    /**
     * 系统管理-修改字典分类
     */
    void adminUpdateCategory(AdminSysDictCategoryUpsertRequest request);

    /**
     * 系统管理-删除字典分类
     */
    void adminDeleteCategory(Collection<Long> ids);

    /**
     * 系统管理-分页列表字典项
     */
    PageResult<SysDictItemDTO> adminListItem(AdminSysDictItemListQuery query);

    /**
     * 系统管理-新增字典项
     */
    Long adminInsertItem(AdminSysDictItemUpsertRequest request);

    /**
     * 系统管理-修改字典项
     */
    void adminUpdateItem(AdminSysDictItemUpsertRequest request);

    /**
     * 系统管理-删除字典项
     */
    void adminDeleteItem(Collection<Long> ids, Long classifiedId);

    /**
     * 列举分类下的字典项
     */
    List<SysDictItemDTO> listItemsByCategory(@NonNull String categoryCode, @Nullable EnabledStatusEnum status);
}
