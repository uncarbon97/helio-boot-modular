package cc.uncarbon.module.sys.service;

import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.entity.SysUserEntity;
import cc.uncarbon.module.sys.enums.SysErrorEnum;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.model.interior.UserDeptContainer;
import cc.uncarbon.module.sys.model.interior.UserRoleContainer;
import cc.uncarbon.module.sys.model.request.*;
import cc.uncarbon.module.sys.model.response.SysUserBO;
import cc.uncarbon.module.sys.model.response.SysUserLoginBO;
import cc.uncarbon.module.sys.model.response.VbenAdminUserInfoVO;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.PostConstruct;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

public interface SysUserService {

    /**
     * 系统管理-分页列表
     */
    PageResult<SysUserBO> adminList(PageParam pageParam, AdminListSysUserDTO dto);

    /**
     * 根据 ID 取详情
     *
     * @param id 主键ID
     * @return null or BO
     */
    SysUserBO getOneById(Long id);

    /**
     * 根据 ID 取详情
     *
     * @param id               主键ID
     * @param throwIfInvalidId 是否在 ID 无效时抛出异常
     * @return null or BO
     */
    SysUserBO getOneById(Long id, boolean throwIfInvalidId) throws BusinessException;

    /**
     * 系统管理-新增
     *
     * @return 主键ID
     */
    Long adminInsert(AdminInsertOrUpdateSysUserDTO dto);

    /**
     * 系统管理-编辑
     */
    void adminUpdate(AdminInsertOrUpdateSysUserDTO dto);

    /**
     * 系统管理-删除
     */
    void adminDelete(Collection<Long> ids);

    /**
     * 系统管理-登录
     */
    SysUserLoginBO adminLogin(SysUserLoginDTO dto);

    /**
     * 系统管理-取当前用户信息
     */
    VbenAdminUserInfoVO adminGetCurrentUserInfo();

    /**
     * 系统管理-重置某用户密码
     */
    void adminResetUserPassword(AdminResetSysUserPasswordDTO dto);

    /**
     * 系统管理-修改当前用户密码
     */
    void adminUpdateCurrentUserPassword(AdminUpdateCurrentSysUserPasswordDTO dto);

    /**
     * 系统管理-绑定用户与角色关联关系
     */
    void adminBindRoles(AdminBindUserRoleRelationDTO dto);

    /**
     * 根据用户账号查询
     */
    SysUserEntity getUserByPin(String pin);

    /**
     * 系统管理 - 取指定用户关联角色ID
     *
     * @param userId 用户ID
     * @return 角色Ids
     */
    Set<Long> listRelatedRoleIds(Long userId);

    /**
     * 系统管理 - 取租户用户IDs
     *
     * @param tenantId    租户ID，非主键ID
     * @param statusEnums 仅保留符合指定状态的，可以为null
     */
    List<Long> listUserIdsByTenantId(Long tenantId, Collection<EnabledStatusEnum> statusEnums);

    /**
     * 系统管理-更新当前用户信息资料
     */
    void adminUpdateCurrentUserInfo(AdminUpdateCurrentSysUserInfoDTO dto);

    /**
     * 系统管理-更新当前用户头像
     */
    void adminUpdateCurrentUserAvatar(AdminUpdateCurrentSysUserAvatarDTO dto);

    /**
     * 实体转 BO
     *
     * @param entity       实体
     * @param fillDeptInfo 是否根据实体部门ID，查询关联部门信息并填充到BO
     * @return BO
     */
    default SysUserBO entity2BO(SysUserEntity entity, boolean fillDeptInfo) {
        if (entity == null) {
            return null;
        }

        SysUserBO bo = new SysUserBO();
        BeanUtil.copyProperties(entity, bo);

        // 可以在此处为BO填充字段
        bo.setUsername(entity.getPin());
        if (fillDeptInfo) {
            Optional.ofNullable(sysDeptService.getSpecifiedUserDeptContainer(bo.getId(), false))
                    .map(UserDeptContainer::primaryRelatedDept)
                    .ifPresent(deptInfo -> bo.setDeptId(deptInfo.getId()).setDeptTitle(deptInfo.getTitle()));
        }
        return bo;
    }

    /**
     * 实体 List 转 BO List
     *
     * @param entityList   实体 List
     * @param fillDeptInfo 是否根据实体部门ID，查询关联部门信息并填充到BO
     * @return BO List
     */
    default List<SysUserBO> entityList2BOs(List<SysUserEntity> entityList, boolean fillDeptInfo) {
        if (CollUtil.isEmpty(entityList)) {
            return Collections.emptyList();
        }

        // 深拷贝
        List<SysUserBO> ret = new ArrayList<>(entityList.size());
        entityList.forEach(
                entity -> ret.add(this.entity2BO(entity, fillDeptInfo))
        );

        return ret;
    }

