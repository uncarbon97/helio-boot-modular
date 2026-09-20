package cc.uncarbon.module.tenant.model.valueobj;

import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;


/**
 * 登录租户推导结果
 * 由租户门面按登录模式（TENANT_FIRST / USER_FIRST）推导，供登录链路使用
 */
@Accessors(chain = true)
@AllArgsConstructor
@RequiredArgsConstructor
@Data
public class TenantLoginContextResult implements Serializable {

    /**
     * 本次推导所依据的登录模式
     */
    @Schema(description = "登录模式")
    private final TenantLoginModeEnum loginMode;

    /**
     * 登录后生效的租户上下文；null=平台视角（超管）或个人空间（用户优先模式无归属）
     */
    @Schema(description = "租户上下文；null=平台视角或个人空间")
    private TenantContext tenantContext;

    /**
     * 可选租户列表；仅 用户优先模式（USER_FIRST）且用户属多个租户时非空，供前端租户切换器使用
     */
    @Schema(description = "可选租户列表；仅 用户优先模式多归属时非空")
    private List<TenantContext> tenantOptions;

    /**
     * 角色信息是否需按租户限定
     * 用户优先模式进入租户时为 true（同一用户在各租户角色不同）；超管、个人空间、租户优先模式为 false
     */
    @Schema(description = "角色信息是否需按租户限定")
    private boolean tenantScopedRole;

}
