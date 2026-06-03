package cc.uncarbon.module.config;

import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.module.adminapi.helper.RolePermissionCacheHelper;
import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * 实现权限数据源加载接口
 *
 * @author Uncarbon
 */
@Component
@RequiredArgsConstructor
public class AdminStpInterface implements StpInterface {

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
        Collection<String> roleCodes = UserContextHolder.getUserContext().getRoleCodes();
        if (roleCodes instanceof List<String> asList) {
            return asList;
        }
        return new ArrayList<>(roleCodes);
    }
}
