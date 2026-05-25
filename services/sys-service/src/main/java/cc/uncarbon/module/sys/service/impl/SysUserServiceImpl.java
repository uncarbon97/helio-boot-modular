package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.core.context.TenantContext;
import cc.uncarbon.framework.core.context.TenantContextHolder;
import cc.uncarbon.framework.core.context.UserContextHolder;
import cc.uncarbon.framework.core.enums.EnabledStatusEnum;
import cc.uncarbon.framework.core.exception.BusinessException;
import cc.uncarbon.framework.core.page.PageParam;
import cc.uncarbon.framework.core.page.PageResult;
import cc.uncarbon.framework.core.props.HeliumProperties;
import cc.uncarbon.module.sys.entity.SysTenantEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.enums.SysErrorEnum;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.model.interior.UserDeptContainer;
import cc.uncarbon.module.sys.model.request.*;
import cc.uncarbon.module.sys.model.response.SysUserBO;
import cc.uncarbon.module.sys.model.response.SysUserLoginBO;
import cc.uncarbon.module.sys.model.response.VbenAdminUserInfoVO;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cc.uncarbon.module.sys.service.SysRoleService;
import cc.uncarbon.module.sys.service.SysMenuService;
import cc.uncarbon.module.sys.service.SysTenantService;
import cc.uncarbon.module.sys.service.SysUserDeptRelationService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cc.uncarbon.module.sys.service.SysUserService;
import cc.uncarbon.module.sys.util.PwdUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
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
    private final HeliumProperties heliumProperties;

    private boolean isTenantEnabled;

    @PostConstruct
    @Override
    public void postConstruct() {
        this.isTenantEnabled = heliumProperties.getTenant().getEnabled();
    }

    @Override
    public PageResult<SysUserBO> adminList(AdminListSysUserDTO dto) {
        // 预处理：根据【手动选择的部门】筛选用户
        Set<Long> deptUserIds = Collections.emptySet();
        if (dto.needFilterBySelectedDeptId()) {
            deptUserIds = sysUserDeptRelationService.listUserIdsByDeptIds(Collections.singleton(dto.getSelectedDeptId()));
            if (CollUtil.isEmpty(deptUserIds)) {
                // 【手动选择的部门】没有任何用户ID，直接返回空列表
                return new PageResult<>(pageParam);
            }
        }

        // 预处理：根据【只能看到本部门及下级部门原则】筛选用户
        Set<Long> visibleUserIds = determineVisibleDeptUserIds();
        if (Objects.equals(CollUtil.getFirst(visibleUserIds), BigInteger.ZERO.longValue())) {
            // 其实啥也看不到……
            return new PageResult<>(pageParam);
        }

        Set<Long> invisibleUserIds = determineInvisibleUserIds();
        Page<SysUserEntity> entityPage = sysUserMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                new QueryWrapper<SysUserEntity>()
                        .lambda()
                        // 手机号
                        .like(CharSequenceUtil.isNotBlank(dto.getPhoneNo()), SysUserEntity::getPhoneNo, CharSequenceUtil.cleanBlank(dto.getPhoneNo()))
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
    public SysUserBO getOneById(Long id) {
        return this.getOneById(id, false);
    }

    @Override
    public SysUserBO getOneById(Long id, boolean throwIfInvalidId) throws BusinessException {
        dataScopeCheck(Collections.singleton(id));

        SysUserEntity entity = sysUserMapper.selectById(id);
        if (throwIfInvalidId) {
            SysErrorEnum.INVALID_ID.assertNotNull(entity);
        }

        return this.entity2BO(entity, true);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long adminInsert(AdminInsertOrUpdateSysUserDTO dto) {
        log.info("[系统管理-新增后台用户] >> 入参={}", dto);
        checkRepeat(dto);

        if (Objects.nonNull(dto.getDeptId())) {
            // 对传入的部门ID，做数据越权检查
            UserDeptContainer deptContainer = sysDeptService.getCurrentUserDeptContainer(true);
            if (deptContainer.hasVisibleDepts() && !CollUtil.contains(deptContainer.getVisibleDeptIds(), dto.getDeptId())) {
                throw new BusinessException(SysErrorEnum.CANNOT_OPERATE_THIS_USER);
            }
        }

        dto.setId(null);
        SysUserEntity entity = new SysUserEntity();
        BeanUtil.copyProperties(dto, entity);

        String salt = IdUtil.randomUUID();
        entity
                .setSalt(salt)
                .setPin(dto.getUsername())
                .setPwd(PwdUtil.encrypt(dto.getPasswordOfNewUser(), salt));

        sysUserMapper.insert(entity);

        sysUserDeptRelationService.cleanAndBind(entity.getId(), dto.getDeptId());

        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminUpdate(AdminInsertOrUpdateSysUserDTO dto) {
        log.info("[系统管理-修改后台用户] >> 入参={}", dto);
        preUpdateCheck(dto.getId(), dto.getStatus());
        checkRepeat(dto);

        SysUserEntity entity = new SysUserEntity();
        BeanUtil.copyProperties(dto, entity);
        // 手动处理异名字段
        entity.setPin(dto.getUsername());

        sysUserDeptRelationService.cleanAndBind(dto.getId(), dto.getDeptId());

        sysUserMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void adminDelete(Collection<Long> ids) {
        log.info("[系统管理-删除后台用户] >> 入参={}", ids);
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
            throw new BusinessException(SysErrorEnum.INCORRECT_PIN_OR_PWD);
        }

        if (!PwdUtil.encrypt(dto.getPassword(), sysUserEntity.getSalt()).equals(sysUserEntity.getPwd())) {
            throw new BusinessException(SysErrorEnum.INCORRECT_PIN_OR_PWD);
        }

        if (SysUserStatusEnum.BANNED == sysUserEntity.getStatus()) {
            throw new BusinessException(SysErrorEnum.BANNED_USER);
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
        SysUserBO sysUserBO = this.getOneById(UserContextHolder.getUserId(), true);
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
                .setPwd(PwdUtil.encrypt(dto.getRandomPassword(), sysUserEntity.getSalt()))
                .setId(dto.getUserId());

        sysUserMapper.updateById(templateEntity);
    }

    @Override
    public void adminUpdateCurrentUserPassword(AdminUpdateCurrentSysUserPasswordDTO dto) {
        SysUserEntity sysUserEntity = sysUserMapper.selectById(UserContextHolder.getUserId());
        if (sysUserEntity == null || !sysUserEntity.getPwd().equals(PwdUtil.encrypt(dto.getOldPassword(), sysUserEntity.getSalt()))) {
            throw new BusinessException(SysErrorEnum.INCORRECT_OLD_PASSWORD);
        }

        sysUserEntity
                .setPwd(PwdUtil.encrypt(dto.getConfirmNewPassword(), sysUserEntity.getSalt()))
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

}
