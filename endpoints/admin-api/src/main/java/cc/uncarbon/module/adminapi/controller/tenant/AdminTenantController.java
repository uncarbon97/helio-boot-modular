package cc.uncarbon.module.adminapi.controller.tenant;

import cc.uncarbon.framework.helium.base.constant.PermissionPattern;
import cc.uncarbon.framework.helium.base.page.PageResult;
import cc.uncarbon.framework.helium.web.model.response.ApiResult;
import cc.uncarbon.module.adminapi.event.KickOutSysUsersEvent;
import cc.uncarbon.module.commons.constant.ApiPathPrefix;
import cc.uncarbon.module.commons.model.request.IdsRequest;
import cc.uncarbon.module.commons.satoken.StpLoginType;
import cc.uncarbon.module.tenant.facade.SysTenantFacade;
import cc.uncarbon.module.tenant.model.query.AdminTenantMetaListQuery;
import cc.uncarbon.module.tenant.model.request.AdminCreateTenantRequest;
import cc.uncarbon.module.tenant.model.request.AdminUpdateTenantMetaRequest;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaDTO;
import cc.uncarbon.module.tenant.model.valueobj.TenantMetaKickOutUsersBO;
import cc.uncarbon.module.tenant.service.TenantMetaService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.extra.spring.SpringUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@SaCheckLogin(type = StpLoginType.ADMIN)
@Tag(name = "租户管理-租户管理")
@RequestMapping(value = ApiPathPrefix.ADMIN + "/v1/tenant")
@RequiredArgsConstructor
@RestController
@Slf4j
public class AdminTenantController {

    private static final String PERMISSION_PREFIX = "Tenant:";

    private final TenantMetaService tenantMetaService;
    private final SysTenantFacade sysTenantFacade;


    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "分页查询")
    @PostMapping(value = "/list")
    public ApiResult<PageResult<TenantMetaDTO>> list(@RequestBody @Valid AdminTenantMetaListQuery query) {
        return ApiResult.success(tenantMetaService.adminList(query));
    }

    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.READ)
    @Operation(summary = "详情")
    @PostMapping(value = "/detail")
    public ApiResult<TenantMetaDTO> detail(@RequestParam Long id) {
        return ApiResult.success(tenantMetaService.getNonnullById(id));
    }

    // @SysOperateLog(value = "新增系统租户")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.CREATE)
    @Operation(summary = "新增")
    @PostMapping(value = "/create")
    public ApiResult<Void> insert(@RequestBody @Valid AdminCreateTenantRequest request) {
        sysTenantFacade.adminCreate(request);

        return ApiResult.success();
    }

    // @SysOperateLog(value = "编辑系统租户")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.UPDATE)
    @Operation(summary = "编辑")
    @PostMapping(value = "/update")
    public ApiResult<Void> update(@RequestBody @Valid AdminUpdateTenantMetaRequest request) {
        TenantMetaKickOutUsersBO needKickOutUsers = sysTenantFacade.adminUpdate(request);

        // 强制登出所有租户用户
        SpringUtil.publishEvent(new KickOutSysUsersEvent(
                new KickOutSysUsersEvent.EventData(needKickOutUsers.getSysUserIds())
        ));

        return ApiResult.success();
    }

    // @SysOperateLog(value = "删除系统租户")
    @SaCheckPermission(type = StpLoginType.ADMIN, value = PERMISSION_PREFIX + PermissionPattern.DELETE)
    @Operation(summary = "删除")
    @PostMapping(value = "/delete")
    public ApiResult<Void> delete(@RequestBody @Valid IdsRequest<Long> request) {
        TenantMetaKickOutUsersBO needKickOutUsers = sysTenantFacade.adminDelete(request.getIds());

        // 强制登出所有租户用户
        SpringUtil.publishEvent(new KickOutSysUsersEvent(
                new KickOutSysUsersEvent.EventData(needKickOutUsers.getSysUserIds())
        ));

        return ApiResult.success();
    }

}
