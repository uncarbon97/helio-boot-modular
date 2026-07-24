package cc.uncarbon.module.config.satoken;

import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.module.adminapi.helper.RolePermissionCacheHelper;
import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * SA-Token 权限数据源
 */
@RequiredArgsConstructor
@Configuration
public class SaTokenPermProvider implements StpInterface {

    private final RolePermissionCacheHelper rolePermissionCacheHelper;


    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return rolePermissionCacheHelper.getCurrentUserPermissions();
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        UserContext u = UserContextHolder.getContext();
        if (u != null) {
            Collection<String> roleCodes = u.getRoleCodes();
            if (roleCodes instanceof List<String> asList) {
                return asList;
            }
            return new ArrayList<>(roleCodes);
        }
        return List.of();
    }
}
