package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.errorcode.StructuredErrorCode;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.module.commons.enums.UserTypeCodeEnum;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import cc.uncarbon.module.sys.enums.LoginLogTypeEnum;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.helper.UserRoleHelper;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cc.uncarbon.module.sys.model.request.AdminAuthPasswordLoginRequest;
import cc.uncarbon.module.sys.model.request.SysLoginLogCreateRequest;
import cc.uncarbon.module.sys.model.response.AdminLoginResult;
import cc.uncarbon.module.sys.service.AdminLoginService;
import cc.uncarbon.module.sys.service.SysLoginLogService;
import cc.uncarbon.module.sys.service.SysMenuService;
import cc.uncarbon.module.sys.util.PwdUtil;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cc.uncarbon.module.tenant.model.valueobj.TenantValidateResult;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
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


    @Override
    public AdminLoginResult passwordLogin(AdminAuthPasswordLoginRequest request, VisitorContext visitorContext) {
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
                        if (ref.userEntity == null) {
                            throw new BusinessException(SysErrorCodeEnum.A01001);
                        }

                        if (!PwdUtil.encrypt(request.getPwd(), ref.userEntity.getPwdSalt()).equals(ref.userEntity.getPwd())) {
                            throw new BusinessException(SysErrorCodeEnum.A01001);
                        }

                        if (SysUserStatusEnum.DISABLED == ref.userEntity.getStatus()) {
                            throw new BusinessException(SysErrorCodeEnum.A01002);
                        }

                        UserRoleScope userRole = userRoleHelper.getSpecifiedUserRole(ref.userEntity.getId());
                        Map<Long, Set<String>> permByRole = sysMenuService.getPermissionsByRole(userRole.getRelatedRoleIds());

                        AdminLoginResult ret = new AdminLoginResult();
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
}
