package cc.uncarbon.module.tenant.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Set;


/**
 * 租户套餐-后台管理-绑定租户套餐菜单
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminTenantPackageBindMenuRequest implements Serializable {

    @Schema(description = "菜单Ids(空=清理关联关系后不再绑定任何菜单)")
    private Set<Long> menuIds;

    @Schema(description = "套餐ID", hidden = true)
    private Long packageId;

}
