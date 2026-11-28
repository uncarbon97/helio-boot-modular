package cc.uncarbon.module.sys.util;

import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.module.sys.constant.SysConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link SysUtil} 单元测试
 */
class SysUtilTest {

    // ---------- isSuperAdmin(Long userId) ----------

    @Test
    void nullUserIdIsNotSuperAdmin() {
        Assertions.assertFalse(SysUtil.isSuperAdmin((Long) null));
    }

    @Test
    void zeroUserIdIsSuperAdmin() {
        Assertions.assertTrue(SysUtil.isSuperAdmin(SysConstant.SUPER_ADMIN_USER_ID));
    }

    @Test
    void otherUserIdIsNotSuperAdmin() {
        Assertions.assertFalse(SysUtil.isSuperAdmin(1L));
        Assertions.assertFalse(SysUtil.isSuperAdmin(-1L));
    }

    // ---------- isSuperAdmin(UserContext) ----------

    @Test
    void nullUserContextIsNotSuperAdmin() {
        Assertions.assertFalse(SysUtil.isSuperAdmin((UserContext) null));
    }

    @Test
    void userContextWithNullRoleCodesIsNotSuperAdmin() {
        UserContext context = new SimpleUserContext();
        Assertions.assertNull(context.getRoleCodes());
        Assertions.assertFalse(SysUtil.isSuperAdmin(context));
    }

    @Test
    void userContextWithEmptyRoleCodesIsNotSuperAdmin() {
        UserContext context = new SimpleUserContext().setRoleCodes(Collections.emptyList());
        Assertions.assertFalse(SysUtil.isSuperAdmin(context));
    }

    @Test
    void userContextWithSuperAdminRoleCodeIsSuperAdmin() {
        UserContext context = new SimpleUserContext()
                .setRoleCodes(List.of("SomeRole", SysConstant.SUPER_ADMIN_ROLE_CODE));
        Assertions.assertTrue(SysUtil.isSuperAdmin(context));
    }

    @Test
    void userContextWithTenantAdminRoleCodeOnlyIsNotSuperAdmin() {
        UserContext context = new SimpleUserContext()
                .setRoleCodes(List.of(SysConstant.TENANT_ADMIN_ROLE_CODE));
        Assertions.assertFalse(SysUtil.isSuperAdmin(context));
    }

    // ---------- isTenantAdmin(UserContext) ----------

    @Test
    void nullUserContextIsNotTenantAdmin() {
        Assertions.assertFalse(SysUtil.isTenantAdmin(null));
    }

    @Test
    void userContextWithNullRoleCodesIsNotTenantAdmin() {
        UserContext context = new SimpleUserContext();
        Assertions.assertFalse(SysUtil.isTenantAdmin(context));
    }

    @Test
    void userContextWithEmptyRoleCodesIsNotTenantAdmin() {
        UserContext context = new SimpleUserContext().setRoleCodes(Collections.emptySet());
        Assertions.assertFalse(SysUtil.isTenantAdmin(context));
    }

    @Test
    void userContextWithTenantAdminRoleCodeIsTenantAdmin() {
        UserContext context = new SimpleUserContext()
                .setRoleCodes(Arrays.asList("SomeRole", SysConstant.TENANT_ADMIN_ROLE_CODE));
        Assertions.assertTrue(SysUtil.isTenantAdmin(context));
    }

    @Test
    void userContextWithSuperAdminRoleCodeOnlyIsNotTenantAdmin() {
        UserContext context = new SimpleUserContext()
                .setRoleCodes(List.of(SysConstant.SUPER_ADMIN_ROLE_CODE));
        Assertions.assertFalse(SysUtil.isTenantAdmin(context));
    }

    // ---------- 组合场景 ----------

    @Test
    void userContextWithBothAdminRoleCodesIsBothAdmin() {
        UserContext context = new SimpleUserContext()
                .setRoleCodes(List.of(SysConstant.SUPER_ADMIN_ROLE_CODE, SysConstant.TENANT_ADMIN_ROLE_CODE));
        Assertions.assertTrue(SysUtil.isSuperAdmin(context));
        Assertions.assertTrue(SysUtil.isTenantAdmin(context));
    }

    @Test
    void regularUserContextIsNeitherAdmin() {
        UserContext context = new SimpleUserContext()
                .setUserId(1L)
                .setRoleCodes(List.of("SomeRole", "AnotherRole"));
        Assertions.assertFalse(SysUtil.isSuperAdmin(context));
        Assertions.assertFalse(SysUtil.isTenantAdmin(context));
    }
}
