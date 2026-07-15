package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.model.request.AdminBatchSetStatusRequest;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.enums.SysRoleFlagEnum;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.helper.UserRoleHelper;
import cc.uncarbon.module.sys.model.internal.UserDeptScope;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cc.uncarbon.module.sys.model.query.AdminSysUserListQuery;
import cc.uncarbon.module.sys.model.request.*;
import cc.uncarbon.module.sys.model.response.TenantUserCreateResult;
import cc.uncarbon.module.sys.model.valueobj.SysUserDTO;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cc.uncarbon.module.sys.service.SysUserDeptRelationService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cc.uncarbon.module.sys.service.SysUserService;
import cc.uncarbon.module.sys.util.PwdUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.*;


/**
 * 系统用户
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysUserServiceImpl implements SysUserService {

    private static final String LOG_PREFIX = "[系统管理][用户]";

    private final SysUserMapper sysUserMapper;
    private final SysDeptServiceImpl sysDeptService;
    private final SysUserDeptRelationService sysUserDeptRelationService;
    private final SysUserRoleRelationService sysUserRoleRelationService;
    private final SysRoleMenuRelationService sysRoleMenuRelationService;
    private final UserRoleHelper userRoleHelper;


    @Override
    public PageResult<SysUserDTO> adminList(AdminSysUserListQuery query) {
        // 预处理：根据【手动选择的部门】筛选用户
        Set<Long> deptUserIds = Set.of();
        if (query.needFilterBySelectedDeptId()) {
            deptUserIds = sysUserDeptRelationService.listUserIdsByDepts(Set.of(query.getSelectedDeptId()));
            if (CollUtil.isEmpty(deptUserIds)) {
                // 【手动选择的部门】没有任何用户ID，直接返回空列表
                return new PageResult<>(query.getPageParam());
            }
        }

        // 预处理：根据【只能看到本部门及下级部门原则】筛选用户
        Set<Long> visibleUserIds = determineVisibleDeptUserIds();
        if (Objects.equals(CollUtil.getFirst(visibleUserIds), BigInteger.ZERO.longValue())) {
            // 其实啥也看不到……
            return new PageResult<>(query.getPageParam());
        }

        Set<Long> invisibleUserIds = userRoleHelper.listInvisibleUserIds();
        Page<SysUserEntity> entityPage = sysUserMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new LambdaQueryWrapper<SysUserEntity>()
                        // 手机号
                        .like(CharSequenceUtil.isNotBlank(query.getPhoneNo()), SysUserEntity::getPhoneNo, CharSequenceUtil.cleanBlank(query.getPhoneNo()))
                        // 根据【手动选择的部门ID】筛选用户
                        .in(CollUtil.isNotEmpty(deptUserIds), SysUserEntity::getId, deptUserIds)
                        // 根据【只能看到本部门及下级部门原则】筛选用户
                        .in(CollUtil.isNotEmpty(visibleUserIds), SysUserEntity::getId, visibleUserIds)
                        // 不显示特定用户
                        .notIn(CollUtil.isNotEmpty(invisibleUserIds), SysUserEntity::getId, invisibleUserIds)
                        // 排序
                        .orderByDesc(SysUserEntity::getId)
        );
        return convertPage(entityPage, true);
    }

    @Override
    public PageResult<SysUserDTO> adminListNoDeptUsers(AdminSysUserListQuery query) {
        Page<SysUserEntity> entityPage = sysUserMapper.pageNoDeptUser(
                new Page<>(query.getPageNum(), query.getPageSize()),
                CharSequenceUtil.cleanBlank(query.getPhoneNo()),
                userRoleHelper.listInvisibleUserIds()
        );
        return convertPage(entityPage, false);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreate(AdminSysUserCreateRequest request, boolean hasBindDeptPerm) {
        log.info(LOG_PREFIX + "新增 >> {}", request);
        checkRepeat(request);
        checkBeforeCreate(request, hasBindDeptPerm);

        request.setId(null);
        var entity = new SysUserEntity();
        BeanUtil.copyProperties(request, entity);

        String salt = IdUtil.randomUUID();
        entity
                .setPin(request.getPin())
                .setPwd(PwdUtil.encrypt(request.getInitPwd(), salt))
                .setPwdSalt(salt)
                // 默认新用户是禁用状态
                .setStatus(SysUserStatusEnum.DISABLED);

        sysUserMapper.insert(entity);
        sysUserDeptRelationService.cleanAndBind(entity.getId(), request.getDeptId());
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdate(AdminSysUserUpdateRequest request) {
        log.info(LOG_PREFIX + "修改 >> {}", request);
        checkExistence(request.getId());
        checkRepeat(request);
        checkBeforeUpdate(request.getId());

        var entity = new SysUserEntity();
        BeanUtil.copyProperties(request, entity);

        sysUserMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminSetStatus(AdminBatchSetStatusRequest<Long, SysUserStatusEnum> request) {
        log.info(LOG_PREFIX + "修改状态 >> {}", request);
        request.getIds().forEach(id -> {
            checkExistence(id);
            checkBeforeSetStatus(id, request.getNewStatus());
        });
        sysUserMapper.update(new LambdaUpdateWrapper<SysUserEntity>()
                .set(SysUserEntity::getStatus, request.getNewStatus())
                .in(SysUserEntity::getId, request.getIds())
        );
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        log.info(LOG_PREFIX + "删除 >> {}", ids);
        checkBeforeDelete(ids);
        // 解除该用户的部门/角色关联，避免孤儿关系行
        ids.forEach(id -> {
            sysUserDeptRelationService.cleanAndBind(id, null);      // null = 解除全部部门绑定
            sysUserRoleRelationService.cleanAndBind(id, null);  // 空集 = 解除全部角色绑定
        });
        sysUserMapper.deleteByIds(ids);
    }

    @Override
    public SysUserDTO getById(Long id) {
        if (id == null) return null;
        checkUserOperationAccess(Set.of(id));

        var entity = sysUserMapper.selectById(id);
        return convertEntity(entity, true);
    }

    @Override
    public SysUserDTO getNonnullById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(getById(id));
    }

    @Override
    public SysUserEntity getNonnullEntityById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(sysUserMapper.selectById(id));
    }

    @Override
    public TenantUserCreateResult createTenantUser(TenantUserCreateRequest request) {
        var entity = new SysUserEntity();
        BeanUtil.copyProperties(request, entity);
        // 按需改写字段
        String salt = IdUtil.randomUUID();
        entity
                .setPwd(PwdUtil.encrypt(request.getPwdPlain(), salt))
                .setPwdSalt(salt);

        if (request.isTenantAdmin()) {
            entity
                    .setNickname(request.getTenantName() + "主管理员")
                    .setStatus(SysUserStatusEnum.ENABLED);
        }
        sysUserMapper.insert(entity);
        return new TenantUserCreateResult(entity.getId(), request.isTenantAdmin());
    }

    @Override
    public void adminResetSpecifiedUserPassword(AdminSysUserResetSpecifiedOnePasswordRequest request) {
        checkBeforeUpdate(request.getUserId());
        checkExistence(request.getUserId());
        var user = sysUserMapper.selectById(request.getUserId());
        sysUserMapper.updateEncryptedPwd(user.getId(),
                PwdUtil.encrypt(request.getRandomPassword(), user.getPwdSalt()));
    }

    @Override
    public void adminBindRole(AdminSysUserBindRoleRequest request) {
        checkBeforeBindUserRoleRelation(request);
        sysUserRoleRelationService.cleanAndBind(request.getUserId(), request.getRoleIds());
    }

    @Override
    public void adminBindDept(AdminSysUserBindDeptRequest request) {
        checkExistence(request.getUserId());
        checkBeforeBindDept(request.getUserId());
        sysUserDeptRelationService.cleanAndBind(request.getUserId(), request.getDeptId());
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */


    /**
     * 实体转值对象
     *
     * @param fillDept 填充部门
     */
    private SysUserDTO convertEntity(SysUserEntity entity, boolean fillDept) {
        if (entity == null) return null;

        var ret = new SysUserDTO();
        BeanUtil.copyProperties(entity, ret);
        // 按需改写字段
        if (fillDept) {
            Optional.ofNullable(sysDeptService.getSpecifiedUserDept(ret.getId(), false))
                    .map(UserDeptScope::primaryRelatedDept)
                    .ifPresent(dept -> ret.setDeptId(dept.getId())
                            .setDeptName(dept.getName()));
        }
        return ret;
    }

    /**
     * 实体转值对象
     *
     * @param fillDept 填充部门
     */
    List<SysUserDTO> convertList(List<SysUserEntity> entityList, boolean fillDept) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }
        return entityList.stream().map(entity -> convertEntity(entity, fillDept)).toList();
    }

    /**
     * 实体转值对象
     *
     * @param fillDept 填充部门
     */
    PageResult<SysUserDTO> convertPage(Page<SysUserEntity> entityPage, boolean fillDept) {
        return new PageResult<SysUserDTO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                .setRecords(convertList(entityPage.getRecords(), fillDept));
    }

    /**
     * 检查是否存在重复
     */
    private void checkRepeat(AdminSysUserUpdateRequest request) {
        var entity = sysUserMapper.getByPin(request.getPin());
        if (entity != null) {
            throw new HasRepeatRecordException("已存在相同的账号");
        }
    }

    /**
     * 检查是否存在
     */
    private void checkExistence(Long id) {
        boolean exists = sysUserMapper.exists(
                new LambdaQueryWrapper<SysUserEntity>()
                        .select(SysUserEntity::getId)
                        .eq(SysUserEntity::getId, id)
                        .last(SQLSegment.LIMIT_1)
        );
        NoRecordException.throwIfFalse(exists);
    }

    /**
     * 新增前检查
     *
     * @param hasBindDeptPerm 当前用户是否有「调整用户部门」权限
     */
    private void checkBeforeCreate(AdminSysUserCreateRequest request, boolean hasBindDeptPerm) {
        checkDeptAccess(request.getDeptId(), hasBindDeptPerm);
    }

    /**
     * 修改前检查
     *
     * @param specifiedUserId 被操作用户ID
     */
    void checkBeforeUpdate(Long specifiedUserId) {
        UserRoleScope me = userRoleHelper.getCurrentUserRole();
        if (me.isSuperAdmin()) {
            return;
        }

        if (Objects.equals(specifiedUserId, UserContextHolder.getUserId())) {
            // 不能动自身用户
            throw new BusinessException(SysErrorCodeEnum.A01020);
        }

        // 目标是超级管理员or租户管理员时，均不能编辑
        UserRoleScope specifiedUser = userRoleHelper.getSpecifiedUserRole(specifiedUserId);
        if (specifiedUser.isSuperAdmin() || specifiedUser.isTenantAdmin()) {
            throw new BusinessException(SysErrorCodeEnum.A01021);
        }

        checkUserOperationAccess(Set.of(specifiedUserId));
        // 暂未实现角色层级，一律平级
    }

    /**
     * 修改状态前检查
     */
    private void checkBeforeSetStatus(Long specifiedUserId, SysUserStatusEnum status) {
        UserRoleScope me = userRoleHelper.getCurrentUserRole();
        if (me.isSuperAdmin()) {
            if (status != SysUserStatusEnum.ENABLED
                    && Objects.equals(specifiedUserId, UserContextHolder.getUserId())) {
                throw new BusinessException(SysErrorCodeEnum.A01020);
            }
            return;
        }
        checkBeforeUpdate(specifiedUserId);
    }

    /**
     * 删除前检查
     */
    void checkBeforeDelete(Collection<Long> ids) {
        if (CollUtil.contains(ids, UserContextHolder.getUserId())) {
            // 不能动自身用户
            throw new BusinessException(SysErrorCodeEnum.A01020);
        }

        // 查询用户关联角色
        List<UserRoleScope> specifiedUserRoles = ids.stream().map(userRoleHelper::getSpecifiedUserRole).toList();
        boolean hasBuiltinRole = specifiedUserRoles.stream()
                .map(UserRoleScope::getRelatedRoles)
                .flatMap(Collection::stream)
                .map(SysRoleEntity::resolveFlags)
                .anyMatch(flags -> CollUtil.contains(flags, SysRoleFlagEnum.BUILTIN));
        if (hasBuiltinRole) {
            throw new BusinessException(SysErrorCodeEnum.A01023);
        }
        checkUserOperationAccess(ids);
    }

    /**
     * 绑定用户与角色关联关系前检查
     * 防止越权访问漏洞
     */
    private void checkBeforeBindUserRoleRelation(AdminSysUserBindRoleRequest request) {
        UserRoleScope me = userRoleHelper.getCurrentUserRole();
        // 是否想要操作自身
        boolean selfFlag = Objects.equals(request.getUserId(), UserContextHolder.getUserId());
        if (me.isSuperAdmin()) {
            // 超级管理员不能去掉自己的超级管理员角色
            if (selfFlag && !CollUtil.contains(request.getRoleIds(), SysConstant.SUPER_ADMIN_ROLE_ID)) {
                throw new BusinessException(SysErrorCodeEnum.A01020);
            }
            // 也不能赋予其他人超级管理员角色
            if (!selfFlag && CollUtil.contains(request.getRoleIds(), SysConstant.SUPER_ADMIN_ROLE_ID)) {
                throw new BusinessException(SysErrorCodeEnum.A01021);
            }
            return;
        }

        if (selfFlag) {
            // 不能动自身用户
            throw new BusinessException(SysErrorCodeEnum.A01020);
        }

        // 目标已经是超级管理员or租户管理员时，均不能绑定
        UserRoleScope specifiedUser = userRoleHelper.getSpecifiedUserRole(request.getUserId());
        if (specifiedUser.isSuperAdmin() || specifiedUser.isTenantAdmin()) {
            throw new BusinessException(SysErrorCodeEnum.A01021);
        }
        whenCurrentUserNotSuperAdmin(request, me);
        checkUserOperationAccess(List.of(request.getUserId()));
    }

    /**
     * 超级管理员之外的用户，都需要校验自身角色范围是否满足输入值
     * 拆分子方法以降低复杂度
     */
    private void whenCurrentUserNotSuperAdmin(AdminSysUserBindRoleRequest request, UserRoleScope me) {
        if (CollUtil.isNotEmpty(request.getRoleIds()) && !me.isSuperAdmin()) {
            boolean overRoles = !CollUtil.containsAll(me.getRelatedRoleIds(), request.getRoleIds());
            if (overRoles && me.isNotAnyAdmin()) {
                // 普通用户超自身角色授予了；如果当前用户拥有新角色的所有菜单，那么也放行
                Set<Long> grantedMenuIds = sysRoleMenuRelationService.listMenuIdsByRoles(me.getRelatedRoleIds());
                Set<Long> needMenuIds = sysRoleMenuRelationService.listMenuIdsByRoles(request.getRoleIds());
                if (!CollUtil.containsAll(grantedMenuIds, needMenuIds)) {
                    throw new BusinessException(SysErrorCodeEnum.A01022);
                }
            }

            if (me.isTenantAdmin()) {
                // 超自身权限，但作为租户管理员有额外情况
                Set<Long> invisibleRoleIds = userRoleHelper.listInvisibleRoleIds();
                // 除非超越了可见角色IDs 授予 or 想要授予用户租户管理员角色，否则不管
                invisibleRoleIds.addAll(me.getRelatedRoleIds());
                if (CollUtil.containsAny(invisibleRoleIds, request.getRoleIds())) {
                    throw new BusinessException(SysErrorCodeEnum.A01022);
                }
            }
        }
    }

    /**
     * 调整用户部门前检查
     * 不做可见范围限制；仅防止越权改动管理员账户
     */
    private void checkBeforeBindDept(Long specifiedUserId) {
        UserRoleScope me = userRoleHelper.getCurrentUserRole();
        if (me.isSuperAdmin()) {
            // 超级管理员为所欲为
            return;
        }
        if (Objects.equals(specifiedUserId, UserContextHolder.getUserId())) {
            // 不能动自身用户
            throw new BusinessException(SysErrorCodeEnum.A01020);
        }
        UserRoleScope specifiedUser = userRoleHelper.getSpecifiedUserRole(specifiedUserId);
        if (specifiedUser.isSuperAdmin() || specifiedUser.isTenantAdmin()) {
            // 目标是超级管理员or租户管理员时，不能调动
            throw new BusinessException(SysErrorCodeEnum.A01021);
        }
    }

    /**
     * 检查当前用户，是否有访问特定部门的权限
     *
     * @param deptId          目标部门ID，为 null 时不校验（视为解除绑定）
     * @param hasBindDeptPerm 当前用户是否有「调整用户部门」权限
     */
    private void checkDeptAccess(Long deptId, boolean hasBindDeptPerm) {
        if (Objects.isNull(deptId)) {
            return;
        }
        // 持有跨部门调岗权限者（如高级 HR），可跨部门管理，不受可见部门域限制
        if (hasBindDeptPerm) {
            return;
        }
        // 对传入的部门ID，做数据越权检查
        UserDeptScope dept = sysDeptService.getCurrentUserDept(true);
        if (dept.hasVisibleDepts() && !CollUtil.contains(dept.getVisibleDeptIds(), deptId)) {
            // 传入的部门ID，不在当前用户可见范围内，阻止
            throw new BusinessException(SysErrorCodeEnum.A01021);
        }
    }

    /**
     * 检查当前用户，是否有操作特定用户的权限
     */
    private void checkUserOperationAccess(Collection<Long> specifiedUserIds) {
        Set<Long> visibleUserIds = determineVisibleDeptUserIds();
        Set<Long> invisibleUserIds = userRoleHelper.listInvisibleUserIds();
        if (CollUtil.isNotEmpty(visibleUserIds) && !CollUtil.containsAll(visibleUserIds, specifiedUserIds)
                || CollUtil.isNotEmpty(invisibleUserIds) && CollUtil.containsAny(invisibleUserIds, specifiedUserIds)) {
            throw new BusinessException(SysErrorCodeEnum.A01021);
        }
    }

    /**
     * 确定本部门及下级部门用户IDs
     * 返回空集合代表不限制
     * 返回[0]或有元素集合，表示有限制
     */
    private Set<Long> determineVisibleDeptUserIds() {
        Set<Long> visibleUserIds = Set.of();
        UserDeptScope deptContainer = sysDeptService.getCurrentUserDept(true);
        if (deptContainer.hasVisibleDepts()) {
            visibleUserIds = sysUserDeptRelationService.listUserIdsByDepts(deptContainer.getVisibleDeptIds());
            if (CollUtil.isEmpty(visibleUserIds)) {
                // 「可见部门」中没有任何用户ID，则直接返回[0]
                return Set.of(BigInteger.ZERO.longValue());
            }
        }
        return visibleUserIds;
    }

}