    /**
     * 实体分页转 BO 分页
     *
     * @param entityPage   实体分页
     * @param fillDeptInfo 是否根据实体部门ID，查询关联部门信息并填充到BO
     * @return BO 分页
     */
    default PageResult<SysUserBO> entityPage2BOPage(Page<SysUserEntity> entityPage, boolean fillDeptInfo) {
        return new PageResult<SysUserBO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                .setRecords(this.entityList2BOs(entityPage.getRecords(), fillDeptInfo));
    }

    /**
     * 检查是否已存在相同数据
     *
     * @param dto DTO
     */
    default void checkExistence(AdminInsertOrUpdateSysUserDTO dto) {
        SysUserEntity existingEntity = this.getUserByPin(dto.getUsername());

        if (existingEntity != null && !existingEntity.getId().equals(dto.getId())) {
            throw new BusinessException(400, "已存在相同账号，请重新输入");
        }
    }

    /**
     * 确定本部门及下级部门用户IDs
     * 返回空集合代表不限制
     * 返回[0]或有元素集合，表示有限制
     */
    default Set<Long> determineVisibleDeptUserIds() {
        Set<Long> visibleUserIds = Collections.emptySet();
        UserDeptContainer deptContainer = sysDeptService.getCurrentUserDeptContainer(true);
        if (deptContainer.hasVisibleDepts()) {
            visibleUserIds = sysUserDeptRelationService.listUserIdsByDeptIds(deptContainer.getVisibleDeptIds());
            if (CollUtil.isEmpty(visibleUserIds)) {
                // 【可见部门】没有任何用户ID，直接返回[0]
                return Collections.singleton(BigInteger.ZERO.longValue());
            }
        }
        return visibleUserIds;
    }

    /**
     * 确定不可见用户IDs
     * 租户管理员：列表中不显示超级管理员用户
     * 普通用户：列表中不显示超级管理员、租户管理员用户
     */
    default Set<Long> determineInvisibleUserIds() {
        Set<Long> invisibleRoleIds = sysRoleService.determineInvisibleRoleIds();
        return sysUserRoleRelationService.listUserIdsByRoleIds(invisibleRoleIds);
    }

    /**
     * 数据越权检查
     */
    default void dataScopeCheck(Collection<Long> userIds) {
        Set<Long> visibleUserIds = determineVisibleDeptUserIds();
        Set<Long> invisibleUserIds = determineInvisibleUserIds();
        if (CollUtil.isNotEmpty(visibleUserIds) && !CollUtil.containsAll(visibleUserIds, userIds)
                || CollUtil.isNotEmpty(invisibleUserIds) && CollUtil.containsAny(invisibleUserIds, userIds)) {
            throw new BusinessException(SysErrorEnum.CANNOT_OPERATE_THIS_USER);
        }
    }

    /**
     * 检查并获取租户上下文 bean，无效或被禁用则直接抛出异常
     *
     * @param tenantId 租户ID
     * @return TenantContext
     */
    default TenantContext checkAndGetTenantContext(Long tenantId) throws BusinessException {
        // 查询租户是否仍有效
        SysTenantEntity tenantEntity = sysTenantService.getTenantEntityByTenantId(tenantId);
        if (tenantEntity == null) {
            throw new BusinessException(SysErrorEnum.INVALID_TENANT);
        }

        if (EnabledStatusEnum.DISABLED == tenantEntity.getStatus()) {
            throw new BusinessException(SysErrorEnum.DISABLED_TENANT);
        }

        return TenantContext.builder()
                .tenantId(tenantEntity.getTenantId())
                .tenantName(tenantEntity.getTenantName())
                .build();
    }

    default void updateLastLoginAt(Long userId, LocalDateTime lastLoginAt) {
        SysUserEntity entity = new SysUserEntity();
        entity
                .setLastLoginAt(lastLoginAt)
                .setId(userId);
        sysUserMapper.updateById(entity);
    }

    /**
     * 编辑后台用户信息前检查
     *
     * @param specifiedUserId 被操作用户ID
     * @param statusEnum      用户状态枚举，可以为null
     */
    default void preUpdateCheck(Long specifiedUserId, @Nullable SysUserStatusEnum statusEnum) {
        UserRoleContainer currentUser = sysRoleService.getCurrentUserRoleContainer();
        if (currentUser.isSuperAdmin()) {
            // 超级管理员除禁用自己外为所欲为
            if (statusEnum == SysUserStatusEnum.BANNED && Objects.equals(specifiedUserId, UserContextHolder.getUserId())) {
                throw new BusinessException(SysErrorEnum.CANNOT_OPERATE_SELF_USER);
            }
            return;
        }

        if (Objects.equals(specifiedUserId, UserContextHolder.getUserId())) {
            // 不能动自身用户
            throw new BusinessException(SysErrorEnum.CANNOT_OPERATE_SELF_USER);
        }

        // 目标是超级管理员or租户管理员时，均不能编辑
        UserRoleContainer specifiedUser = sysRoleService.getSpecifiedUserRoleContainer(specifiedUserId);
        if (specifiedUser.isSuperAdmin() || specifiedUser.isTenantAdmin()) {
            throw new BusinessException(SysErrorEnum.CANNOT_OPERATE_THIS_USER);
        }

        dataScopeCheck(Collections.singleton(specifiedUserId));
        // 暂未实现角色层级，一律平级
    }

