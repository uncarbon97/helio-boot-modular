package cc.uncarbon.module.support;

import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.module.adminapi.helper.RolePermissionCacheHelper;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * SA-Token 配置类
 *
 * @author Uncarbon
 */
@RequiredArgsConstructor
@Component
public class SaTokenConfigurer implements StpInterface, WebMvcConfigurer {

    private final RolePermissionCacheHelper rolePermissionCacheHelper;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注解拦截器
        registry
                .addInterceptor(new SaInterceptor())
                .addPathPatterns("/**");
    }

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
