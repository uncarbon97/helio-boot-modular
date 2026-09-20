package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.errorcode.StructuredErrorCode;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.module.commons.enums.UserTypeCodeEnum;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserRoleRelationMapper;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import cc.uncarbon.module.sys.enums.LoginLogTypeEnum;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.helper.UserRoleHelper;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cc.uncarbon.module.sys.model.request.AdminAuthPasswordLoginRequest;
import cc.uncarbon.module.sys.model.request.SysLoginLogCreateRequest;
import cc.uncarbon.module.sys.model.response.SysUserLoginResult;
import cc.uncarbon.module.sys.service.AdminLoginService;
import cc.uncarbon.module.sys.service.SysLoginLogService;
import cc.uncarbon.module.sys.service.SysMenuService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cc.uncarbon.module.sys.util.PwdUtil;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cc.uncarbon.module.tenant.model.valueobj.TenantLoginContextResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
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
    private final SysUserRoleRelationService sysUserRoleRelationService;
    private final SysUserRoleRelationMapper sysUserRoleRelationMapper;


    @Override
    public SysUserLoginResult passwordLogin(AdminAuthPasswordLoginRequest request, VisitorContext visitorContext) {
        // 临时在 lambda 中保存变量
        var ref = new Object() {
            SysUserEntity userEntity = null;
            boolean loginSuccessFlag = false;
        };
        StructuredErrorCode loginFailedErrorCode = null;
        try {
            /*
             * 第 1 步：全局定位用户
             * 行级隔离下无租户上下文的查询会拼接 tenant_id = NULL（恒假），跨租户读取必须显式忽略隔离，
             * 保证 NONE / LINE / DATASOURCE 各策略行为一致
             */
            ref.userEntity = TenantContextHolder.callIgnored(() -> sysUserMapper.getByPin(request.getPin()));
            // 不要直接提示「账号不存在」or「密码不正确」，避免撞库攻击
            SysErrorCodeEnum.A01001.throwIfNull(ref.userEntity);
            SysErrorCodeEnum.A01001.throwIf(!PwdUtil.verify(request.getPwd(), ref.userEntity.getPwd()));
            SysErrorCodeEnum.A01002.throwIf(ref.userEntity.getStatus() != SysUserStatusEnum.ENABLED);

            /*
             * 第 2 步：按身份与登录模式推导租户上下文
             */
            final boolean userIsSuperAdmin = SysConstant.SUPER_ADMIN_USER_ID.equals(ref.userEntity.getId());
            TenantContext tenantContext;
            List<TenantContext> tenantOptions;

            /*
             * 超管=平台视角
             * 用户优先模式=按用户-租户关联推导
             * 租户优先模式=编码与归属一致性校验
             */
            TenantLoginContextResult resolved = tenantFacade.resolveLoginTenant(
                    ref.userEntity.getId(), userIsSuperAdmin, request.getTenantCode(), ref.userEntity.getTenantId());
            tenantContext = resolved.getTenantContext();
            tenantOptions = resolved.getTenantOptions();

            if (!resolved.isTenantScopedRole() && tenantContext != null && tenantContext.getTenantId() != null) {
                // 租户优先模式：同一 pin 可能存在于多个租户，按 (pin, tenantId) 复核精确定位
                // （sys_user 定位需忽略隔离：无上下文下行级拦截器会拼 tenant_id = NULL，与显式条件冲突）
                SysUserEntity located = TenantContextHolder.callIgnored(() ->
                        sysUserMapper.getByPinAndTenantId(request.getPin(), tenantContext.getTenantId()));
                SysErrorCodeEnum.A01001.throwIfNull(located);
                ref.userEntity = located;
            }

            /*
             * 第 3 步：在生效租户上下文内重建角色快照与权限（不忽略租户隔离）
             * 平台视角/个人空间=无上下文，行级策略下仅平台域角色行（tenant_id=NULL，即超管角色）可见；
             * 进入租户=按目标租户过滤（sys_role 为租户表，行级拦截器天然限定，无需手工拼租户条件）
             */
            return TenantContextHolder.callWithContext(tenantContext, () -> {
                UserRoleScope userRole = userRoleHelper.getSpecifiedUserRole(ref.userEntity.getId());
                assertHasEnabledRole(ref.userEntity.getId(), userRole,
                        tenantContext == null ? null : tenantContext.getTenantId());

                Map<Long, Set<String>> permByRole = sysMenuService.getPermissionsByRole(userRole.getRelatedRoleIds());

                SysUserLoginResult ret = new SysUserLoginResult();
                BeanUtil.copyProperties(ref.userEntity, ret);
                ret.setRoleIds(userRole.getRelatedRoleIds())
                        .setRoleCodes(userRole.getRelatedRoles().stream().map(SysRoleEntity::getCode).toList())
                        .setPermissions(permByRole.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()))
                        .setPermByRole(permByRole)
                        .setTenantContext(tenantContext)
                        .setTenantOptions(tenantOptions);

                sysUserMapper.updateLastLoginAt(ref.userEntity.getId(), Instant.now());
                ref.loginSuccessFlag = true;
                return ret;
            });
        } catch (BusinessException be) {
            loginFailedErrorCode = be.getErrorCode();
            throw be;
        } catch (Exception e) {
            loginFailedErrorCode = SysErrorCodeEnum.B01001;
            throw new BusinessException(loginFailedErrorCode);
        } finally {
            // 无论登录成功还是失败，都保存登录记录
            SysLoginLogCreateRequest logRequest = new SysLoginLogCreateRequest()
                    .setLoginLogType(LoginLogTypeEnum.PASSWORD_LOGIN)
                    .setUserPin(request.getPin())
                    .setUserTypeCode(UserTypeCodeEnum.ADMIN_USER.getValue())
                    .setVisitorContext(visitorContext)
                    .setResultStatus(ref.loginSuccessFlag ? LogResultStatusEnum.SUCCESS : LogResultStatusEnum.FAILED);
            if (ref.userEntity != null) {
                logRequest.setUserId(ref.userEntity.getId());
            }
            if (loginFailedErrorCode != null) {
                logRequest.setFailedMsg(loginFailedErrorCode.getErrorMsgFriendly());
            }
            sysLoginLogService.create(logRequest);
        }
    }

    @Override
    public @Nullable UserContext buildSessionUserContext(Long userId) {
        return buildSessionUserContext(userId, null);
    }

    @Override
    public @Nullable UserContext buildSessionUserContext(Long userId, @Nullable Long tenantId) {
        SysUserEntity userEntity = ignoredCall(() -> sysUserMapper.selectById(userId));
        if (userEntity == null || SysUserStatusEnum.DISABLED == userEntity.getStatus()) {
            return null;
        }

        UserRoleScope userRole = ignoredCall(() -> userRoleHelper.getSpecifiedUserRole(userId, tenantId));
        return new SimpleUserContext()
                .setUserId(userEntity.getId())
                .setUserPin(userEntity.getPin())
                .setUserTypeCode(UserTypeCodeEnum.ADMIN_USER.getValue())
                .setRoleIds(userRole.getRelatedRoleIds())
                .setRoleCodes(userRole.getRelatedRoles().stream().map(SysRoleEntity::getCode).toList())
                .setUserPhoneNo(userEntity.getPhoneNo())
                .setUserNickname(userEntity.getNickname());
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 有关联角色但均被禁用时拒绝登录，避免产生零权限会话
     *
     * @param tenantId 为 null 时按全局角色关联判断
     */
    private void assertHasEnabledRole(Long userId, UserRoleScope userRole, @Nullable Long tenantId) {
        boolean hasAnyBinding = tenantId == null
                ? CollUtil.isNotEmpty(sysUserRoleRelationService.listRoleIdsByUser(userId))
                : CollUtil.isNotEmpty(sysUserRoleRelationMapper.listRoleIdsByUserAndTenant(userId, tenantId));
        if (CollUtil.isEmpty(userRole.getRelatedRoleIds()) && hasAnyBinding) {
            throw new BusinessException(SysErrorCodeEnum.A01005);
        }
    }

    /**
     * 在忽略租户隔离的作用域内执行，业务异常原样抛出，其余异常按系统错误包装
     */
    private <T> T ignoredCall(Callable<T> op) {
        try {
            return TenantContextHolder.callIgnored(op);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new IllegalStateException("登录服务执行失败", e);
        }
    }

}
