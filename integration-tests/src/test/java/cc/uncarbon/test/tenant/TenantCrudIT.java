package cc.uncarbon.test.tenant;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.sys.dal.entity.SysMenuEntity;
import cc.uncarbon.module.sys.enums.MenuTypeEnum;
import cc.uncarbon.module.tenant.errorcode.TenantErrorCodeEnum;
import cc.uncarbon.module.tenant.model.request.AdminTenantCreateRequest;
import cc.uncarbon.module.tenant.model.request.AdminTenantMetaUpdateRequest;
import cc.uncarbon.module.tenant.model.request.AdminTenantPackageBindMenuRequest;
import cc.uncarbon.module.tenant.model.request.AdminTenantPackageUpsertRequest;
import cc.uncarbon.module.tenant.service.TenantPackageService;
import cc.uncarbon.module.tenant.service.TenantService;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cc.uncarbon.test.base.BaseIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import cc.uncarbon.module.tenant.dal.mapper.TenantMetaMapper;
import cc.uncarbon.module.tenant.dal.mapper.TenantPackageMapper;
import cc.uncarbon.module.sys.dal.mapper.SysMenuMapper;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserRoleRelationMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserTenantRelationMapper;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMenuRelationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserTenantRelationEntity;
import cc.uncarbon.module.tenant.dal.entity.TenantMetaEntity;
import cc.uncarbon.module.tenant.dal.entity.TenantPackageEntity;

/**
 * 租户生命周期集成测试：建租户（角色+管理员+套餐级联）、
 * 套餐变更级联裁剪角色菜单、禁用/删除保护
 */
@Tag("tenant")
class TenantCrudIT extends BaseIntegrationTest {

    @Resource
    private TenantService tenantService;
    @Resource
    private TenantPackageService tenantPackageService;
    @Resource
    private SysRoleMenuRelationService sysRoleMenuRelationService;

    @Resource
    private TenantMetaMapper tenantMetaMapper;
    @Resource
    private TenantPackageMapper tenantPackageMapper;
    @Resource
    private SysMenuMapper sysMenuMapper;
    @Resource
    private SysRoleMapper sysRoleMapper;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysUserRoleRelationMapper userRoleRelationMapper;
    @Resource
    private SysUserTenantRelationMapper userTenantRelationMapper;
    @Resource
    private SysRoleMenuRelationMapper roleMenuRelationMapper;


    @Test
    void tenantLifecycleWithPackageCascade() throws Exception {
        long nano = System.nanoTime() % 1_000_000_000L;
        Long m1 = createMenu("it:perm:tc1" + nano);
        Long m2 = createMenu("it:perm:tc2" + nano);

        // 套餐：新建默认禁用 → 启用 → 绑定菜单
        Long pkgId = tenantPackageService.adminCreate(new AdminTenantPackageUpsertRequest()
                .setCode("itp" + nano).setName("IT套餐").setDescription("d"));
        Assertions.assertEquals(EnabledStatusEnum.DISABLED,
                tenantPackageService.getById(pkgId).getStatus());
        tenantPackageService.adminSetStatus(new AdminSetStatusRequest<Long, EnabledStatusEnum>()
                .setId(pkgId).setNewStatus(EnabledStatusEnum.ENABLED));
        tenantPackageService.adminBindMenu(new AdminTenantPackageBindMenuRequest()
                .setId(pkgId).setMenuIds(Set.of(m1, m2)));
        Assertions.assertEquals(Set.of(m1, m2),
                Set.copyOf(tenantPackageService.getById(pkgId).getMenuIds()));

        // 建租户：自动创建管理员角色/用户并按套餐绑定菜单
        String code = "ittc" + nano;
        Long tenantId = tenantService.adminCreate(new AdminTenantCreateRequest()
                .setCode(code).setName("IT级联租户")
                .setTenantAdminPin("ittca" + nano).setTenantAdminPwd("it-pass-123456")
                .setTenantAdminEmail("it@a.b.c").setTenantAdminPhoneNo("13800001111")
                .setPackageId(pkgId));
        Long adminRoleId = tenantAdminRoleId(tenantId);
        Assertions.assertNotNull(adminRoleId);
        Assertions.assertEquals(Set.of(m1, m2),
                sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(adminRoleId)));

