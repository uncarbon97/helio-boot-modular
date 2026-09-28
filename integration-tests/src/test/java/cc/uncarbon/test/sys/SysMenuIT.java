package cc.uncarbon.test.sys;

import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.sys.dal.entity.SysMenuEntity;
import cc.uncarbon.module.sys.enums.MenuTypeEnum;
import cc.uncarbon.module.sys.enums.MenuVisibleScopeEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.request.AdminSysMenuUpsertRequest;
import cc.uncarbon.module.sys.service.SysMenuService;
import cc.uncarbon.test.base.BaseIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import cc.uncarbon.module.sys.dal.mapper.SysMenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 菜单管理集成测试：CRUD、权限标识重复拦截、成环拦截、删除约束、
 * 「仅超管可见」子树对非超管隐藏、超管角色权限全量
 */
@Tag("sys")
class SysMenuIT extends BaseIntegrationTest {

    @Resource
    private SysMenuService sysMenuService;

    @Resource
    private SysMenuMapper sysMenuMapper;


    @Test
    void menuCrudLifecycle() {
        String perm = "it:perm:" + (System.nanoTime() % 1_000_000_000L);

        Long menuId = create("IT按钮", null, perm, MenuVisibleScopeEnum.ALL);
        Assertions.assertNotNull(menuId);

        // 权限标识重复 → A01032
        var dup = Assertions.assertThrows(HasRepeatRecordException.class,
                () -> create("IT按钮2", null, perm, MenuVisibleScopeEnum.ALL));
        Assertions.assertEquals(SysErrorCodeEnum.A01032, ((HasRepeatRecordException) dup).getErrorCode());

        // 上级不存在 → NoRecord
        Assertions.assertThrows(NoRecordException.class,
                () -> create("IT孤儿", 99_999_999L, perm + "x", MenuVisibleScopeEnum.ALL));

        // 上级为自身 → A01041
        var cycle = Assertions.assertThrows(BusinessException.class,
                () -> withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                        () -> sysMenuService.adminUpdate(new AdminSysMenuUpsertRequest()
                                .setId(menuId).setName("IT按钮").setMenuType(MenuTypeEnum.BUTTON)
                                .setStatus(EnabledStatusEnum.ENABLED).setPermission(perm)
                                .setParentId(menuId))));
        Assertions.assertEquals(SysErrorCodeEnum.A01041, ((BusinessException) cycle).getErrorCode());

        // 有下级不能删 → A01042
        Long childId = create("IT子按钮", menuId, perm + "c", MenuVisibleScopeEnum.ALL);
        var delParent = Assertions.assertThrows(BusinessException.class,
                () -> withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                        () -> sysMenuService.adminDelete(List.of(menuId))));
        Assertions.assertEquals(SysErrorCodeEnum.A01042, ((BusinessException) delParent).getErrorCode());

        // 叶子可删
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> sysMenuService.adminDelete(List.of(childId)));
        Assertions.assertNull(sysMenuService.getById(childId));
    }

    @Test
    void superAdminOnlySubtreeHiddenForNonSuperAdmin() {
        long nano = System.nanoTime() % 1_000_000_000L;
        Long rootId = create("IT受保护目录" + nano, null, null, MenuVisibleScopeEnum.SUPER_ADMIN_ONLY);
        Long childId = create("IT受保护子菜单" + nano, rootId, null, MenuVisibleScopeEnum.ALL);

        // 非超管视角：列表与详情均不可见
        withContext(new SimpleUserContext().setUserId(9L).setRoleIds(Set.of(999L)),
                testTenant(0L, "平台", "platform"), () -> {
                    Assertions.assertTrue(sysMenuService.adminList().stream()
                            .noneMatch(m -> rootId.equals(m.getId()) || childId.equals(m.getId())));
                    Assertions.assertNull(sysMenuService.getById(rootId));
                    Assertions.assertTrue(sysMenuService.listSuperAdminOnlySubtreeMenuIds().contains(rootId));
                });

        // 超管视角：可见
        withContext(new SimpleUserContext().setUserId(0L).setRoleIds(Set.of(0L)),
                testTenant(0L, "平台", "platform"),
                () -> Assertions.assertNotNull(sysMenuService.getById(rootId)));
    }

    @Test
    void superAdminRoleReadsAllPermissions() {
        String perm = "it:perm:super" + (System.nanoTime() % 1_000_000_000L);
        Long menuId = create("IT超管权限按钮", null, perm, MenuVisibleScopeEnum.ALL);

        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> Assertions.assertTrue(
                        sysMenuService.getPermissionsByRole(List.of(0L)).get(0L).contains(perm)));
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private Long create(String name, Long parentId, String permission, MenuVisibleScopeEnum scope) {
        var ref = new Object() {
            Long id;
        };
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> ref.id = sysMenuService.adminCreate(new AdminSysMenuUpsertRequest()
                        .setName(name).setParentId(parentId).setMenuType(MenuTypeEnum.BUTTON)
                        .setStatus(EnabledStatusEnum.ENABLED).setPermission(permission)
                        .setVisibleScope(scope).setSort(99)));
        return ref.id;
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.runIgnored(() ->
                sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenuEntity>()
                                .likeRight(SysMenuEntity::getName, "IT"))
                        .forEach(m -> sysMenuMapper.deleteById(m.getId())));
    }
}
