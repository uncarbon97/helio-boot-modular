package cc.uncarbon.module.sys.model.internal;

import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;


/**
 * 会话内租户切换标记
 * 与生效租户上下文分开存放于 sa-token 会话，避免污染 {@link TenantContext} 契约
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TenantSwitchInfo implements Serializable {

    /**
     * 会话属性名
     */
    public static final String CAMEL_NAME = "tenantSwitchInfo";


    /**
     * 切换前的租户上下文；null=切换前为平台视角（超级管理员）或个人空间
     */
    private TenantContext originalTenantContext;

    /**
     * 切换时刻
     */
    private Instant switchedAt;

}