        // 套餐改空 → 租户内角色菜单级联清空
        tenantService.adminUpdate(new AdminTenantMetaUpdateRequest()
                .setId(tenantId).setName("IT级联租户改名").setPackageId(null));
        Assertions.assertEquals(Set.of(),
                sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(adminRoleId)));

        // 平台自营域保护
        var platform = Assertions.assertThrows(BusinessException.class,
                () -> tenantService.adminDelete(List.of(0L)));
        Assertions.assertEquals(TenantErrorCodeEnum.A03013, ((BusinessException) platform).getErrorCode());

        // 仍有用户不能删
        var inUse = Assertions.assertThrows(BusinessException.class,
                () -> tenantService.adminDelete(List.of(tenantId)));
        Assertions.assertEquals(TenantErrorCodeEnum.A03007, ((BusinessException) inUse).getErrorCode());

        // 禁用 → 返回租户用户ID（供强制登出）
        var meta = tenantService.getById(tenantId, true);
        List<Long> kicked = tenantService.adminSetStatus(new AdminSetStatusRequest<Long, EnabledStatusEnum>()
                .setId(tenantId).setNewStatus(EnabledStatusEnum.DISABLED));
        Assertions.assertNotNull(meta.getAdminUserId());
        Assertions.assertTrue(kicked.contains(meta.getAdminUserId()));

        // 清空用户后可删
        cleanupTenantUsers(tenantId);
        tenantService.adminDelete(List.of(tenantId));
        Assertions.assertNull(tenantService.getById(tenantId, false));

        // 套餐去引用后可删
        tenantPackageService.adminDelete(List.of(pkgId));
        Assertions.assertNull(tenantPackageService.getById(pkgId));
    }

    @Test
    void duplicateTenantCodeRejected() {
        long nano = System.nanoTime() % 1_000_000_000L;
        String code = "itdup" + nano;
        tenantService.adminCreate(new AdminTenantCreateRequest()
                .setCode(code).setName("IT重复租户")
                .setTenantAdminPin("itdupa" + nano).setTenantAdminPwd("it-pass-123456")
                .setTenantAdminEmail("it@a.b.c").setTenantAdminPhoneNo("13800001111"));

        var dup = Assertions.assertThrows(HasRepeatRecordException.class,
                () -> tenantService.adminCreate(new AdminTenantCreateRequest()
                        .setCode(code).setName("IT重复租户2")
                        .setTenantAdminPin("itdupb" + nano).setTenantAdminPwd("it-pass-123456")
                        .setTenantAdminEmail("it@a.b.c").setTenantAdminPhoneNo("13800001111")));
        Assertions.assertEquals(TenantErrorCodeEnum.A03005, ((HasRepeatRecordException) dup).getErrorCode());
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private Long createMenu(String permission) throws Exception {
        return TenantContextHolder.callIgnored(() -> {
            var entity = new SysMenuEntity()
                    .setName("IT套餐菜单").setParentId(0L).setMenuType(MenuTypeEnum.BUTTON)
                    .setStatus(EnabledStatusEnum.ENABLED).setPermission(permission).setSort(99);
            sysMenuMapper.insert(entity);
            return entity.getId();
        });
    }

    private Long tenantAdminRoleId(Long tenantId) throws Exception {
        var ref = new Object() {
            Long id;
        };
        TenantContextHolder.callWithContext(testTenant(tenantId, null, null), () -> {
            var role = sysRoleMapper.selectOne(
                    new LambdaQueryWrapper<SysRoleEntity>()
                            .eq(SysRoleEntity::getCode, "OrgAdmin"));
            ref.id = role == null ? null : role.getId();
            return null;
        });
        return ref.id;
    }

    private void cleanupTenantUsers(Long tenantId) {
        TenantContextHolder.runIgnored(() -> {
            List<Long> userIds = userTenantRelationMapper.selectList(
                            new LambdaQueryWrapper<SysUserTenantRelationEntity>()
                                    .eq(SysUserTenantRelationEntity::getTenantId, tenantId))
                    .stream().map(SysUserTenantRelationEntity::getUserId).toList();
            if (!userIds.isEmpty()) {
                userRoleRelationMapper.deleteByRoleIds(
                        sysRoleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                                        .eq(SysRoleEntity::getTenantId, tenantId))
                                .stream().map(SysRoleEntity::getId).toList());
                userTenantRelationMapper.delete(
                        new LambdaQueryWrapper<SysUserTenantRelationEntity>()
                                .eq(SysUserTenantRelationEntity::getTenantId, tenantId));
                sysUserMapper.deleteByIds(userIds);
            }
            roleMenuRelationMapper.deleteByRoleIds(
                    sysRoleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                                    .eq(SysRoleEntity::getTenantId, tenantId))
                            .stream().map(SysRoleEntity::getId).toList());
            sysRoleMapper.delete(new LambdaQueryWrapper<SysRoleEntity>()
                    .eq(SysRoleEntity::getTenantId, tenantId));
        });
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.runIgnored(() -> {
            // 残留租户
            tenantMetaMapper.selectList(new LambdaQueryWrapper<TenantMetaEntity>()
                            .likeRight(TenantMetaEntity::getCode, "it"))
                    .forEach(t -> {
                        cleanupTenantUsers(t.getId());
                        tenantMetaMapper.deleteById(t.getId());
                    });
            // 套餐与菜单
            tenantPackageMapper.selectList(new LambdaQueryWrapper<TenantPackageEntity>()
                            .likeRight(TenantPackageEntity::getCode, "itp"))
                    .forEach(p -> tenantPackageMapper.deleteById(p.getId()));
            sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenuEntity>()
                            .likeRight(SysMenuEntity::getPermission, "it:perm:"))
                    .forEach(m -> sysMenuMapper.deleteById(m.getId()));
        });
    }
}
