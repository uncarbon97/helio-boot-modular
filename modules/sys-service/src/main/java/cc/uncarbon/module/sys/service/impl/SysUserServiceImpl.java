package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.db.fieldcrypto.blindindex.BlindIndexCalculator;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import cc.uncarbon.module.commons.constant.SQLSegment;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.facade.AuditNicknameFiller;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.constant.SysUserCipherCols;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserTenantRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserTenantRelationMapper;
import cc.uncarbon.module.sys.enums.SysRoleFlagEnum;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.internal.UserDeptScope;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cc.uncarbon.module.sys.model.query.AdminSysRoleListRelatedUserQuery;
import cc.uncarbon.module.sys.model.query.AdminSysUserListQuery;
import cc.uncarbon.module.sys.model.request.*;
import cc.uncarbon.module.sys.model.response.SysUserBindRoleResult;
import cc.uncarbon.module.sys.model.response.TenantUserCreateResult;
import cc.uncarbon.module.sys.model.valueobj.SysUserBasicProfileDTO;
import cc.uncarbon.module.sys.model.valueobj.SysUserDTO;
import cc.uncarbon.module.sys.service.*;
import cc.uncarbon.module.sys.support.UserRoleHelper;
import cc.uncarbon.module.sys.util.PwdUtil;
import cc.uncarbon.module.tenant.constant.TenantConstant;
import cc.uncarbon.module.tenant.facade.TenantConfigFacade;
import cc.uncarbon.module.tenant.util.TenantUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


