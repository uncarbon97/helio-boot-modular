package cc.uncarbon.module.sys.model.valueobj;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 后台管理-登录页配置
 * 前端租户相关 UI 行为的唯一真源（是否渲染租户编码输入框、是否渲染登录后租户切换器），
 * 禁止前端本地硬编码开关常量
 */
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminAuthLoginConfigVO implements Serializable {

    @Schema(description = "是否启用多租户隔离")
    private Boolean tenantEnabled;

    @Schema(description = "登录模式：TENANT_FIRST=租户优先（同账号可跨租户共存）；USER_FIRST=用户优先（账号全局唯一）")
    private String loginMode;

    @Schema(description = "登录页是否显示租户编码输入框")
    private Boolean showTenantInput;

}
