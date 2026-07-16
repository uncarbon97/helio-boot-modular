package cc.uncarbon.module.tenant.model.valueobj;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Set;

/**
 * 租户套餐绑定菜单结果
 * 用于调用方按需更新缓存
 */
@Accessors(chain = true)
@RequiredArgsConstructor
@Data
public class TenantPackageBindMenuResult {

    @Schema(description = "租户套餐ID")
    private final Long packageId;

    @Schema(description = "租户角色ID集合映射；key=租户ID，value=租户内角色ID集合")
    private final Map<Long, Set<Long>> tenantRoleIdsMap;

}
