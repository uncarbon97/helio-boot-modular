package cc.uncarbon.module.sys.helper;

import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserRoleRelationMapper;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cc.uncarbon.module.sys.util.SysUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户、角色助手类
 * <p>
 * 授权类判定只允许使用：会话快照 roleIds（功能权限），或本类经平台域/归属域作用域解析的结果；
 * 禁止在租户上下文内裸查用户-角色关联做超管/租管判定——超管切入其他租户视角后，
 * 行级过滤会追加 tenant_id=当前租户，平台域 (0,0,0) 绑定随之隐藏、判定失效
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class UserRoleHelper {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleRelationMapper sysUserRoleRelationMapper;


    /**
     * 取当前用户关联角色信息
     */
    public UserRoleScope getCurrentUserRole() {
        return getSpecifiedUserRole(UserContextHolder.getUserId());
    }

    /**
     * 取指定用户关联角色信息（仅包含启用状态的角色）
     * <p>
     * 已禁用的角色一律视为用户不再持有，与登录会话快照、菜单鉴权口径保持一致
     */
    @SneakyThrows
    public UserRoleScope getSpecifiedUserRole(Long specifiedUserId) {
        if (SysUtil.isSuperAdmin(specifiedUserId)) return UserRoleScope.mockSuperAdmin();
        return doResolveUserRole(specifiedUserId);
    }

    /**
     * 列举隐藏角色IDs
     * 租户管理员：列表中不显示超级管理员角色
     * 普通角色：列表中不显示超级管理员、租户管理员角色
     */
    public Set<Long> listHiddenRoleIds() {
        UserRoleScope me = getCurrentUserRole();
        // 超级管理员：不限制
        if (me.isSuperAdmin()) {
            return Set.of();
        }
        // 租户管理员：列表中不显示超级管理员角色
        if (me.isTenantAdmin()) {
            return Set.of(SysConstant.SUPER_ADMIN_ROLE_ID);
        }
        // 普通角色：列表中不显示超级管理员、租户管理员角色
        Set<Long> ret = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                // 仅取主键ID
                .select(SysRoleEntity::getId)
                // 值相符
                .eq(SysRoleEntity::getCode, SysConstant.TENANT_ADMIN_ROLE_CODE)
        ).stream().map(SysRoleEntity::getId).collect(Collectors.toSet());
        ret.add(SysConstant.SUPER_ADMIN_ROLE_ID);
        return ret;
    }

    /**
     * 列举隐藏用户IDs
     * 租户管理员：列表中不显示超级管理员用户
     * 普通用户：列表中不显示超级管理员、租户管理员用户
     */
    public Set<Long> listHiddenUserIds() {
        Set<Long> hiddenRoleIds = listHiddenRoleIds();
        return sysUserRoleRelationMapper.listUserIdsByRoles(hiddenRoleIds);
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private UserRoleScope doResolveUserRole(Long specifiedUserId) {
        List<Long> userRoleIds = sysUserRoleRelationMapper.listRoleIdsByUser(specifiedUserId);
        if (CollUtil.isEmpty(userRoleIds)) {
            return new UserRoleScope(List.of(), List.of());
        }
        List<SysRoleEntity> userRoles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                .in(SysRoleEntity::getId, userRoleIds)
                .eq(SysRoleEntity::getStatus, EnabledStatusEnum.ENABLED)
                .orderByAsc(SysRoleEntity::getId)
        );
        List<Long> enabledRoleIds = userRoles.stream().map(SysRoleEntity::getId).toList();
        return new UserRoleScope(enabledRoleIds, userRoles);
    }

}
