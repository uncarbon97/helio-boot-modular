package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.db.constant.SQLSegment;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.enums.SysErrorCodeEnum;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.model.interior.UserDeptContainer;
import cc.uncarbon.module.sys.model.interior.UserRoleContainer;
import cc.uncarbon.module.sys.model.query.AdminSysUserListQuery;
import cc.uncarbon.module.sys.model.request.*;
import cc.uncarbon.module.sys.model.valueobj.SysUserBO;
import cc.uncarbon.module.sys.model.valueobj.SysUserLoginBO;
import cc.uncarbon.module.sys.model.valueobj.VbenAdminUserInfoVO;
import cc.uncarbon.module.sys.service.*;
import cc.uncarbon.module.sys.util.PwdUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 后台用户
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleService sysRoleService;
    private final SysDeptServiceImpl sysDeptService;
    private final SysMenuService sysMenuService;
    private final SysTenantService sysTenantService;
    private final SysUserDeptRelationService sysUserDeptRelationService;
    private final SysUserRoleRelationService sysUserRoleRelationService;
    private final SysRoleMenuRelationService sysRoleMenuRelationService;

    private boolean isTenantEnabled;

    @PostConstruct
    @Override
    public void postConstruct() {
        this.isTenantEnabled = heliumProperties.getTenant().getEnabled();
    }

    @Override
    public PageResult<SysUserBO> adminList(AdminSysUserListQuery query) {
        // 预处理：根据【手动选择的部门】筛选用户
        Set<Long> deptUserIds = Collections.emptySet();
        if (query.needFilterBySelectedDeptId()) {
            deptUserIds = sysUserDeptRelationService.listUserIdsByDeptIds(Collections.singleton(query.getSelectedDeptId()));
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

        Set<Long> invisibleUserIds = determineInvisibleUserIds();
        Page<SysUserEntity> entityPage = sysUserMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new QueryWrapper<SysUserEntity>()
                        .lambda()
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

        return this.entityPage2BOPage(entityPage, true);
    }

    @Override
    public SysUserBO getById(Long id) {
        dataScopeCheck(Collections.singleton(id));

        SysUserEntity entity = sysUserMapper.selectById(id);
        return this.entity2BO(entity, true);
    }

    @Override
    public SysUserBO getNonnullById(Long id) throws NoRecordException {
        return NoRecordException.throwIfNull(getById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminCreate(AdminSysUserUpsertRequest request) {
        log.info("[后台管理-新增后台用户] >> 入参={}", request);
        checkRepeat(request);

        if (Objects.nonNull(request.getDeptId())) {
            // 对传入的部门ID，做数据越权检查
            UserDeptContainer deptContainer = sysDeptService.getCurrentUserDeptContainer(true);
            if (deptContainer.hasVisibleDepts() && !CollUtil.contains(deptContainer.getVisibleDeptIds(), request.getDeptId())) {
                throw new BusinessException(SysErrorCodeEnum.A01021);
            }
        }

        request.setId(null);
        SysUserEntity entity = new SysUserEntity();
        BeanUtil.copyProperties(request, entity);

        String salt = IdUtil.randomUUID();
        entity
                .setPin(request.getPin())
                .setPwd(PwdUtil.encrypt(request.getPasswordOfNewUser(), salt))
                .setPwdSalt(salt);

        sysUserMapper.insert(entity);

        sysUserDeptRelationService.cleanAndBind(entity.getId(), request.getDeptId());

        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdate(AdminSysUserUpsertRequest request) {
        log.info("[后台管理-修改后台用户] >> 入参={}", request);
        checkExistence(request.getId());
        preUpdateCheck(request.getId(), request.getStatus());
        checkRepeat(request);

        SysUserEntity entity = new SysUserEntity();
        BeanUtil.copyProperties(request, entity);

        sysUserDeptRelationService.cleanAndBind(request.getId(), request.getDeptId());

        sysUserMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        log.info("[后台管理-删除后台用户] >> 入参={}", ids);
        preDeleteCheck(ids);
        sysUserMapper.deleteByIds(ids);
    }

    @Override
    public SysUserLoginBO adminLogin(SysUserLoginDTO dto) {
        /*
        如果启用了多租户功能，并且前端指定了租户ID，则先查库确认租户是否有效
        注意：数据源级多租户，登录前【必须】主动指定租户ID，如: dto.setTenantId(101L)
         */
        // ConcurrentHashMap 的 value 不能为 null，还是 new 一个吧
        TenantContext tenantContext = new TenantContext();
        if (isTenantEnabled && ObjectUtil.isNotNull(dto.getTenantId())) {
            tenantContext = this.checkAndGetTenantContext(dto.getTenantId());
            // 验证通过，将所属租户写入租户上下文，使得 SQL 拦截器可以正确执行
            TenantContextHolder.setTenantContext(tenantContext);
        }

        // 不要直接提示“账号不存在”或“密码不正确”，避免撞库攻击
        SysUserEntity sysUserEntity = this.getUserByPin(dto.getUsername());
        if (sysUserEntity == null) {
            throw new BusinessException(SysErrorCodeEnum.A01002);
        }

        if (!PwdUtil.encrypt(dto.getPassword(), sysUserEntity.getPwdSalt()).equals(sysUserEntity.getPwd())) {
            throw new BusinessException(SysErrorCodeEnum.A01002);
        }

        if (SysUserStatusEnum.BANNED == sysUserEntity.getStatus()) {
            throw new BusinessException(SysErrorCodeEnum.A01003);
        }

        /*
        以上为有效性校验, 进入实际业务逻辑
        ---------------------------------------------------
         */

        if (isTenantEnabled && ObjectUtil.isNotNull(sysUserEntity.getTenantId())) {
            // 二次赋值，以防万一（也许前端没有传租户ID，上方的 if 块并没有执行）
            tenantContext = this.checkAndGetTenantContext(sysUserEntity.getTenantId());
            TenantContextHolder.setTenantContext(tenantContext);
        }

        this.updateLastLoginAt(sysUserEntity.getId(), LocalDateTimeUtil.now());

        // 取账号完整信息
        SysUserBO sysUserBO = this.entity2BO(sysUserEntity, false);
        Map<Long, String> roleMap = sysRoleService.getRoleMapByUserId(sysUserBO.getId());

        Map<Long, Set<String>> roleIdPermissionMap = sysMenuService.getRoleIdPermissionMap(roleMap.keySet());

        // 包装返回体；有的字段类型不一致, 单独转换
        SysUserLoginBO ret = new SysUserLoginBO();
        BeanUtil.copyProperties(sysUserBO, ret);

        Set<String> permissions = roleIdPermissionMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());

        ret
                .setRoleIds(new HashSet<>(roleMap.keySet()))
                .setRoles(new ArrayList<>(roleMap.values()))
                .setPermissions(permissions)
                .setRoleIdPermissionMap(roleIdPermissionMap)
                .setTenantContext(tenantContext);

        return ret;
    }

    @Override
    public VbenAdminUserInfoVO adminGetCurrentUserInfo() {
        SysUserBO sysUserBO = this.getNonnullById(UserContextHolder.getUserId());
        return VbenAdminUserInfoVO.builder()
                .username(sysUserBO.getUsername())
                .nickname(sysUserBO.getNickname())
                .lastLoginAt(sysUserBO.getLastLoginAt())
                .gender(sysUserBO.getGender())
                .email(sysUserBO.getEmail())
                .phoneNo(sysUserBO.getPhoneNo())
                .avatar(sysUserBO.getAvatarUrl())
                .build();
    }

    @Override
    public void adminResetUserPassword(AdminResetSysUserPasswordDTO dto) {
        preUpdateCheck(dto.getUserId(), null);
        SysUserEntity sysUserEntity = sysUserMapper.selectById(dto.getUserId());

        SysUserEntity templateEntity = new SysUserEntity();
        templateEntity
                .setPwd(PwdUtil.encrypt(dto.getRandomPassword(), sysUserEntity.getPwdSalt()))
                .setId(dto.getUserId());

        sysUserMapper.updateById(templateEntity);
    }

    @Override
    public void adminUpdateCurrentUserPassword(AdminUpdateCurrentSysUserPasswordDTO dto) {
        SysUserEntity sysUserEntity = sysUserMapper.selectById(UserContextHolder.getUserId());
        if (sysUserEntity == null || !sysUserEntity.getPwd().equals(PwdUtil.encrypt(dto.getOld(), sysUserEntity.getPwdSalt()))) {
            throw new BusinessException(SysErrorCodeEnum.A01004);
        }

        sysUserEntity
                .setPwd(PwdUtil.encrypt(dto.getConfirmNeo(), sysUserEntity.getPwdSalt()))
                .setId(UserContextHolder.getUserId());

        sysUserMapper.updateById(sysUserEntity);
    }

    @Override
    public void adminBindRoles(AdminBindUserRoleRelationDTO dto) {
        preBindUserRoleRelationCheck(dto);
        sysUserRoleRelationService.cleanAndBind(dto.getUserId(), dto.getRoleIds());
    }

    @Override
    public SysUserEntity getUserByPin(String pin) {
        return sysUserMapper.getUserByPin(pin);
    }

    @Override
    public Set<Long> listRelatedRoleIds(Long userId) {
        if (ObjectUtil.isNull(userId)) {
            return Collections.emptySet();
        }
        return sysRoleService.getRoleMapByUserId(userId).keySet();
    }

    @Override
    public List<Long> listUserIdsByTenantId(Long tenantId, Collection<EnabledStatusEnum> statusEnums) {
        if (Objects.isNull(tenantId)) {
            return List.of();
        }
        // 备份原始租户上下文；以下查询方式可同时兼容行级、数据源级多租户
        TenantContext originContext = TenantContextHolder.getTenantContext();
        try {
            // 临时切换租户
            TenantContextHolder.setTenantContext(new TenantContext(tenantId, CharSequenceUtil.EMPTY));
            return sysUserMapper.selectIds(statusEnums);
        } finally {
            TenantContextHolder.setTenantContext(originContext);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdateCurrentUserInfo(AdminUpdateCurrentSysUserInfoDTO dto) {
        SysUserEntity update = SysUserEntity.of(dto);
        update.setId(UserContextHolder.getUserId());
        sysUserMapper.updateById(update);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdateCurrentUserAvatar(AdminUpdateCurrentSysUserAvatarDTO dto) {
        dto.securityCheck();

        SysUserEntity update = SysUserEntity.of(dto);
        update.setId(UserContextHolder.getUserId());
        sysUserMapper.updateById(update);
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */


    /**
     * 实体转值对象
     *
     * @param entity       实体
     * @param fillDeptInfo 是否根据实体部门ID，查询关联部门信息并填充到BO
     */
    SysUserBO entity2BO(SysUserEntity entity, boolean fillDeptInfo) {
        if (entity == null) {
            return null;
        }

        SysUserBO bo = new SysUserBO();
        BeanUtil.copyProperties(entity, bo);

        // 按需改写字段
        bo.setUsername(entity.getPin());
        if (fillDeptInfo) {
            Optional.ofNullable(sysDeptService.getSpecifiedUserDeptContainer(bo.getId(), false))
                    .map(UserDeptContainer::primaryRelatedDept)
                    .ifPresent(deptInfo -> bo.setDeptId(deptInfo.getId()).setDeptTitle(deptInfo.getTitle()));
        }
        return bo;
    }

    /**
     * 实体转值对象
     *
     * @param entityList   实体 List
     * @param fillDeptInfo 是否根据实体部门ID，查询关联部门信息并填充到BO List
     */
    List<SysUserBO> entityList2BOs(List<SysUserEntity> entityList, boolean fillDeptInfo) {
        if (CollUtil.isEmpty(entityList)) {
            return List.of();
        }

        // 深拷贝
        List<SysUserBO> ret = new ArrayList<>(entityList.size());
        entityList.forEach(
                entity -> ret.add(this.entity2BO(entity, fillDeptInfo))
        );

        return ret;
    }

    /**
     * 实体转值对象
     *
     * @param entityPage   实体分页
     * @param fillDeptInfo 是否根据实体部门ID，查询关联部门信息并填充到BO 分页
     */
    PageResult<SysUserBO> entityPage2BOPage(Page<SysUserEntity> entityPage, boolean fillDeptInfo) {
        return new PageResult<SysUserBO>()
                .setCurrent(entityPage.getCurrent())
                .setSize(entityPage.getSize())
                .setTotal(entityPage.getTotal())
                .setRecords(this.entityList2BOs(entityPage.getRecords(), fillDeptInfo));
    }

    /**
     * 检查是否存在重复
     */
    private void checkRepeat(AdminSysUserUpsertRequest request) {
        SysUserEntity existingEntity = this.getUserByPin(request.getPin());

        if (existingEntity != null && !existingEntity.getId().equals(request.getId())) {
            throw new BusinessException(400, "已存在相同账号，请重新输入");
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
     * 确定本部门及下级部门用户IDs
     * 返回空集合代表不限制
     * 返回[0]或有元素集合，表示有限制
     */
    Set<Long> determineVisibleDeptUserIds() {
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
    Set<Long> determineInvisibleUserIds() {
        Set<Long> invisibleRoleIds = sysRoleService.determineInvisibleRoleIds();
        return sysUserRoleRelationService.listUserIdsByRoleIds(invisibleRoleIds);
    }

    /**
     * 数据越权检查
     */
    void dataScopeCheck(Collection<Long> userIds) {
        Set<Long> visibleUserIds = determineVisibleDeptUserIds();
        Set<Long> invisibleUserIds = determineInvisibleUserIds();
        if (CollUtil.isNotEmpty(visibleUserIds) && !CollUtil.containsAll(visibleUserIds, userIds)
                || CollUtil.isNotEmpty(invisibleUserIds) && CollUtil.containsAny(invisibleUserIds, userIds)) {
            throw new BusinessException(SysErrorCodeEnum.A01021);
        }
    }

    /**
     * 检查并获取租户上下文 bean，无效或被禁用则直接抛出异常
     *
     * @param tenantId 租户ID
     * @return TenantContext
     */
    TenantContext checkAndGetTenantContext(Long tenantId) throws BusinessException {
        // 查询租户是否仍有效
        TenantMetaEntity tenantEntity = sysTenantService.getTenantEntityByTenantId(tenantId);
        if (tenantEntity == null) {
            throw new BusinessException(SysErrorCodeEnum.INVALID_TENANT);
        }

        if (EnabledStatusEnum.DISABLED == tenantEntity.getStatus()) {
            throw new BusinessException(SysErrorCodeEnum.DISABLED_TENANT);
        }

        return TenantContext.builder()
                .tenantId(tenantEntity.getTenantId())
                .tenantName(tenantEntity.getTenantName())
                .build();
    }

    void updateLastLoginAt(Long userId, LocalDateTime lastLoginAt) {
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
    void preUpdateCheck(Long specifiedUserId, @Nullable SysUserStatusEnum statusEnum) {
        UserRoleContainer currentUser = sysRoleService.getCurrentUserRoleContainer();
        if (currentUser.isSuperAdmin()) {
            // 超级管理员除禁用自己外为所欲为
            if (statusEnum == SysUserStatusEnum.BANNED && Objects.equals(specifiedUserId, UserContextHolder.getUserId())) {
                throw new BusinessException(SysErrorCodeEnum.A01020);
            }
            return;
        }

        if (Objects.equals(specifiedUserId, UserContextHolder.getUserId())) {
            // 不能动自身用户
            throw new BusinessException(SysErrorCodeEnum.A01020);
        }

        // 目标是超级管理员or租户管理员时，均不能编辑
        UserRoleContainer specifiedUser = sysRoleService.getSpecifiedUserRoleContainer(specifiedUserId);
        if (specifiedUser.isSuperAdmin() || specifiedUser.isTenantAdmin()) {
            throw new BusinessException(SysErrorCodeEnum.A01021);
        }

        dataScopeCheck(Collections.singleton(specifiedUserId));
        // 暂未实现角色层级，一律平级
    }

    /**
     * 删除后台用户前检查
     */
    void preDeleteCheck(Collection<Long> ids) {
        if (CollUtil.contains(ids, UserContextHolder.getUserId())) {
            // 不能动自身用户
            throw new BusinessException(SysErrorCodeEnum.A01020);
        }

        // 目标是超级管理员时，不能删除
        List<UserRoleContainer> specifiedUsers = ids.stream().map(sysRoleService::getSpecifiedUserRoleContainer).toList();
        if (specifiedUsers.stream().anyMatch(UserRoleContainer::isSuperAdmin)) {
            throw new BusinessException(SysErrorCodeEnum.A01021);
        }

        // 只有超级管理员可以删租户管理员用户
        UserRoleContainer currentUser = sysRoleService.getCurrentUserRoleContainer();
        if (specifiedUsers.stream().anyMatch(UserRoleContainer::isTenantAdmin) && !currentUser.isSuperAdmin()) {
            throw new BusinessException(SysErrorCodeEnum.A01021);
        }

        dataScopeCheck(ids);
        // 暂未实现角色层级，一律平级
    }

    /**
     * 绑定后台用户与角色关联关系前检查
     * 防止越权访问漏洞
     */
    void preBindUserRoleRelationCheck(AdminBindUserRoleRelationDTO dto) {
        UserRoleContainer currentUser = sysRoleService.getCurrentUserRoleContainer();
        // 是否对自己操作
        boolean selfFlag = Objects.equals(dto.getUserId(), UserContextHolder.getUserId());
        if (currentUser.isSuperAdmin()) {
            // 超级管理员不能去掉自己的超级管理员角色
            if (selfFlag && !CollUtil.contains(dto.getRoleIds(), SysConstant.SUPER_ADMIN_ROLE_ID)) {
                throw new BusinessException(SysErrorCodeEnum.A01020);
            }
            // 也不能赋予其他人超级管理员角色
            if (!selfFlag && CollUtil.contains(dto.getRoleIds(), SysConstant.SUPER_ADMIN_ROLE_ID)) {
                throw new BusinessException(SysErrorCodeEnum.A01021);
            }
            return;
        }

        if (selfFlag) {
            // 不能动自身用户
            throw new BusinessException(SysErrorCodeEnum.A01020);
        }

        // 目标已经是超级管理员or租户管理员时，均不能绑定
        UserRoleContainer specifiedUser = sysRoleService.getSpecifiedUserRoleContainer(dto.getUserId());
        if (specifiedUser.isSuperAdmin() || specifiedUser.isTenantAdmin()) {
            throw new BusinessException(SysErrorCodeEnum.A01021);
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
    void currentUserNotSuperAdmin(AdminBindUserRoleRelationDTO dto, UserRoleContainer currentUser) {
        if (CollUtil.isNotEmpty(dto.getRoleIds()) && !currentUser.isSuperAdmin()) {
            boolean overRoles = !CollUtil.containsAll(currentUser.getRelatedRoleIds(), dto.getRoleIds());
            if (overRoles && currentUser.isNotAnyAdmin()) {
                // 普通用户超自身角色授予了；如果当前用户拥有新角色的所有菜单，那么也放行
                Set<Long> grantedMenuIds = sysRoleMenuRelationService.listMenuIdsByRoleIds(currentUser.getRelatedRoleIds());
                Set<Long> needMenuIds = sysRoleMenuRelationService.listMenuIdsByRoleIds(dto.getRoleIds());
                if (!CollUtil.containsAll(grantedMenuIds, needMenuIds)) {
                    throw new BusinessException(SysErrorCodeEnum.A01022);
                }
            }

            if (currentUser.isTenantAdmin()) {
                // 超自身权限，但作为租户管理员有额外情况
                Set<Long> invisibleRoleIds = sysRoleService.determineInvisibleRoleIds();
                // 除非超越了可见角色IDs授予 or 想要授予用户租户管理员角色，否则不管
                invisibleRoleIds.addAll(currentUser.getRelatedRoleIds());
                if (CollUtil.containsAny(invisibleRoleIds, dto.getRoleIds())) {
                    throw new BusinessException(SysErrorCodeEnum.A01022);
                }
            }
        }
    }

}
