package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.errorcode.StructuredErrorCode;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.module.commons.enums.UserTypeCodeEnum;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import cc.uncarbon.module.sys.enums.LoginLogTypeEnum;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.helper.UserRoleHelper;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cc.uncarbon.module.sys.model.request.AdminAuthPasswordLoginRequest;
import cc.uncarbon.module.sys.model.request.SysLoginLogCreateRequest;
import cc.uncarbon.module.sys.model.response.SysUserLoginResult;
import cc.uncarbon.module.sys.model.valueobj.AdminLoginTenantUIConfigVO;
import cc.uncarbon.module.sys.service.AdminLoginService;
import cc.uncarbon.module.sys.service.SysLoginLogService;
import cc.uncarbon.module.sys.service.SysMenuService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cc.uncarbon.module.sys.util.PwdUtil;
import cc.uncarbon.module.tenant.errorcode.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cc.uncarbon.module.tenant.model.valueobj.TenantValidateResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class AdminLoginServiceImpl implements AdminLoginService {


    private final SysUserMapper sysUserMapper;
    private final SysMenuService sysMenuService;
    private final SysLoginLogService sysLoginLogService;
    private final UserRoleHelper userRoleHelper;
    private final TenantFacade tenantFacade;
    private final TenantUserRoleFacade tenantUserRoleFacade;
    private final SysUserRoleRelationService sysUserRoleRelationService;


    @Override
    public SysUserLoginResult passwordLogin(AdminAuthPasswordLoginRequest request, VisitorContext visitorContext) {
        // 登录模式分支内聚：切换 USER_FIRST 时仅需补充对应分支实现，不散落 if-else
        return switch (tenantFacade.getLoginMode()) {
            case TENANT_FIRST -> tenantFirstPasswordLogin(request, visitorContext);
            case USER_FIRST -> userFirstPasswordLogin(request, visitorContext);
        };
    }

    /**
     * 租户优先登录（现行）：租户编码 + 账号 + 密码 三元素
     */
    private SysUserLoginResult tenantFirstPasswordLogin(AdminAuthPasswordLoginRequest request,
                                                        VisitorContext visitorContext) {
        TenantValidateResult tenant = tenantFacade.validateByCode(request.getTenantCode());
        if (!tenant.isValid()) {
            throw new BusinessException(tenant.getErrorCode());
        }

        // 临时在 lambda 中保存变量
        var ref = new Object() {
            SysUserEntity userEntity = null;
            boolean loginSuccessFlag = false;
        };
        StructuredErrorCode loginFailedErrorCode = null;
        try {
            var tenantContext =
                    new SimpleTenantContext(tenant.getTenantId(), tenant.getTenantName(), tenant.getTenantCode());
            return TenantContextHolder.callWithContext(
                    tenantContext,
                    () -> {
                        // 不要直接提示「账号不存在」or「密码不正确」，避免撞库攻击
                        ref.userEntity = sysUserMapper.getByPin(request.getPin());
                        checkUserEntity(request, ref.userEntity);

                        // 已禁用的角色不参与登录会话快照
                        UserRoleScope userRole = userRoleHelper.getSpecifiedUserRole(ref.userEntity.getId());
                        if (CollUtil.isEmpty(userRole.getRelatedRoleIds())
                                && CollUtil.isNotEmpty(sysUserRoleRelationService.listRoleIdsByUser(ref.userEntity.getId()))) {
                            // 有关联角色但均被禁用，拒绝登录，避免产生零权限会话
                            throw new BusinessException(SysErrorCodeEnum.A01005);
                        }
                        Map<Long, Set<String>> permByRole = sysMenuService.getPermissionsByRole(userRole.getRelatedRoleIds());

                        SysUserLoginResult ret = new SysUserLoginResult();
                        BeanUtil.copyProperties(ref.userEntity, ret);
                        ret.setRoleIds(userRole.getRelatedRoleIds())
                                .setRoleCodes(userRole.getRelatedRoles().stream().map(SysRoleEntity::getCode).toList())
                                .setPermissions(permByRole.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()))
                                .setPermByRole(permByRole)
                                .setTenantContext(tenantContext);

                        sysUserMapper.updateLastLoginAt(ref.userEntity.getId(), Instant.now());
                        ref.loginSuccessFlag = true;
                        return ret;
                    }
            );
        } catch (BusinessException be) {
            loginFailedErrorCode = be.getErrorCode();
            throw be;
        } catch (Exception e) {
            loginFailedErrorCode = SysErrorCodeEnum.B01001;
            log.error("[{}] 内部异常，{} >>", LoginLogTypeEnum.PASSWORD_LOGIN, e.getMessage(), e);
            throw new BusinessException(loginFailedErrorCode);
        } finally {
            // 无论登录成功还是失败，都保存登录记录
            saveLoginLog(request, visitorContext, ref.userEntity, ref.loginSuccessFlag,
                    loginFailedErrorCode, tenant.getTenantId());
        }
    }

    /**
     * 用户优先登录：账号 + 密码 二元素，登录后按关联租户选择激活
     *
     * <p>全局 getByPin（pin 全局唯一）→ 校验密码 → 候选租户 = sys_user.tenant_id（记忆版）优先、
     * 其余按 relation ID ASC 兜底，逐个尝试启用租户 → 自动激活首个可用租户并发 token，
     * 登录响应携带全部关联租户列表；多租户用户经 switch 接口换租户（与超管切换同机制）</p>
     */
    private SysUserLoginResult userFirstPasswordLogin(AdminAuthPasswordLoginRequest request,
                                                      VisitorContext visitorContext) {
        var ref = new Object() {
            SysUserEntity userEntity = null;
            Long activatedTenantId = null;
            boolean loginSuccessFlag = false;
        };
        StructuredErrorCode loginFailedErrorCode = null;
        try {
            // pin 全局唯一：忽略租户态全局查找账号
            ref.userEntity = TenantContextHolder.callIgnored(() -> sysUserMapper.getByPin(request.getPin()));
            checkUserEntity(request, ref.userEntity);

            // 候选顺序：记忆版租户优先，其余按 relation ID ASC（加入先后）
            List<Long> candidateTenantIds =
                    new ArrayList<>(tenantUserRoleFacade.listEnabledTenantIdsByUser(ref.userEntity.getId()));
            Long rememberedTenantId = ref.userEntity.getTenantId();
            if (rememberedTenantId != null) {
                candidateTenantIds.remove(rememberedTenantId);
                candidateTenantIds.addFirst(rememberedTenantId);
            }
            TenantContext activatedContext = null;
            for (Long tenantId : candidateTenantIds) {
                activatedContext = tenantFacade.resolveEnabledTenant(tenantId);
                if (activatedContext != null) {
                    break;
                }
            }
            if (activatedContext == null) {
                // 无可用租户：未归属任何租户 / 归属租户均已不可用
                throw new BusinessException(candidateTenantIds.isEmpty()
                        ? SysErrorCodeEnum.A01008 : TenantErrorCodeEnum.A03006);
            }
            ref.activatedTenantId = activatedContext.getTenantId();

            final TenantContext loginTenantContext = activatedContext;
            return TenantContextHolder.callWithContext(loginTenantContext, () -> {
                // 已禁用的角色不参与登录会话快照
                UserRoleScope userRole = userRoleHelper.getSpecifiedUserRole(ref.userEntity.getId());
                if (CollUtil.isEmpty(userRole.getRelatedRoleIds())
                        && CollUtil.isNotEmpty(sysUserRoleRelationService.listRoleIdsByUser(ref.userEntity.getId()))) {
                    // 有关联角色但均被禁用，拒绝登录，避免产生零权限会话
                    throw new BusinessException(SysErrorCodeEnum.A01005);
                }
                Map<Long, Set<String>> permByRole = sysMenuService.getPermissionsByRole(userRole.getRelatedRoleIds());

                SysUserLoginResult ret = new SysUserLoginResult();
                BeanUtil.copyProperties(ref.userEntity, ret);
                ret.setRoleIds(userRole.getRelatedRoleIds())
                        .setRoleCodes(userRole.getRelatedRoles().stream().map(SysRoleEntity::getCode).toList())
                        .setPermissions(permByRole.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()))
                        .setPermByRole(permByRole)
                        .setTenantContext(loginTenantContext)
                        .setSelectableTenants(tenantFacade.listSelectableTenants(ref.userEntity.getId()));

                sysUserMapper.updateLastLoginAt(ref.userEntity.getId(), Instant.now());
                // 记忆激活租户：与投影列不一致时回写（超级管理员在门面内豁免）
                tenantUserRoleFacade.rememberActiveTenant(ref.userEntity.getId(), loginTenantContext.getTenantId());
                ref.loginSuccessFlag = true;
                return ret;
            });
        } catch (BusinessException be) {
            loginFailedErrorCode = be.getErrorCode();
            throw be;
        } catch (Exception e) {
            loginFailedErrorCode = SysErrorCodeEnum.B01001;
            log.error("[{}] 内部异常，{} >>", LoginLogTypeEnum.PASSWORD_LOGIN, e.getMessage(), e);
            throw new BusinessException(loginFailedErrorCode);
        } finally {
            // 无论登录成功还是失败，都保存登录记录
            saveLoginLog(request, visitorContext, ref.userEntity, ref.loginSuccessFlag,
                    loginFailedErrorCode, ref.activatedTenantId);
        }
    }

    @Override
    public @Nullable UserContext buildSessionUserContext(Long userId) {
        // 任意线程可安全调用（异步刷新会话、切换租户等场景）：
        // 先忽略租户态取用户本体与归属租户，再在归属租户作用域内解析角色
        SysUserEntity userEntity;
        try {
            userEntity = TenantContextHolder.callIgnored(() -> sysUserMapper.selectById(userId));
        } catch (Exception e) {
            log.error("重建用户会话上下文失败 >> userId={}", userId, e);
            return null;
        }
        if (userEntity == null || SysUserStatusEnum.DISABLED == userEntity.getStatus()) {
            return null;
        }

        Long homeTenantId = userEntity.getTenantId();
        try {
            if (homeTenantId != null) {
                return TenantContextHolder.callWithContext(
                        new SimpleTenantContext(homeTenantId, null, null),
                        () -> assembleSessionUserContext(userEntity));
            }
            return assembleSessionUserContext(userEntity);
        } catch (Exception e) {
            log.error("重建用户会话上下文失败 >> userId={}", userId, e);
            return null;
        }
    }

    @Override
    public @Nullable UserContext buildSessionUserContext(Long userId, TenantContext tenantContext) {
        if (userId == null || tenantContext == null || tenantUserRoleFacade.isSuperAdmin(userId)) {
            // 超管权限与视角解耦：始终按归属租户解析本人快照
            return buildSessionUserContext(userId);
        }
        SysUserEntity userEntity;
        try {
            userEntity = TenantContextHolder.callIgnored(() -> sysUserMapper.selectById(userId));
        } catch (Exception e) {
            log.error("重建用户会话上下文失败 >> userId={}", userId, e);
            return null;
        }
        if (userEntity == null || SysUserStatusEnum.DISABLED == userEntity.getStatus()) {
            return null;
        }
        try {
            return TenantContextHolder.callWithContext(tenantContext, () -> assembleSessionUserContext(userEntity));
        } catch (Exception e) {
            log.error("重建用户会话上下文失败 >> userId={}", userId, e);
            return null;
        }
    }

    @Override
    public AdminLoginTenantUIConfigVO getTenantUIConfig() {
        boolean tenantEnabled = tenantFacade.isTenantEnabled();
        TenantLoginModeEnum loginMode = tenantFacade.getLoginMode();
        return new AdminLoginTenantUIConfigVO()
                .setShowTenantCodeInputFlag(tenantEnabled && TenantLoginModeEnum.TENANT_FIRST == loginMode);
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private void checkUserEntity(AdminAuthPasswordLoginRequest request, SysUserEntity userEntity) {
        if (userEntity == null) {
            throw new BusinessException(SysErrorCodeEnum.A01001);
        }
        if (!PwdUtil.verify(request.getPwd(), userEntity.getPwd())) {
            throw new BusinessException(SysErrorCodeEnum.A01001);
        }
        if (SysUserStatusEnum.DISABLED == userEntity.getStatus()) {
            throw new BusinessException(SysErrorCodeEnum.A01002);
        }
    }

    /**
     * 落登录日志
     */
    private void saveLoginLog(AdminAuthPasswordLoginRequest request, VisitorContext visitorContext,
                              @Nullable SysUserEntity userEntity, boolean loginSuccess,
                              @Nullable StructuredErrorCode loginFailedErrorCode, @Nullable Long tenantId) {
        SysLoginLogCreateRequest logRequest = new SysLoginLogCreateRequest()
                .setLoginLogType(LoginLogTypeEnum.PASSWORD_LOGIN)
                .setUserPin(request.getPin())
                .setUserTypeCode(UserTypeCodeEnum.ADMIN_USER.getValue())
                .setVisitorContext(visitorContext)
                .setResultStatus(loginSuccess ? LogResultStatusEnum.SUCCESS : LogResultStatusEnum.FAILED)
                .setTenantId(tenantId != null ? tenantId : 0L);
        if (userEntity != null) {
            logRequest.setUserId(userEntity.getId());
        }
        if (loginFailedErrorCode != null) {
            logRequest.setFailedMsg(loginFailedErrorCode.getErrorMsgFriendly());
        }
        sysLoginLogService.create(logRequest);
    }

    /**
     * 在当前（已按归属租户建立的）作用域内组装会话用户上下文
     */
    private UserContext assembleSessionUserContext(SysUserEntity userEntity) {
        UserRoleScope userRole = userRoleHelper.getSpecifiedUserRole(userEntity.getId());
        return new SimpleUserContext()
                .setUserId(userEntity.getId())
                .setUserPin(userEntity.getPin())
                .setUserTypeCode(UserTypeCodeEnum.ADMIN_USER.getValue())
                .setRoleIds(userRole.getRelatedRoleIds())
                .setRoleCodes(userRole.getRelatedRoles().stream().map(SysRoleEntity::getCode).toList())
                .setUserPhoneNo(userEntity.getPhoneNo())
                .setUserNickname(userEntity.getNickname());
    }
}
