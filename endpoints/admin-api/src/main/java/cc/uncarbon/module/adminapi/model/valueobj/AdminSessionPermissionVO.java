package cc.uncarbon.module.adminapi.model.valueobj;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collection;

/**
 * 后台管理-当前会话角色与权限
 * 登录后、切换租户后前端统一从这里重取，保持单一来源
 */
@Getter
@Setter
@RequiredArgsConstructor
@Accessors(chain = true)
public class AdminSessionPermissionVO {

    @Schema(description = "角色编码列表")
    private Collection<String> roles;

    @Schema(description = "权限名集合")
    private Collection<String> permissions;
}
