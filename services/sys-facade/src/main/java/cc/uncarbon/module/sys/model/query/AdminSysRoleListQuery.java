package cc.uncarbon.module.sys.model.query;

import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.base.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 分页查询系统角色
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminSysRoleListQuery implements Serializable, PageQuery {

    @Schema(description = "分页查询参数")
    private PageParam pageParam;

    @Schema(description = "角色名称(关键词)")
    private String name;

    @Schema(description = "角色编码(关键词)")
    private String code;

}
