package cc.uncarbon.module.tenant.model.query;

import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.base.page.PageQuery;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 租户管理-租户管理-分页查询
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminTenantMetaListQuery implements Serializable, PageQuery {

    @Schema(description = "分页查询参数")
    private PageParam pageParam;

    @Schema(description = "租户编码(关键词)")
    private String code;

    @Schema(description = "租户名称(关键词)")
    private String name;

    @Schema(description = "状态")
    private EnabledStatusEnum status;

}
