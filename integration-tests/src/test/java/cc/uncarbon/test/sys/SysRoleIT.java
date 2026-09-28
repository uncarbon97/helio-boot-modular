package cc.uncarbon.test.sys;

import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysMenuEntity;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.enums.MenuTypeEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.model.request.AdminSysRoleBindMenuRequest;
import cc.uncarbon.module.sys.model.request.AdminSysRoleUpsertRequest;
import cc.uncarbon.module.sys.model.request.AdminSysMenuUpsertRequest;
import cc.uncarbon.module.sys.service.SysMenuService;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cc.uncarbon.module.sys.service.SysRoleService;
import cc.uncarbon.test.base.BaseIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.dal.mapper.SysMenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 角色管理集成测试：CRUD、保留编码/同名拦截、绑定菜单、删除级联清理
 */
@Tag("sys")
class SysRoleIT extends BaseIntegrationTest {

    @Resource
    private SysRoleService sysRoleService;
    @Resource
    private SysRoleMenuRelationService sysRoleMenuRelationService;
    @Resource
    private SysMenuService sysMenuService;

    @Resource
    private SysRoleMapper sysRoleMapper;
    @Resource
    private SysMenuMapper sysMenuMapper;


    @Test
    void roleCrudLifecycle() {
        String code = "itr" + (System.nanoTime() % 1_000_000_000L);
        Long menuId = createMenu("it:perm:" + code);

        Long roleId = create(code);
        Assertions.assertNotNull(roleId);

        // 重复编码 → A01031
        var dup = Assertions.assertThrows(HasRepeatRecordException.class, () -> create(code));
        Assertions.assertEquals(SysErrorCodeEnum.A01031, ((HasRepeatRecordException) dup).getErrorCode());

        // 保留编码 → A01010
        var reserved = Assertions.assertThrows(BusinessException.class,
                () -> create(SysConstant.SUPER_ADMIN_ROLE_CODE));
        Assertions.assertEquals(SysErrorCodeEnum.A01010, ((BusinessException) reserved).getErrorCode());

        // 绑定菜单
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> sysRoleService.adminBindMenu(
                        new AdminSysRoleBindMenuRequest().setRoleId(roleId).setMenuIds(Set.of(menuId))));
        Assertions.assertEquals(Set.of(menuId),
                sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(roleId)));

        // 删除角色 → 关联关系清理
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> sysRoleService.adminDelete(List.of(roleId)));
        Assertions.assertEquals(Set.of(),
                sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(roleId)));
        Assertions.assertNull(sysRoleService.getById(roleId));
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private Long create(String code) {
        var ref = new Object() {
            Long id;
        };
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> ref.id = sysRoleService.adminCreate(
                        new AdminSysRoleUpsertRequest().setCode(code).setName("IT角色").setDescription("d")));
        return ref.id;
    }

    private Long createMenu(String permission) {
        var ref = new Object() {
            Long id;
        };
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> ref.id = sysMenuService.adminCreate(
                        new AdminSysMenuUpsertRequest()
                                .setName("IT按钮").setMenuType(MenuTypeEnum.BUTTON)
                                .setStatus(EnabledStatusEnum.ENABLED)
                                .setPermission(permission).setSort(99)));
        return ref.id;
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.runIgnored(() -> {
            sysRoleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                            .likeRight(SysRoleEntity::getCode, "itr"))
                    .forEach(r -> sysRoleMapper.deleteById(r.getId()));
            sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenuEntity>()
                            .likeRight(SysMenuEntity::getPermission, "it:perm:"))
                    .forEach(m -> sysMenuMapper.deleteById(m.getId()));
        });
    }
}
