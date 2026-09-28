package cc.uncarbon.test.auth;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import cc.uncarbon.framework.helium.web.context.SimpleVisitorContext;
import cc.uncarbon.module.sys.dal.mapper.*;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.query.AdminSysLoginLogListQuery;
import cc.uncarbon.module.sys.model.request.AdminAuthPasswordLoginRequest;
import cc.uncarbon.module.sys.model.response.SysUserLoginResult;
import cc.uncarbon.module.sys.service.AdminLoginService;
import cc.uncarbon.module.sys.service.SysLoginLogService;
import cc.uncarbon.module.tenant.dal.mapper.TenantMetaMapper;
import cc.uncarbon.module.tenant.facade.TenantFacade;
import cc.uncarbon.module.tenant.model.request.AdminTenantCreateRequest;
import cc.uncarbon.module.tenant.service.TenantService;
import cc.uncarbon.test.base.BaseIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.uncarbon.module.sys.dal.entity.SysUserTenantRelationEntity;
import cc.uncarbon.module.sys.dal.entity.SysLoginLogEntity;

/**
 * 登录集成测试：双登录模式（TENANT_FIRST / USER_FIRST 自动适配）、
 * 防枚举统一错误码、登录成功/失败均落日志
 */
@Tag("auth")
class AdminAuthIT extends BaseIntegrationTest {

    private static final String PWD = "it-pass-123456";

    @Resource
    private AdminLoginService adminLoginService;
    @Resource
    private TenantService tenantService;
    @Resource
    private SysLoginLogService sysLoginLogService;

    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysRoleMapper sysRoleMapper;
    @Resource
    private SysUserRoleRelationMapper userRoleRelationMapper;
    @Resource
    private SysRoleMenuRelationMapper roleMenuRelationMapper;
    @Resource
    private SysUserTenantRelationMapper userTenantRelationMapper;
    @Resource
    private SysLoginLogMapper loginLogMapper;
    @Resource
    private TenantFacade tenantFacade;
    @Resource
    private TenantMetaMapper tenantMetaMapper;


    @Test
    void passwordLoginBranches() {
        String nano = String.valueOf(System.nanoTime() % 1_000_000_000L);
        String code = "itt" + nano;
        String adminPin = "ita" + nano;
        Long tenantId = tenantService.adminCreate(new AdminTenantCreateRequest()
                .setCode(code).setName("IT租户" + nano)
                .setTenantAdminPin(adminPin).setTenantAdminPwd(PWD)
                .setTenantAdminEmail("it@a.b.c").setTenantAdminPhoneNo("13800001111"));

        boolean tenantFirst = tenantFacade.getLoginMode() == TenantLoginModeEnum.TENANT_FIRST;

        // 成功登录：租户管理员角色与租户上下文
        SysUserLoginResult ok = adminLoginService.passwordLogin(
                new AdminAuthPasswordLoginRequest().setPin(adminPin).setPwd(PWD)
                        .setTenantCode(tenantFirst ? code : null),
                new SimpleVisitorContext().setClientIp("127.0.0.1").setUserAgent("UA"));
        Assertions.assertNotNull(ok.getId());
        Assertions.assertEquals(tenantId, ok.getTenantContext().getTenantId());
        Assertions.assertTrue(ok.getRoleCodes().contains("OrgAdmin"));
        Assertions.assertFalse(ok.getRoleIds().isEmpty());

        // 密码错误：防枚举统一错误码 A01001
        var wrong = Assertions.assertThrows(BusinessException.class,
                () -> adminLoginService.passwordLogin(
                        new AdminAuthPasswordLoginRequest().setPin(adminPin).setPwd("wrong-pass-9")
                                .setTenantCode(tenantFirst ? code : null),
                        new SimpleVisitorContext().setClientIp("127.0.0.1").setUserAgent("UA")));
        Assertions.assertEquals(SysErrorCodeEnum.A01001, wrong.getErrorCode());

        // 账号不存在：同样 A01001
        var unknown = Assertions.assertThrows(BusinessException.class,
                () -> adminLoginService.passwordLogin(
                        new AdminAuthPasswordLoginRequest().setPin("nosuch" + nano).setPwd(PWD)
                                .setTenantCode(tenantFirst ? code : null),
                        new SimpleVisitorContext().setClientIp("127.0.0.1").setUserAgent("UA")));
        Assertions.assertEquals(SysErrorCodeEnum.A01001, unknown.getErrorCode());

        // 成功与失败均已落登录日志
        var page = sysLoginLogService.adminList(
                new AdminSysLoginLogListQuery()
                        .setPageParam(new PageParam(1, 10))
                        .setUserPin(adminPin));
        Assertions.assertTrue(page.getTotal() >= 3, "应有至少 3 条登录日志（成功+两次失败）");

        cleanupTenant(tenantId, code);
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private void cleanupTenant(Long tenantId, String code) {
        TenantContextHolder.runIgnored(() -> {
            // 租户内角色
            List<SysRoleEntity> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                    .eq(SysRoleEntity::getTenantId, tenantId));
            List<Long> roleIds = roles.stream().map(SysRoleEntity::getId).toList();
            if (!roleIds.isEmpty()) {
                roleMenuRelationMapper.deleteByRoleIds(roleIds);
                userRoleRelationMapper.deleteByRoleIds(roleIds);
                sysRoleMapper.deleteByIds(roleIds);
            }
            // 用户与关联
            List<Long> userIds = userTenantRelationMapper.selectList(
                            new LambdaQueryWrapper<SysUserTenantRelationEntity>()
                                    .eq(SysUserTenantRelationEntity::getTenantId, tenantId))
                    .stream().map(SysUserTenantRelationEntity::getUserId).toList();
            if (!userIds.isEmpty()) {
                userTenantRelationMapper.delete(
                        new LambdaQueryWrapper<SysUserTenantRelationEntity>()
                                .eq(SysUserTenantRelationEntity::getTenantId, tenantId));
                sysUserMapper.deleteByIds(userIds);
            }
            tenantMetaMapper.deleteById(tenantId);
        });
    }

    @AfterEach
    void cleanupLogs() {
        TenantContextHolder.runIgnored(() ->
                loginLogMapper.selectList(new LambdaQueryWrapper<SysLoginLogEntity>()
                                .likeRight(SysLoginLogEntity::getUserPin, "ita"))
                        .forEach(l -> loginLogMapper.deleteById(l.getId())));
    }
}
