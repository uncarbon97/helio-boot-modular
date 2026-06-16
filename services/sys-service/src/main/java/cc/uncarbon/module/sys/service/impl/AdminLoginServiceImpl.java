package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.enums.ErrorCodeEnum;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.module.commons.enums.UserTypeCodeEnum;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import cc.uncarbon.module.sys.enums.LoginLogTypeEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
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
import cn.hutool.core.date.LocalDateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

        SysUserEntity userEntity = null;
        boolean loginSuccessFlag = false;
        ErrorCodeEnum loginFailedErrorCode = null;
        try {
            // 切换租户态
            TenantContext tenantContext =
                    new SimpleTenantContext(tenant.getTenantId(), tenant.getTenantName(), tenant.getTenantCode());
            TenantContextHolder.setTenantContext(tenantContext);

            // 不要直接提示「账号不存在」or「密码不正确」，避免撞库攻击
            userEntity = sysUserMapper.getByPin(request.getPin());
            if (userEntity == null) {
                throw new BusinessException(SysErrorCodeEnum.A01001);
            }

            if (!PwdUtil.encrypt(request.getPwd(), userEntity.getPwdSalt()).equals(userEntity.getPwd())) {
                throw new BusinessException(SysErrorCodeEnum.A01001);
            }

            if (SysUserStatusEnum.BANNED == userEntity.getStatus()) {
                throw new BusinessException(SysErrorCodeEnum.A01004);
            }

            UserRoleScope userRole = userRoleHelper.getSpecifiedUserRole(userEntity.getId());
            Map<Long, Set<String>> permByRole = sysMenuService.getPermissionsByRole(userRole.getRelatedRoleIds());

            AdminLoginResult ret = new AdminLoginResult();
            BeanUtil.copyProperties(userEntity, ret);
            ret.setRoleIds(userRole.getRelatedRoleIds())
                    .setRoleCodes(userRole.getRelatedRoles().stream().map(SysRoleEntity::getCode).toList())
                    .setPermissions(permByRole.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()))
                    .setRolePermissionMap(permByRole)
                    .setTenantContext(tenantContext);

            sysUserMapper.updateLastLoginAt(userEntity.getId(), LocalDateTimeUtil.now());
            loginSuccessFlag = true;
            return ret;
        } catch (BusinessException be) {
            loginFailedErrorCode = be.getErrorCodeEnum();
            throw be;
        } finally {
            // 无论登录成功还是失败，都保存登录记录
            SysLoginLogCreateRequest logRequest = new SysLoginLogCreateRequest()
                    .setLoginLogType(LoginLogTypeEnum.LOGIN)
                    .setUserPin(request.getPin())
                    .setUserTypeCode(UserTypeCodeEnum.ADMIN_USER.getValue())
                    .setVisitorContext(visitorContext)
                    .setResultStatus(loginSuccessFlag ? LogResultStatusEnum.SUCCESS : LogResultStatusEnum.FAILED);
            if (userEntity != null) {
                logRequest.setUserId(userEntity.getId());
            }
            if (loginFailedErrorCode != null) {
                logRequest.setFailedMsg(loginFailedErrorCode.getErrorMsgFriendly());
            }
            sysLoginLogService.create(logRequest);
            TenantContextHolder.clear();
        }
    }
}
