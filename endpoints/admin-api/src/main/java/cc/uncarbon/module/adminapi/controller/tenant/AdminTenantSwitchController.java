package cc.uncarbon.module.adminapi.controller.tenant;

import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.annotation.SysOperateLog;
import cc.uncarbon.module.adminapi.helper.TenantSwitchHelper;
import cc.uncarbon.module.adminapi.model.internal.TenantSwitchInfo;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.sys.facade.TenantUserRoleFacade;
import cc.uncarbon.module.sys.model.request.AdminTenantSwitchEnterRequest;
import cc.uncarbon.module.sys.model.valueobj.AdminTenantContextVO;
import cc.uncarbon.module.sys.service.AdminLoginService;
import cc.uncarbon.module.tenant.constant.TenantConstant;
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
 * <p>TENANT_FIRST 阶段限超级管理员：切换至其他租户视角做跨租户管理，权限保持超管本人，全程审计；
 * USER_FIRST 切换时放开给普通用户（在本人关联租户间切换，权限快照按目标租户重建）。</p>
 *
 * <p>租户域查询经 {@link TenantFacade}；鉴权在服务层做活体校验（防会话快照陈旧），故此处仅要求登录态。</p>
 */
@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "租户管理-" + AdminTenantSwitchController.BIZ_TYPE)
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/tenant-switch")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminTenantSwitchController {

    static final String BIZ_TYPE = "切换租户";

    private final TenantFacade tenantFacade;
    private final TenantUserRoleFacade tenantUserRoleFacade;
    private final TenantSwitchHelper tenantSwitchHelper;
    private final AdminLoginService adminLoginService;


    @Operation(summary = "可切换租户列表")
    @PostMapping(value = "/switchable")
    public ApiResult<List<TenantContext>> switchable() {
        return ApiResult.success(tenantFacade.listSelectableTenants(UserContextHolder.getUserId()));
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "切换至租户",
            bizNo = "{{#request.tenantCode}}", success = "切换至租户：{{#request.tenantCode}}")
    @Operation(summary = "切换至租户")
    @PostMapping(value = "/enter")
    public ApiResult<AdminTenantContextVO> enter(@RequestBody @Valid AdminTenantSwitchEnterRequest request) {
        final StpLogic stpUtil = StpKit.ADMIN;
        final Long loginId = UserContextHolder.getUserId();
        SaSession session = stpUtil.getSession();

        // 活体校验：目标租户存在且启用 + 当前用户有权切入
        TenantContext target = tenantFacade.resolveSwitchTarget(request.getTenantCode());
        tenantFacade.assertSwitchable(loginId, target.getTenantId());

        TenantContext current = readTenantContext(session);
        if (current != null && Objects.equals(current.getTenantId(), target.getTenantId())) {
            // 幂等：已处于目标租户视角
            return ApiResult.success(toContext(session));
        }

        // 切换标记：首次切换记录切换前上下文，T1→T2 直切时保持原始视角不变
        TenantContext original = current;
        if (session.get(TenantSwitchInfo.CAMEL_NAME) instanceof TenantSwitchInfo info) {
            original = info.getOriginalTenantContext();
        }
        session.set(TenantSwitchInfo.CAMEL_NAME, new TenantSwitchInfo(original, Instant.now()));
        // 会话写入新租户上下文；本请求后续及下一次请求由 ContextBindingFilter 从会话恢复
        session.set(TenantContext.CAMEL_NAME, target);
        // 权限快照按目标租户重建（USER_FIRST 普通用户；超管在服务层保持本人权限）
        refreshSessionUserContext(session, loginId, target);
        // USER_FIRST：记忆激活租户，供下次登录首选（超级管理员在门面内豁免）
        tenantUserRoleFacade.rememberActiveTenant(loginId, target.getTenantId());

        // 维护租户在会话登记，供租户禁用时强制登出
        if (current != null) {
            tenantSwitchHelper.unregister(current.getTenantId(), loginId);
        }
        tenantSwitchHelper.register(target.getTenantId(), loginId);
        return current();
    }

    @SysOperateLog(bizType = BIZ_TYPE, behavior = "退出切换租户", success = "退出切换租户，回到默认视角")
    @Operation(summary = "退出切换租户")
    @PostMapping(value = "/exit")
    public ApiResult<AdminTenantContextVO> exit() {
        final StpLogic stpUtil = StpKit.ADMIN;
        final Long loginId = UserContextHolder.getUserId();
        SaSession session = stpUtil.getSession();

        if (!(session.get(TenantSwitchInfo.CAMEL_NAME) instanceof TenantSwitchInfo)) {
            // 幂等：未处于切换态
            return ApiResult.success(toContext(session));
        }

        TenantContext current = readTenantContext(session);

        // 恢复默认视角：TENANT_FIRST=登录租户（超管即平台自营域 0）；原始上下文缺失时回平台视角
        TenantContext restore;
        if (session.get(TenantSwitchInfo.CAMEL_NAME) instanceof TenantSwitchInfo info
                && info.getOriginalTenantContext() != null) {
            restore = info.getOriginalTenantContext();
        } else {
            restore = tenantFacade.resolveDefaultTenant(loginId);
        }

        if (restore != null) {
            session.set(TenantContext.CAMEL_NAME, restore);
            // 恢复视角后按恢复租户重建权限快照 + 记忆激活租户
            refreshSessionUserContext(session, loginId, restore);
            tenantUserRoleFacade.rememberActiveTenant(loginId, restore.getTenantId());
        } else {
            session.delete(TenantContext.CAMEL_NAME);
        }
        session.delete(TenantSwitchInfo.CAMEL_NAME);

        if (current != null) {
            tenantSwitchHelper.unregister(current.getTenantId(), loginId);
        }
        if (restore != null) {
            tenantSwitchHelper.register(restore.getTenantId(), loginId);
        }
        return current();
    }

    @Operation(summary = "当前会话租户信息")
    @PostMapping(value = "/current")
    public ApiResult<AdminTenantContextVO> current() {
        return ApiResult.success(toContext(StpKit.ADMIN.getSession()));
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 视角切换后按生效租户原位刷新会话权限快照；刷新失败（用户已不存在/被禁用）保留原快照并告警
     */
    private void refreshSessionUserContext(SaSession session, Long loginId, TenantContext tenantContext) {
        UserContext refreshed = adminLoginService.buildSessionUserContext(loginId, tenantContext);
        if (refreshed != null) {
            session.set(UserContext.CAMEL_NAME, refreshed);
        } else {
            log.warn("[{}] 切换后重建用户会话上下文失败，保留原快照 >> userId={}", BIZ_TYPE, loginId);
        }
    }

    private static TenantContext readTenantContext(SaSession session) {
        return session.get(TenantContext.CAMEL_NAME) instanceof TenantContext t ? t : null;
    }

    /**
     * 按当前会话状态构造租户信息视图
     */
    private static AdminTenantContextVO toContext(SaSession session) {
        TenantContext t = readTenantContext(session);
        TenantSwitchInfo info = session.get(TenantSwitchInfo.CAMEL_NAME) instanceof TenantSwitchInfo i ? i : null;

        // TENANT_FIRST 下超管登录即平台自营域，处于 0 号租户且未切换 = 平台视角
        boolean firstPartyView = t != null && info == null
                && Objects.equals(TenantConstant.FIRST_PARTY_TENANT_ID, t.getTenantId());

        return new AdminTenantContextVO()
                .setTenantCode(t == null ? null : t.getTenantCode())
                .setTenantName(t == null ? null : t.getTenantName())
                .setFirstPartyView(firstPartyView)
                .setSwitched(info != null);
    }

}