    /**
     * 删除后台用户前检查
     */
    default void preDeleteCheck(Collection<Long> ids) {
        if (CollUtil.contains(ids, UserContextHolder.getUserId())) {
            // 不能动自身用户
            throw new BusinessException(SysErrorEnum.CANNOT_OPERATE_SELF_USER);
        }

        // 目标是超级管理员时，不能删除
        List<UserRoleContainer> specifiedUsers = ids.stream().map(sysRoleService::getSpecifiedUserRoleContainer).toList();
        if (specifiedUsers.stream().anyMatch(UserRoleContainer::isSuperAdmin)) {
            throw new BusinessException(SysErrorEnum.CANNOT_OPERATE_THIS_USER);
        }

        // 只有超级管理员可以删租户管理员用户
        UserRoleContainer currentUser = sysRoleService.getCurrentUserRoleContainer();
        if (specifiedUsers.stream().anyMatch(UserRoleContainer::isTenantAdmin) && !currentUser.isSuperAdmin()) {
            throw new BusinessException(SysErrorEnum.CANNOT_OPERATE_THIS_USER);
        }

        dataScopeCheck(ids);
        // 暂未实现角色层级，一律平级
    }

    /**
     * 绑定后台用户与角色关联关系前检查
     * 防止越权访问漏洞
     */
    default void preBindUserRoleRelationCheck(AdminBindUserRoleRelationDTO dto) {
        UserRoleContainer currentUser = sysRoleService.getCurrentUserRoleContainer();
        // 是否对自己操作
        boolean selfFlag = Objects.equals(dto.getUserId(), UserContextHolder.getUserId());
        if (currentUser.isSuperAdmin()) {
            // 超级管理员不能去掉自己的超级管理员角色
            if (selfFlag && !CollUtil.contains(dto.getRoleIds(), SysConstant.SUPER_ADMIN_ROLE_ID)) {
                throw new BusinessException(SysErrorEnum.CANNOT_OPERATE_SELF_USER);
            }
            // 也不能赋予其他人超级管理员角色
            if (!selfFlag && CollUtil.contains(dto.getRoleIds(), SysConstant.SUPER_ADMIN_ROLE_ID)) {
                throw new BusinessException(SysErrorEnum.CANNOT_OPERATE_THIS_USER);
            }
            return;
        }

        if (selfFlag) {
            // 不能动自身用户
            throw new BusinessException(SysErrorEnum.CANNOT_OPERATE_SELF_USER);
        }

        // 目标已经是超级管理员or租户管理员时，均不能绑定
        UserRoleContainer specifiedUser = sysRoleService.getSpecifiedUserRoleContainer(dto.getUserId());
        if (specifiedUser.isSuperAdmin() || specifiedUser.isTenantAdmin()) {
            throw new BusinessException(SysErrorEnum.CANNOT_OPERATE_THIS_USER);
        }

        // 超级管理员之外的用户，都需要校验自身角色范围是否满足输入值
        currentUserNotSuperAdmin(dto, currentUser);

        dataScopeCheck(Collections.singleton(dto.getUserId()));
    }

    /**
     * 绑定后台用户与角色关联关系前检查
     * 超级管理员之外的用户，都需要校验自身角色范围是否满足输入值
     * 拆分子方法以降低Cognitive Complexity
     */
    default void currentUserNotSuperAdmin(AdminBindUserRoleRelationDTO dto, UserRoleContainer currentUser) {
        if (CollUtil.isNotEmpty(dto.getRoleIds()) && !currentUser.isSuperAdmin()) {
            boolean overRoles = !CollUtil.containsAll(currentUser.getRelatedRoleIds(), dto.getRoleIds());
            if (overRoles && currentUser.isNotAnyAdmin()) {
                // 普通用户超自身角色授予了；如果当前用户拥有新角色的所有菜单，那么也放行
                Set<Long> grantedMenuIds = sysRoleMenuRelationService.listMenuIdsByRoleIds(currentUser.getRelatedRoleIds());
                Set<Long> needMenuIds = sysRoleMenuRelationService.listMenuIdsByRoleIds(dto.getRoleIds());
                if (!CollUtil.containsAll(grantedMenuIds, needMenuIds)) {
                    throw new BusinessException(SysErrorEnum.BEYOND_AUTHORITY_BIND_ROLES);
                }
            }

            if (currentUser.isTenantAdmin()) {
                // 超自身权限，但作为租户管理员有额外情况
                Set<Long> invisibleRoleIds = sysRoleService.determineInvisibleRoleIds();
                // 除非超越了可见角色IDs授予 or 想要授予用户租户管理员角色，否则不管
                invisibleRoleIds.addAll(currentUser.getRelatedRoleIds());
                if (CollUtil.containsAny(invisibleRoleIds, dto.getRoleIds())) {
                    throw new BusinessException(SysErrorEnum.BEYOND_AUTHORITY_BIND_ROLES);
                }
            }
        }
    }
}
