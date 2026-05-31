package cc.uncarbon.module.sys.model.query;

import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.base.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 系统管理-字典项-分页查询
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysDictItemListQuery implements Serializable, PageQuery {

    @Schema(description = "分页查询参数")
    private PageParam pageParam;

    @Schema(description = "所属分类ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "所属分类ID必填")
    private Long categoryId;

}
