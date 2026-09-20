package cc.uncarbon.module.adminapi.controller.tenant;

import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.annotation.SysOperateLog;
import cc.uncarbon.module.adminapi.helper.TenantSwitchRegistry;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.model.internal.TenantSwitchInfo;
import cc.uncarbon.module.sys.model.request.AdminTenantSwitchRequest;
import cc.uncarbon.module.sys.model.valueobj.AdminTenantContextVO;
import cc.uncarbon.module.sys.service.AdminLoginService;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 超级管理员：平台视角登录后显式切入/退出租户视角，权限保持超管本人，全程审计
 * 用户优先模式（USER_FIRST）用户：在自身归属租户间切换，权限快照按目标租户重建
 * 租户域查询经 {@link TenantFacade}，用户域信息经 {@link TenantUserRoleFacade}；
 * 鉴权在服务层做活体校验（防会话快照陈旧），故此处仅要求登录态
 */
@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "租户管理-" + AdminTenantSwitchController.BIZ_TYPE)
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/tenant/switch")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminTenantSwitchController {

    static final String BIZ_TYPE = "切换租户";

    private final TenantFacade tenantFacade;
    private final TenantUserRoleFacade tenantUserRoleFacade;
    private final AdminLoginService adminLoginService;
    private final TenantSwitchRegistry tenantSwitchRegistry;


    @Operation(summary = "可切换租户列表")
    @PostMapping(value = "/switchable")
    public ApiResult<List<TenantContext>> switchable() {
        return ApiResult.success(tenantFacade.listSelectableTenants(UserContextHolder.getUserId()));
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "切换租户视角",
            bizNo = "{{#request.tenantCode}}", success = "切换租户视角：{{#request.tenantCode}}")
    @Operation(summary = "切换租户视角")
    @PostMapping(value = "/exec")
    public ApiResult<AdminTenantContextVO> switchTenant(@RequestBody @Valid AdminTenantSwitchRequest request) {
        final StpLogic stpUtil = StpKit.ADMIN;
        long loginId = stpUtil.getLoginIdAsLong();
        SaSession session = stpUtil.getSession();

        TenantContext target = tenantFacade.resolveSwitchTarget(request.getTenantCode());
        tenantFacade.assertSwitchable(loginId, target.getTenantId());

        TenantContext current = readTenantContext(session);
        if (current != null && Objects.equals(current.getTenantId(), target.getTenantId())) {
            // 幂等：已处于目标租户视角
            return ApiResult.success(currentContextVO(session));
        }

        if (!tenantUserRoleFacade.isSuperAdmin(loginId)) {
            // 非超管（用户优先模式用户）：按目标租户重建权限快照；用户已禁用时强制登出
            UserContext fresh = adminLoginService.buildSessionUserContext(loginId, target.getTenantId());
            if (fresh == null) {
                stpUtil.logout();
                throw new BusinessException(SysErrorCodeEnum.A01002);
            }
            if (fresh.getRoleIds() == null || fresh.getRoleIds().isEmpty()) {
                // 与登录口径一致：无可用角色时拒绝，避免产生零权限会话
                throw new BusinessException(SysErrorCodeEnum.A01005);
            }
            session.set(UserContext.CAMEL_NAME, fresh);
        }
        // 超管：保留本人权限快照，仅重写租户上下文（对应「切换视角、保留超管权限」的既定决策）

        // 切换标记：首次切换记录切换前上下文，T1→T2 直切时保持原始视角不变
        TenantContext original = current;
        if (session.get(TenantSwitchInfo.CAMEL_NAME) instanceof TenantSwitchInfo info) {
            original = info.getOriginalTenantContext();
        }
        session.set(TenantSwitchInfo.CAMEL_NAME, new TenantSwitchInfo(original, Instant.now()));
        session.set(TenantContext.CAMEL_NAME, target);

        // 维护租户在会话登记，供租户禁用时强制登出
        if (current != null) {
            tenantSwitchRegistry.unregister(current.getTenantId(), loginId);
        }
        tenantSwitchRegistry.register(target.getTenantId(), loginId);

        return ApiResult.success(currentContextVO(session));
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "退出租户视角", success = "退出租户视角，回到默认视角")
    @Operation(summary = "退出切换")
    @PostMapping(value = "/exit-switch")
    public ApiResult<AdminTenantContextVO> exitSwitch() {
        final StpLogic stpUtil = StpKit.ADMIN;
        long loginId = stpUtil.getLoginIdAsLong();
        SaSession session = stpUtil.getSession();

        if (!(session.get(TenantSwitchInfo.CAMEL_NAME) instanceof TenantSwitchInfo)) {
            // 幂等：未处于切换态
            return ApiResult.success(currentContextVO(session));
        }

        TenantContext current = readTenantContext(session);

        // 恢复默认视角：超管=平台视角；普通用户=默认租户（用户优先模式关联 / 租户优先模式归属）或个人空间
        TenantContext restore = null;
        if (!tenantUserRoleFacade.isSuperAdmin(loginId)) {
            restore = tenantFacade.resolveDefaultTenant(loginId);
            UserContext fresh = restore != null
                    ? adminLoginService.buildSessionUserContext(loginId, restore.getTenantId())
                    : null;
            if (fresh == null || fresh.getRoleIds() == null || fresh.getRoleIds().isEmpty()) {
                // 无默认租户（个人空间）或用户已禁用，强制登出，避免残留旧租户权限快照
                stpUtil.logout();
                throw new BusinessException(SysErrorCodeEnum.A01002);
            }
            session.set(UserContext.CAMEL_NAME, fresh);
        }

        if (restore != null) {
            session.set(TenantContext.CAMEL_NAME, restore);
        } else {
            session.delete(TenantContext.CAMEL_NAME);
        }
        session.delete(TenantSwitchInfo.CAMEL_NAME);

        if (current != null) {
            tenantSwitchRegistry.unregister(current.getTenantId(), loginId);
        }
        if (restore != null) {
            tenantSwitchRegistry.register(restore.getTenantId(), loginId);
        }

        return ApiResult.success(currentContextVO(session));
    }

    @Operation(summary = "当前会话租户信息")
    @PostMapping(value = "/current")
    public ApiResult<AdminTenantContextVO> current() {
        return ApiResult.success(currentContextVO(StpKit.ADMIN.getSession()));
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private TenantContext readTenantContext(SaSession session) {
        return session.get(TenantContext.CAMEL_NAME) instanceof TenantContext t ? t : null;
    }

    /**
     * 按当前会话状态构造租户信息视图（横幅数据一次拿全）
     */
    private AdminTenantContextVO currentContextVO(SaSession session) {
        TenantContext t = readTenantContext(session);
        TenantSwitchInfo info = session.get(TenantSwitchInfo.CAMEL_NAME) instanceof TenantSwitchInfo i ? i : null;
        UserContext u = UserContextHolder.getContext();

        boolean platformView = t == null && u != null && u.getRoleCodes() != null
                && u.getRoleCodes().contains(SysConstant.SUPER_ADMIN_ROLE_CODE);

        return new AdminTenantContextVO()
                .setTenantId(t == null ? null : t.getTenantId())
                .setTenantCode(t == null ? null : t.getTenantCode())
                .setTenantName(t == null ? null : t.getTenantName())
                .setPlatformView(platformView)
                .setSwitched(info != null)
                .setOriginalTenantContext(info == null ? null : info.getOriginalTenantContext())
                .setSwitchedAt(info == null ? null : info.getSwitchedAt());
    }

}