/**
 * 系统用户
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysUserServiceImpl implements SysUserService {

    private static final String LOG_PREFIX = "[系统管理][用户]";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserTenantRelationMapper sysUserTenantRelationMapper;
    private final SysDeptService sysDeptService;
    private final SysUserDeptRelationService sysUserDeptRelationService;
    private final SysUserRoleRelationService sysUserRoleRelationService;
    private final SysRoleMenuRelationService sysRoleMenuRelationService;
    private final TenantConfigFacade tenantConfigFacade;
    private final UserRoleHelper userRoleHelper;
    private final AuditNicknameFiller auditNicknameFiller;
    private final BlindIndexCalculator blindIndexCalculator;


    @SneakyThrows
    @Override
    public PageResult<SysUserDTO> adminList(AdminSysUserListQuery query) {
        final PageParam pageParam = Objects.requireNonNullElseGet(query.getPageParam(), PageParam::new);

        // 预处理：根据【手动选择的部门】筛选用户
        Set<Long> userIdsByDept = Set.of();
        if (query.needFilterBySelectedDeptId()) {
            userIdsByDept = sysUserDeptRelationService.listUserIdsByDepts(Set.of(query.getSelectedDeptId()));
            if (CollUtil.isEmpty(userIdsByDept)) {
                // 【手动选择的部门】没有任何用户ID，直接返回空列表
                return new PageResult<>(pageParam);
            }
        }

        // USER_FIRST 租户域内成员真源在 sys_user_tenant_relation；sys_user.tenant_id 投影列只反映最近记忆租户，
        // 行级过滤会漏掉拉入成员，改按成员ID集合取全局用户
        Set<Long> memberUserIds = Set.of();
        boolean byTenantMember = needTenantMemberScope();
        if (byTenantMember) {
            memberUserIds = sysUserTenantRelationMapper.listUserIdsByTenantId(TenantContextHolder.getTenantId());
            if (CollUtil.isEmpty(memberUserIds)) {
                // 本租户没有任何成员，直接返回空列表
                return new PageResult<>(pageParam);
            }
        }

        Set<Long> hiddenUserIds = userRoleHelper.listHiddenUserIds();
        // 与实体侧 @BlindIndexOf(Tail4Digits) 同一归一化口径，否则索引对不上
        String phoneTail4BlindIndex = CharSequenceUtil.isBlank(query.getPhoneNoTail4()) ? null
                : blindIndexCalculator.compute(SysUserCipherCols.PHONE_TAIL4_PURPOSE,
                        new SysUserEntity.Tail4Digits().extract(query.getPhoneNoTail4()));
        Page<SysUserEntity> entityPage = selectUserPage(pageParam.getPageNum(), pageParam.getPageSize(),
                new LambdaQueryWrapper<SysUserEntity>()
                        // 手机号尾号
                        .eq(phoneTail4BlindIndex != null,
                                SysUserEntity::getPhoneNoTail4BlindIndex, phoneTail4BlindIndex)
                        // 根据【手动选择的部门ID】筛选用户
                        .in(CollUtil.isNotEmpty(userIdsByDept), SysUserEntity::getId, userIdsByDept)
                        // USER_FIRST 租户域内限定本租户成员
                        .in(byTenantMember, SysUserEntity::getId, memberUserIds)
                        // 不显示特定用户
                        .notIn(CollUtil.isNotEmpty(hiddenUserIds), SysUserEntity::getId, hiddenUserIds)
                        // 排序
                        .orderByDesc(SysUserEntity::getId)
        );
        var ret = convertPage(entityPage);
        fillDept(ret.getRecords());
        return ret;
    }

    @Override
    public PageResult<SysUserDTO> adminListRoleRelatedUsers(AdminSysRoleListRelatedUserQuery query) {
        checkRoleQueryAccess(query.getRoleId());
        Set<Long> relatedUserIds = sysUserRoleRelationService.listUserIdsByRole(query.getRoleId());
        if (CollUtil.isEmpty(relatedUserIds)) {
            // 该角色没有关联任何用户，直接返回空列表
            return new PageResult<>(Objects.requireNonNullElseGet(query.getPageParam(), PageParam::new));
        }

        String keyword = CharSequenceUtil.cleanBlank(query.getKeyword());
        Set<Long> hiddenUserIds = userRoleHelper.listHiddenUserIds();
        // 角色关联表本身租户隔离，ID 集合已限定本租户；USER_FIRST 租户域内成员投影列可能指向他租户，忽略行级隔离取数
        Page<SysUserEntity> entityPage = selectUserPage(query.getPageNum(), query.getPageSize(),
                new LambdaQueryWrapper<SysUserEntity>()
                        // 关键词(账号/昵称)
                        .and(CharSequenceUtil.isNotBlank(keyword), w -> w
                                .like(SysUserEntity::getPin, keyword)
                                .or()
                                .like(SysUserEntity::getNickname, keyword))
                        // 仅角色关联的用户
                        .in(SysUserEntity::getId, relatedUserIds)
                        // 不显示特定用户（与用户分页列表一致）
                        .notIn(CollUtil.isNotEmpty(hiddenUserIds), SysUserEntity::getId, hiddenUserIds)
                        // 排序
                        .orderByDesc(SysUserEntity::getId)
        );
        var ret = convertPage(entityPage);
        fillDept(ret.getRecords());
        return ret;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreate(AdminSysUserCreateRequest request) {
        log.info(LOG_PREFIX + "新增 >> {}", request);
        checkRepeat(request);
        checkBeforeCreate(request);

        request.setId(null);
        var entity = new SysUserEntity();
        BeanUtil.copyProperties(request, entity);

        entity
                .setPin(request.getPin())
                .setPwd(PwdUtil.hash(request.getInitPwd()))
                .setCleanPhoneNo(request.getPhoneNo())
                .setCleanEmail(request.getEmail())
                // 默认新用户是禁用状态
                .setStatus(SysUserStatusEnum.DISABLED);

        sysUserMapper.insert(entity);
        sysUserDeptRelationService.cleanAndBind(entity.getId(), request.getDeptId());
        // 双写：relation 为唯一真源；tenant_id 投影列由 INSERT 自动填充，
        // 未启用租户隔离（strategy=NONE）时无上下文可填，回落平台自营域与列默认值对齐
        sysUserTenantRelationMapper.insert(SysUserTenantRelationEntity.of(
                Objects.requireNonNullElse(entity.getTenantId(), TenantConstant.PLATFORM_TENANT_ID), entity.getId()));
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
        entity
                .setCleanPhoneNo(request.getPhoneNo())
                .setCleanEmail(request.getEmail());

        updateUserByCurrentScope(() -> sysUserMapper.updateById(entity));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminSetStatus(AdminSetStatusRequest<Long, SysUserStatusEnum> request) {
        log.info(LOG_PREFIX + "修改状态 >> {}", request);
        checkExistence(request.getId());
        checkBeforeSetStatus(request.getId(), request.getNewStatus());
        updateUserByCurrentScope(() -> sysUserMapper.updateStatusBatch(List.of(request.getId()), request.getNewStatus()));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        log.info(LOG_PREFIX + "删除 >> {}", ids);
        checkBeforeDelete(ids);
        // 解除该用户的部门/角色/租户关联，避免孤儿关系行
        ids.forEach(id -> {
            sysUserDeptRelationService.cleanAndBind(id, null);  // null = 解除全部部门绑定
            sysUserRoleRelationService.cleanAndBindByUser(id, null);  // 空集 = 解除全部角色绑定
            sysUserTenantRelationMapper.deleteByUserId(id);
        });
        sysUserMapper.deleteByIds(ids);
    }

    @Override
    public SysUserDTO getOperableById(Long id) {
        if (id == null) return null;
        checkUserOperationAccess(Set.of(id));

        var entity = selectUserByCurrentScope(id);
        var ret = auditNicknameFiller.fillOne(entity, this::convertEntity);
        if (ret != null) {
            fillDept(List.of(ret));
        }
        return ret;
    }

    @Override
    public SysUserDTO getNonnullOperableById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(getOperableById(id));
    }

    @Override
    public SysUserBasicProfileDTO getNonnullBasicProfileById(Long id) throws NoRecordException {
        var entity = NoRecordException.throwIfNull(selectUserByCurrentScope(id));
        var ret = new SysUserBasicProfileDTO();
        BeanUtil.copyProperties(entity, ret);
        return ret;
    }

    @Override
    public TenantUserCreateResult createTenantUser(TenantUserCreateRequest request) {
        var entity = new SysUserEntity();
        BeanUtil.copyProperties(request, entity);
        // 按需改写字段
        entity.setPwd(PwdUtil.hash(request.getPwdPlain()))
                .setCleanPhoneNo(request.getPhoneNo())
                .setCleanEmail(request.getEmail());

        if (request.isTenantAdmin()) {
            entity.setNickname(request.getTenantName() + "主管理员")
                    .setStatus(SysUserStatusEnum.ENABLED);
        }
        sysUserMapper.insert(entity);
        return new TenantUserCreateResult(entity.getId(), request.isTenantAdmin());
    }

    @Override
    public void adminResetPassword(AdminSysUserResetPasswordRequest request) {
        checkBeforeUpdate(request.getUserId());
        checkExistence(request.getUserId());
        var user = selectUserByCurrentScope(request.getUserId());
        updateUserByCurrentScope(() -> sysUserMapper.updateEncryptedPwd(user.getId(),
                PwdUtil.hash(request.getRandomPassword()), request.getPwdMustChange()));
    }

    @Override
    public SysUserBindRoleResult adminBindRole(AdminSysUserBindRoleRequest request) {
        checkBeforeBindUserRoleRelation(request);
        sysUserRoleRelationService.cleanAndBindByUser(request.getUserId(), request.getRoleIds());
        // 查询修改后的用户，关联的角色
        UserRoleScope userRoleScope = userRoleHelper.getSpecifiedUserRole(request.getUserId());
        return new SysUserBindRoleResult()
                .setOld(getNonnullOperableById(request.getUserId()))
                .setRoleNames(userRoleScope.getRelatedRoles().stream().map(SysRoleEntity::getName).toList());
    }

    @Override
    public void adminBindDept(AdminSysUserBindDeptRequest request) {
        checkExistence(request.getUserId());
        checkBeforeBindDept(request.getUserId());
        checkUserOperationAccess(Set.of(request.getUserId()));
        // 调岗目标部门必须启用；解除绑定（deptId 为空）不受限
        checkDeptAccess(request.getDeptId());
        sysUserDeptRelationService.cleanAndBind(request.getUserId(), request.getDeptId());
    }


    @Override
    public void checkUserQueryAccess(Long userId) {
        UserRoleScope me = userRoleHelper.getCurrentUserRole();
        if (me.isSuperAdmin()) {
            return;
        }
        checkUserOperationAccess(Set.of(userId));
    }

    @Override
    public void checkUserQueryAccess(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        UserRoleScope me = userRoleHelper.getCurrentUserRole();
        if (me.isSuperAdmin()) {
            return;
        }
        checkUserOperationAccess(userIds);
    }

    @Override
    public void checkRoleQueryAccess(Long roleId) {
        UserRoleScope me = userRoleHelper.getCurrentUserRole();
        if (me.isSuperAdmin()) {
            return;
        }
        // 隐藏角色（如内置超管角色）不可查询
        Set<Long> hiddenRoleIds = userRoleHelper.listHiddenRoleIds();
        if (CollUtil.contains(hiddenRoleIds, roleId)) {
            throw new BusinessException(SysErrorCodeEnum.A01021);
        }
        // 角色必须存在于当前租户范围内；租户行级过滤下，外租户/已删除角色查不到
        boolean exists = sysRoleMapper.exists(new LambdaQueryWrapper<SysRoleEntity>()
                .select(SysRoleEntity::getId)
                .eq(SysRoleEntity::getId, roleId)
                .last(SQLSegment.LIMIT_1)
        );
        if (!exists) {
            throw new BusinessException(SysErrorCodeEnum.A01021);
        }
    }

    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */


    /**
     * 实体转值对象
     */
    private SysUserDTO convertEntity(SysUserEntity entity) {
        if (entity == null) return null;

        var ret = new SysUserDTO();
        BeanUtil.copyProperties(entity, ret);
        return ret;
    }

    /**
     * 实体转值对象
     */
    private List<SysUserDTO> convertList(List<SysUserEntity> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }
        return auditNicknameFiller.fill(entityList, this::convertEntity);
    }

    /**
     * 实体转值对象
     */
    private PageResult<SysUserDTO> convertPage(Page<SysUserEntity> entityPage) {
        return new PageResult<SysUserDTO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                .setRecords(convertList(entityPage.getRecords()));
    }

    /**
     * 填充所属部门
     */
    private void fillDept(Collection<SysUserDTO> collection) {
        if (CollUtil.isNotEmpty(collection)) {
            for (SysUserDTO item : collection) {
                if (item != null) {
                    Optional.ofNullable(sysDeptService.getSpecifiedUserDept(item.getId(), false))
                            .map(UserDeptScope::primaryRelatedDept)
                            .ifPresent(dept -> item.setDeptId(dept.getId())
                                    .setDeptName(dept.getName()));
                }
            }
        }
    }

    /**
     * 检查是否存在重复
     */
    @SneakyThrows
    private void checkRepeat(AdminSysUserUpdateRequest request) {
        LambdaQueryWrapper<SysUserEntity> qw = new LambdaQueryWrapper<SysUserEntity>()
                .select(SysUserEntity::getId)
                // 并非原地更新
                .ne(Objects.nonNull(request.getId()), SysUserEntity::getId, request.getId())
                .eq(SysUserEntity::getPin, request.getPin())
                .last(SQLSegment.LIMIT_1);
        var entity = isUserFirstLoginMode()
                ? TenantContextHolder.callIgnored(() -> sysUserMapper.selectOne(qw))
                : sysUserMapper.selectOne(qw);
        if (entity != null) {
            throw new HasRepeatRecordException(SysErrorCodeEnum.A01030);
        }
    }

    /**
     * 是否用户优先登录模式
     */
    private boolean isUserFirstLoginMode() {
        return TenantLoginModeEnum.USER_FIRST == tenantConfigFacade.getLoginMode();
    }

    /**
     * 检查是否存在
     */
    @SneakyThrows
    private void checkExistence(Long id) {
        LambdaQueryWrapper<SysUserEntity> qw = new LambdaQueryWrapper<SysUserEntity>()
                .select(SysUserEntity::getId)
                .eq(SysUserEntity::getId, id)
                .last(SQLSegment.LIMIT_1);
        boolean exists = needTenantMemberScope()
                // USER_FIRST 租户域内成员投影列可能指向他租户，忽略行级过滤按ID判定
                ? TenantContextHolder.callIgnored(() -> sysUserMapper.exists(qw))
                : sysUserMapper.exists(qw);
        NoRecordException.throwIfFalse(exists);
    }

    /**
     * 新增前检查
     */
    private void checkBeforeCreate(AdminSysUserCreateRequest request) {
        checkDeptAccess(request.getDeptId());
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
        // USER_FIRST 租户域内「删除」语义实为移出本租户，须走 tenant-expel；全局删人仅平台自营域可执行
        if (isUserFirstLoginMode() && !TenantUtil.isPlatformDomain(TenantContextHolder.getTenantId())) {
            throw new BusinessException(SysErrorCodeEnum.A01053);
        }
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
        if (selfFlag && !me.isSuperAdmin()) {
            // 不能动自身用户
            throw new BusinessException(SysErrorCodeEnum.A01020);
        }
        if (selfFlag && !CollUtil.contains(request.getRoleIds(), SysConstant.SUPER_ADMIN_ROLE_ID)) {
            // 超级管理员不能去掉自己的超级管理员角色
            throw new BusinessException(SysErrorCodeEnum.A01020);
        }
        checkBuiltinRoleImmutable(request);
        if (me.isSuperAdmin()) {
            // 也不能赋予其他人超级管理员角色
            if (!selfFlag && CollUtil.contains(request.getRoleIds(), SysConstant.SUPER_ADMIN_ROLE_ID)) {
                throw new BusinessException(SysErrorCodeEnum.A01021);
            }
            return;
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
                Set<Long> hiddenRoleIds = new HashSet<>(userRoleHelper.listHiddenRoleIds());
                // 除非超越了可见角色IDs 授予 or 想要授予用户租户管理员角色，否则不管
                hiddenRoleIds.addAll(me.getRelatedRoleIds());
                if (CollUtil.containsAny(hiddenRoleIds, request.getRoleIds())) {
                    throw new BusinessException(SysErrorCodeEnum.A01022);
                }

                // 想要授予的角色必须存在于本租户范围内；租户行级过滤下，外租户/已删除角色查不到
                Set<Long> requestedRoleIds = new HashSet<>(CollUtil.emptyIfNull(request.getRoleIds()));
                Set<Long> existingRoleIds = CollUtil.isEmpty(requestedRoleIds) ? Set.of()
                        : sysRoleMapper.selectByIds(requestedRoleIds).stream()
                        .map(SysRoleEntity::getId)
                        .collect(java.util.stream.Collectors.toSet());
                if (!existingRoleIds.containsAll(requestedRoleIds)) {
                    throw new BusinessException(SysErrorCodeEnum.A01022);
                }
            }
        }
    }

    /**
     * 内置角色（超级管理员、租户管理员等）必须与对应管理员保持关联，不允许手动分配，也不允许解除关联
     */
    @SneakyThrows
    private void checkBuiltinRoleImmutable(AdminSysUserBindRoleRequest request) {
        if (Objects.equals(request.getUserId(), SysConstant.SUPER_ADMIN_USER_ID)) {
            // 超管的角色关联固定在平台自营域，切入其他租户视角后按平台域解析
            TenantContextHolder.callWithContext(
                    new SimpleTenantContext(TenantConstant.PLATFORM_TENANT_ID, null, null),
                    () -> {
                        doCheckBuiltinRoleImmutable(request);
                        return null;
                    });
            return;
        }
        doCheckBuiltinRoleImmutable(request);
    }

    private void doCheckBuiltinRoleImmutable(AdminSysUserBindRoleRequest request) {
        Set<Long> currentRoleIds = new HashSet<>(sysUserRoleRelationService.listRoleIdsByUser(request.getUserId()));
        Collection<Long> requestedRoleIds = CollUtil.emptyIfNull(request.getRoleIds());
        // 涉及的角色 = 现有关联 + 想要关联，一并取出解析特殊标记
        Set<Long> involvedRoleIds = new HashSet<>(currentRoleIds);
        involvedRoleIds.addAll(requestedRoleIds);
        if (CollUtil.isEmpty(involvedRoleIds)) {
            return;
        }

        Set<Long> builtinRoleIds = sysRoleMapper.selectByIds(involvedRoleIds).stream()
                .filter(role -> CollUtil.contains(role.resolveFlags(), SysRoleFlagEnum.BUILTIN))
                .map(SysRoleEntity::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (CollUtil.isEmpty(builtinRoleIds)) {
            return;
        }

        // 已关联的内置角色不能被解除
        if (CollUtil.isNotEmpty(CollUtil.subtract(builtinRoleIds, requestedRoleIds))) {
            throw new BusinessException(SysErrorCodeEnum.A01025);
        }
        // 未关联的内置角色不能被手动分配
        if (CollUtil.isNotEmpty(CollUtil.subtract(builtinRoleIds, currentRoleIds))) {
            throw new BusinessException(SysErrorCodeEnum.A01024);
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
     * 检查目标部门是否可被引用（存在且处于启用状态）
     *
     * @param deptId 目标部门ID，为 null 时不校验（视为解除绑定）
     */
    private void checkDeptAccess(Long deptId) {
        if (Objects.isNull(deptId)) {
            return;
        }
        // 目标部门必须处于启用状态，屏蔽对停用部门的新增引用
        if (EnabledStatusEnum.DISABLED == sysDeptService.getNonnullById(deptId).getStatus()) {
            throw new BusinessException(SysErrorCodeEnum.A01035);
        }
    }

    /**
     * 检查当前用户，是否有操作特定用户的权限
     * <p>USER_FIRST 租户域内行级隔离不再是可见范围依据（拉入成员投影列指向他租户），改为显式校验成员关系；
     * 其余场景可见范围仍由 sys_user 行级租户隔离兜底，跨租户/已删除用户按记录不存在处理
     */
    private void checkUserOperationAccess(Collection<Long> specifiedUserIds) {
        Set<Long> hiddenUserIds = userRoleHelper.listHiddenUserIds();
        if (CollUtil.isNotEmpty(hiddenUserIds) && CollUtil.containsAny(hiddenUserIds, specifiedUserIds)) {
            throw new BusinessException(SysErrorCodeEnum.A01021);
        }
        if (needTenantMemberScope()) {
            // 非本租户成员不可见、不可操作，按越权处理
            Set<Long> memberUserIds = sysUserTenantRelationMapper.listUserIdsByTenantId(TenantContextHolder.getTenantId());
            if (!CollUtil.containsAll(memberUserIds, specifiedUserIds)) {
                throw new BusinessException(SysErrorCodeEnum.A01021);
            }
        }
    }

    /**
     * USER_FIRST 模式且当前处于租户域（非平台自营域）时，成员真源在 sys_user_tenant_relation，
     * sys_user 行级隔离不可作为可见范围依据
     */
    private boolean needTenantMemberScope() {
        return isUserFirstLoginMode() && !TenantUtil.isPlatformDomain(TenantContextHolder.getTenantId());
    }

    /**
     * 按当前作用域读取用户
     * <p>USER_FIRST 租户域内成员投影列可能指向他租户，行级过滤会漏读，改为忽略隔离按ID直读
     * （可见性由 {@link #checkUserOperationAccess} 成员关系校验兜底）
     */
    @SneakyThrows
    private SysUserEntity selectUserByCurrentScope(Long id) {
        if (needTenantMemberScope()) {
            return TenantContextHolder.callIgnored(() -> sysUserMapper.selectById(id));
        }
        return sysUserMapper.selectById(id);
    }

    /**
     * 按当前作用域分页读取用户
     * <p>USER_FIRST 租户域内忽略行级隔离取数，调用方须以显式 ID 集合限定范围
     */
    @SneakyThrows
    private Page<SysUserEntity> selectUserPage(long pageNum, long pageSize, LambdaQueryWrapper<SysUserEntity> qw) {
        if (needTenantMemberScope()) {
            return TenantContextHolder.callIgnored(() -> sysUserMapper.selectPage(new Page<>(pageNum, pageSize), qw));
        }
        return sysUserMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }

    /**
     * 按当前作用域写用户
     * <p>USER_FIRST 租户域内成员投影列指向他租户，行级过滤会使 UPDATE 落空，忽略隔离按ID写
     */
    private void updateUserByCurrentScope(Runnable writeOp) {
        if (needTenantMemberScope()) {
            TenantContextHolder.runIgnored(writeOp);
            return;
        }
        writeOp.run();
    }

}
