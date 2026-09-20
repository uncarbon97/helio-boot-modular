package cc.uncarbon.module.sys.helper;

import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserRoleRelationMapper;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户、角色助手类
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
    public UserRoleScope getSpecifiedUserRole(Long specifiedUserId) {
        return getSpecifiedUserRole(specifiedUserId, null);
    }

    /**
     * 取指定用户在指定租户下的关联角色信息（仅包含启用状态的角色）
     * <p>
     * tenantId 为 null 时取全局角色快照；用户优先模式下用户在各租户持有不同角色，需按租户限定
     */
    public UserRoleScope getSpecifiedUserRole(Long specifiedUserId, @Nullable Long tenantId) {
        List<Long> userRoleIds = tenantId == null
                ? sysUserRoleRelationMapper.listRoleIdsByUser(specifiedUserId)
                : sysUserRoleRelationMapper.listRoleIdsByUserAndTenant(specifiedUserId, tenantId);
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

    /**
     * 列举不可见角色IDs
     * 租户管理员：列表中不显示超级管理员角色
     * 普通角色：列表中不显示超级管理员、租户管理员角色
     *
     * @return mutable Set，支持外部改变元素
     */
    public Set<Long> listInvisibleRoleIds() {
        UserRoleScope me = getCurrentUserRole();
        // 超级管理员：不限制
        if (me.isSuperAdmin()) {
            return new HashSet<>();
        }
        // 租户管理员：列表中不显示超级管理员角色
        if (me.isTenantAdmin()) {
            return CollUtil.newHashSet(SysConstant.SUPER_ADMIN_ROLE_ID);
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
     * 列举不可见用户IDs
     * 租户管理员：列表中不显示超级管理员用户
     * 普通用户：列表中不显示超级管理员、租户管理员用户
     */
    public Set<Long> listInvisibleUserIds() {
        Set<Long> invisibleRoleIds = listInvisibleRoleIds();
        return sysUserRoleRelationMapper.listUserIdsByRoles(invisibleRoleIds);
    }
}
