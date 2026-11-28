package cc.uncarbon.module.adminapi.model.valueobj;

import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;


/**
 * 后台管理-用户管理页，控制模式相关功能入口
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AdminSysUserUIConfigVO implements Serializable {

    @Schema(description = "是否显示用户优先模式功能入口")
    private boolean showUserFirstFeaturesFlag = false;

    @Schema(description = "用户管理页「删除」按钮语义：GLOBAL_DELETE=全局删除；TENANT_EXPEL=移出本租户")
    private UserDeleteActionEnum userDeleteAction = UserDeleteActionEnum.GLOBAL_DELETE;


    /**
     * 仅用户优先登录模式开放上述功能入口；用户优先且租户域时，「删除」降级为「移出本租户」
     */
    public static AdminSysUserUIConfigVO of(TenantLoginModeEnum loginMode, boolean platformDomainFlag) {
        var ret = new AdminSysUserUIConfigVO()
                .setShowUserFirstFeaturesFlag(TenantLoginModeEnum.USER_FIRST == loginMode);
        if (ret.isShowUserFirstFeaturesFlag() && !platformDomainFlag) {
            ret.setUserDeleteAction(UserDeleteActionEnum.TENANT_EXPEL);
        }
        return ret;
    }


    /**
     * 用户管理页「删除」按钮的两种语义
     */
    @Getter
    @AllArgsConstructor
    public enum UserDeleteActionEnum {

        /**
         * 全局删除（租户优先模式，或用户优先模式平台自营域）
         */
        GLOBAL_DELETE,

        /**
         * 移出本租户（用户优先模式租户域）
         */
        TENANT_EXPEL,
    }
}
