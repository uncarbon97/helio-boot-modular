package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.facade.AuditNicknameFiller;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.sys.AuditNicknameFillerTestSupport;
import cc.uncarbon.module.sys.MybatisPlusTestSupport;
import cc.uncarbon.module.sys.constant.SysConstant;
import cc.uncarbon.module.sys.dal.entity.SysMenuEntity;
import cc.uncarbon.module.sys.dal.mapper.SysMenuMapper;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.enums.MenuTypeEnum;
import cc.uncarbon.module.sys.enums.MenuVisibleScopeEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.request.AdminSysMenuUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysMenuDTO;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cc.uncarbon.module.sys.support.UserRoleHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * {@link SysMenuServiceImpl} 菜单树/权限解析分支测试
 * （「仅超管可见」子树剔除、祖先禁用级联、成环防护、权限串解析）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysMenuServiceImplTest {

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @Mock
    private SysMenuMapper sysMenuMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysRoleMenuRelationService sysRoleMenuRelationService;
    @Mock
    private UserRoleHelper userRoleHelper;
    @Spy
    private AuditNicknameFiller auditNicknameFiller = AuditNicknameFillerTestSupport.passThrough();

    @InjectMocks
    private SysMenuServiceImpl service;


    /*
    ----------------------------------------------------------------
                        列表/详情可见性
    ----------------------------------------------------------------
     */

    @Test
    void adminListStripsSuperAdminOnlySubtreeForNonSuperAdmin() {
        List<SysMenuEntity> menus = List.of(
                menu(1L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.SUPER_ADMIN_ONLY),
                menu(2L, 1L, MenuTypeEnum.MENU, MenuVisibleScopeEnum.ALL),
                menu(3L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL));
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenReturn(menus);

        withRoles(7L, Set.of(5L), () -> {
            var ret = service.adminList();
            Assertions.assertEquals(Set.of(3L), ids(ret));
        });
    }

    @Test
    void adminListKeepsAllForSuperAdminOrNoUserContext() {
        List<SysMenuEntity> menus = List.of(
                menu(1L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.SUPER_ADMIN_ONLY),
                menu(2L, 1L, MenuTypeEnum.MENU, MenuVisibleScopeEnum.ALL));
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenReturn(menus);

        withRoles(0L, Set.of(0L), () ->
                Assertions.assertEquals(2, service.adminList().size()));

        // 无用户上下文（系统内部调用）不剔除
        Assertions.assertEquals(2, service.adminList().size());
    }

    @Test
    void adminListKeepsAllWhenRoleIdsEmpty() {
        // 有用户上下文但角色为空（未完成授权的会话）→ 不做剔除
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenReturn(List.of(
                menu(1L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.SUPER_ADMIN_ONLY),
                menu(2L, 1L, MenuTypeEnum.MENU, MenuVisibleScopeEnum.ALL)));

        withRoles(7L, Set.of(), () ->
                Assertions.assertEquals(2, service.adminList().size()));
    }

    @Test
    void convertEntityCleansFieldsByMenuType() {
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenReturn(List.of(
                menu(1L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL).setPath("sys"),
                menu(2L, 1L, MenuTypeEnum.MENU, MenuVisibleScopeEnum.ALL)
                        .setPath("/user").setComponent("UserView").setExternalLink("https://a.b"),
                menu(3L, 1L, MenuTypeEnum.EXTERNAL_LINK, MenuVisibleScopeEnum.ALL)
                        .setPath("/link").setComponent("Comp").setExternalLink("https://e.f"),
                menu(4L, 1L, MenuTypeEnum.BUTTON, MenuVisibleScopeEnum.ALL)
                        .setPath("/btn").setComponent("BtnComp").setExternalLink("https://g.h")));

        // 无用户上下文 → 直接转换
        List<SysMenuDTO> ret = service.adminList();

        Assertions.assertEquals(4, ret.size());
        // 根节点 parentId 归一为 null；目录/按钮清空组件与外链
        Assertions.assertNull(ret.get(0).getParentId());
        Assertions.assertNull(ret.get(0).getComponent());
        Assertions.assertNull(ret.get(0).getExternalLink());
        // 菜单保留组件、清空外链
        Assertions.assertEquals("UserView", ret.get(1).getComponent());
        Assertions.assertNull(ret.get(1).getExternalLink());
        // 外链保留链接、清空组件
        Assertions.assertNull(ret.get(2).getComponent());
        Assertions.assertEquals("https://e.f", ret.get(2).getExternalLink());
        // 按钮两者都清空
        Assertions.assertNull(ret.get(3).getComponent());
        Assertions.assertNull(ret.get(3).getExternalLink());
    }

    @Test
    void getByIdProtectedMenuInvisibleForNonSuperAdmin() {
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenReturn(List.of(
                menu(1L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.SUPER_ADMIN_ONLY)));
        Mockito.when(sysMenuMapper.selectById(1L))
                .thenReturn(menu(1L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.SUPER_ADMIN_ONLY));

        withRoles(7L, Set.of(5L), () ->
                Assertions.assertNull(service.getById(1L)));
        withRoles(0L, Set.of(0L), () ->
                Assertions.assertNotNull(service.getById(1L)));
        Assertions.assertNull(service.getById(null));
    }


    /*
    ----------------------------------------------------------------
                        权限串解析
    ----------------------------------------------------------------
     */

    @Test
    void getPermissionsByRoleEmptyInput() {
        Assertions.assertEquals(Map.of(), service.getPermissionsByRole(null));
        Assertions.assertEquals(Map.of(), service.getPermissionsByRole(List.of()));
    }

    @Test
    void getPermissionsByRoleSuperAdminReadsAll() {
        Mockito.when(sysRoleMapper.listEnabledRoleIds(Mockito.anyCollection())).thenReturn(Set.of());
        Mockito.when(sysMenuMapper.selectList(Mockito.isNull())).thenReturn(List.of(
                permMenu(2L, "p2"), permMenu(3L, null), permMenu(4L, "")));

        var ret = service.getPermissionsByRole(List.of(SysConstant.SUPER_ADMIN_ROLE_ID));

        Assertions.assertEquals(Set.of("p2"), ret.get(SysConstant.SUPER_ADMIN_ROLE_ID));
    }

    @Test
    void getPermissionsByRoleDisabledRoleYieldsEmptyAndCached() {
        Mockito.when(sysRoleMapper.listEnabledRoleIds(Mockito.anyCollection())).thenReturn(Set.of());

        var ret = service.getPermissionsByRole(List.of(5L));

        Assertions.assertTrue(ret.containsKey(5L));
        Assertions.assertEquals(Set.of(), ret.get(5L));
    }

    @Test
    void getPermissionsByRoleWithoutMenusYieldsEmpty() {
        Mockito.when(sysRoleMapper.listEnabledRoleIds(Mockito.anyCollection())).thenReturn(Set.of(5L));
        Mockito.when(sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(5L))).thenReturn(Set.of());

        Assertions.assertEquals(Set.of(), service.getPermissionsByRole(List.of(5L)).get(5L));
    }

    @Test
    void getPermissionsByRoleFiltersDisabledAncestor() {
        Mockito.when(sysRoleMapper.listEnabledRoleIds(Mockito.anyCollection())).thenReturn(Set.of(5L));
        Mockito.when(sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(5L))).thenReturn(Set.of(2L));
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenReturn(List.of(
                menu(1L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL).setStatus(EnabledStatusEnum.DISABLED),
                menu(2L, 1L, MenuTypeEnum.BUTTON, MenuVisibleScopeEnum.ALL)));

        Assertions.assertEquals(Set.of(), service.getPermissionsByRole(List.of(5L)).get(5L));
    }

    @Test
    void getPermissionsByRoleFiltersSuperAdminOnlySubtree() {
        Mockito.when(sysRoleMapper.listEnabledRoleIds(Mockito.anyCollection())).thenReturn(Set.of(5L));
        Mockito.when(sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(5L))).thenReturn(Set.of(2L));
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenReturn(List.of(
                menu(1L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.SUPER_ADMIN_ONLY),
                menu(2L, 1L, MenuTypeEnum.BUTTON, MenuVisibleScopeEnum.ALL)));

        Assertions.assertEquals(Set.of(), service.getPermissionsByRole(List.of(5L)).get(5L));
    }

    @Test
    void getPermissionsByRoleHappyPath() {
        Mockito.when(sysRoleMapper.listEnabledRoleIds(Mockito.anyCollection())).thenReturn(Set.of(5L));
        Mockito.when(sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(5L))).thenReturn(Set.of(2L));
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenReturn(List.of(
                menu(1L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL),
                permMenu(2L, "p2")));

        Assertions.assertEquals(Set.of("p2"), service.getPermissionsByRole(List.of(5L)).get(5L));
    }

    @Test
    void getPermissionsByRoleMixedRoles() {
        // 0=超管读取全部；5=已禁用角色缓存空集；6=普通角色正常解析
        Mockito.when(sysRoleMapper.listEnabledRoleIds(Mockito.anyCollection())).thenReturn(Set.of(6L));
        Mockito.when(sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(6L))).thenReturn(Set.of(2L));
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenReturn(List.of(
                menu(1L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL),
                permMenu(2L, "p2")));
        // 注意：any() 也能匹配 null，需放在 isNull() 之前，让超管全量查询命中专属桩
        Mockito.when(sysMenuMapper.selectList(Mockito.isNull())).thenReturn(List.of(permMenu(9L, "pAll")));

        var ret = service.getPermissionsByRole(List.of(
                SysConstant.SUPER_ADMIN_ROLE_ID, 5L, 6L));

        Assertions.assertEquals(Set.of("pAll"), ret.get(SysConstant.SUPER_ADMIN_ROLE_ID));
        Assertions.assertEquals(Set.of(), ret.get(5L));
        Assertions.assertEquals(Set.of("p2"), ret.get(6L));
    }

    @Test
    void getPermissionsByRoleCycleWithoutDisabledPasses() {
        // 1→2→1 成环且全启用 → 视为链完整
        Mockito.when(sysRoleMapper.listEnabledRoleIds(Mockito.anyCollection())).thenReturn(Set.of(5L));
        Mockito.when(sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(5L))).thenReturn(Set.of(1L));
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenReturn(List.of(
                menu(1L, 2L, MenuTypeEnum.BUTTON, MenuVisibleScopeEnum.ALL),
                menu(2L, 1L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL)));

        var ret = service.getPermissionsByRole(List.of(5L));

        // 菜单均无权限串 → 空集合，但成环校验本身不抛错不死循环
        Assertions.assertEquals(Set.of(), ret.get(5L));
    }


    /*
    ----------------------------------------------------------------
                        当前用户可见菜单
    ----------------------------------------------------------------
     */

    @Test
    void visibleMenusNoRolesReturnsEmpty() {
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenReturn(List.of());

        // 无角色不再报错，直接返回空集合
        List<SysMenuDTO> ret = withRolesReturn(7L, Set.of(), service::adminListVisibleMenus);

        Assertions.assertEquals(List.of(), ret);
    }

    @Test
    void visibleMenusTraceParentsAndNullifyPermission() {
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenReturn(List.of(
                menu(1L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL).setPath("sys"),
                permMenu(2L, "p2").setPath("user").setMenuType(MenuTypeEnum.MENU)));
        Mockito.when(sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(5L))).thenReturn(Set.of(2L));

        List<SysMenuDTO> ret =
                withRolesReturn(7L, Set.of(5L), () -> service.adminListSideMenus());

        // 父级目录被补全；侧边菜单不含按钮；权限串全部置空
        Assertions.assertEquals(Set.of(1L, 2L),
                ret.stream().map(SysMenuDTO::getId)
                        .collect(Collectors.toSet()));
        ret.forEach(dto -> Assertions.assertNull(dto.getPermission()));
        // convertEntity 防漏斜杠：sys → /sys
        ret.forEach(dto -> Assertions.assertTrue(dto.getPath().startsWith("/")));
    }

    @Test
    void listSuperAdminOnlySubtreeExpandsBreadthFirst() {
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenReturn(List.of(
                menu(1L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.SUPER_ADMIN_ONLY),
                menu(2L, 1L, MenuTypeEnum.MENU, MenuVisibleScopeEnum.ALL),
                menu(3L, 2L, MenuTypeEnum.BUTTON, MenuVisibleScopeEnum.ALL),
                menu(4L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL)));

        Assertions.assertEquals(Set.of(1L, 2L, 3L), service.listSuperAdminOnlySubtreeMenuIds());
    }

    @Test
    void visibleMenusSuperAdminOnlyEnabledKept() {
        // 第一次 selectList = 全量菜单（含禁用，用于祖先链校验）；后续 = 按计算出的可见Ids过滤后的结果
        var calls = new AtomicInteger();
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenAnswer(inv -> {
            if (calls.incrementAndGet() == 1) {
                return List.of(
                        menu(1L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL),
                        menu(2L, 1L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL).setStatus(EnabledStatusEnum.DISABLED),
                        menu(3L, 0L, MenuTypeEnum.MENU, MenuVisibleScopeEnum.ALL));
            }
            return List.of(
                    menu(1L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL),
                    menu(3L, 0L, MenuTypeEnum.MENU, MenuVisibleScopeEnum.ALL));
        });

        // 超管视角：被禁用的 2 号目录（及其子树位）不可见
        List<SysMenuDTO> ret = withRolesReturn(0L, Set.of(SysConstant.SUPER_ADMIN_ROLE_ID),
                () -> service.adminListVisibleMenus());

        Assertions.assertEquals(Set.of(1L, 3L), ids(ret));
        ret.forEach(dto -> Assertions.assertNull(dto.getPermission()));
    }

    @Test
    void visibleMenusSuperAdminEmptyMapYieldsEmpty() {
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenReturn(List.of());

        List<SysMenuDTO> ret = withRolesReturn(0L, Set.of(SysConstant.SUPER_ADMIN_ROLE_ID),
                () -> service.adminListVisibleMenus());

        Assertions.assertEquals(List.of(), ret);
        Mockito.verify(sysRoleMenuRelationService, Mockito.never()).listMenuIdsByRoles(Mockito.anyCollection());
    }

    @Test
    void visibleMenusNormalUserStripsDisabledAncestorAndProtectedSubtree() {
        // 1 为「仅超管可见」目录，2 为其子（且被禁用）；3 通用目录，5 其子
        var calls = new AtomicInteger();
        Mockito.when(sysMenuMapper.selectList(Mockito.any())).thenAnswer(inv -> {
            if (calls.incrementAndGet() == 1) {
                return List.of(
                        menu(1L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.SUPER_ADMIN_ONLY),
                        menu(2L, 1L, MenuTypeEnum.MENU, MenuVisibleScopeEnum.ALL).setStatus(EnabledStatusEnum.DISABLED),
                        menu(3L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL),
                        menu(5L, 3L, MenuTypeEnum.MENU, MenuVisibleScopeEnum.ALL));
            }
            // SQL 侧按计算出的可见Ids过滤后的返回
            return List.of(
                    menu(3L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL),
                    menu(5L, 3L, MenuTypeEnum.MENU, MenuVisibleScopeEnum.ALL));
        });
        Mockito.when(sysRoleMenuRelationService.listMenuIdsByRoles(Set.of(5L))).thenReturn(Set.of(2L, 5L));

        // 直接关联 2、5 → 补齐祖先 1、3 → 2 自身禁用剔除 → 受保护子树 1（连带2）剔除 → 只剩 3、5
        List<SysMenuDTO> ret = withRolesReturn(7L, Set.of(5L), () -> service.adminListSideMenus());

        Assertions.assertEquals(Set.of(3L, 5L), ids(ret));
        ret.forEach(dto -> Assertions.assertNull(dto.getPermission()));
    }


    /*
    ----------------------------------------------------------------
                        增删改
    ----------------------------------------------------------------
     */

    @Test
    void adminCreateDuplicatePermissionRejected() {
        Mockito.when(sysMenuMapper.selectOne(Mockito.any())).thenReturn(
                new SysMenuEntity().setId(1L));

        var ex = Assertions.assertThrows(HasRepeatRecordException.class,
                () -> service.adminCreate(upsert(null, 0L, "p1")));
        Assertions.assertEquals(SysErrorCodeEnum.A01032, ex.getErrorCode());
    }

    @Test
    void adminCreateCleansBlankPermission() {
        Mockito.when(sysMenuMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(sysMenuMapper.insert(Mockito.any(SysMenuEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysMenuEntity.class).setId(8L);
            return 1;
        });

        // 权限标识含空白字符 → 校验前先清洗
        Long id = service.adminCreate(upsert(null, 0L, " p1 "));

        Assertions.assertEquals(8L, id);
        ArgumentCaptor<SysMenuEntity> captor = ArgumentCaptor.forClass(SysMenuEntity.class);
        Mockito.verify(sysMenuMapper).insert(captor.capture());
        Assertions.assertEquals("p1", captor.getValue().getPermission());
    }

    @Test
    void adminCreateMissingParentRejected() {
        Mockito.when(sysMenuMapper.selectById(9L)).thenReturn(null);

        Assertions.assertThrows(NoRecordException.class,
                () -> service.adminCreate(upsert(null, 9L, null)));
    }

    @Test
    void adminCreateDisabledParentRejected() {
        Mockito.when(sysMenuMapper.selectById(9L)).thenReturn(
                menu(9L, 0L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL).setStatus(EnabledStatusEnum.DISABLED));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminCreate(upsert(null, 9L, null)));
        Assertions.assertEquals(SysErrorCodeEnum.A01044, ex.getErrorCode());
    }

    @Test
    void adminCreateDefaultsParentIdAndVisibleScope() {
        Mockito.when(sysMenuMapper.insert(Mockito.any(SysMenuEntity.class))).thenAnswer(inv -> {
            inv.getArgument(0, SysMenuEntity.class).setId(7L);
            return 1;
        });

        Long id = service.adminCreate(upsert(null, null, null));

        Assertions.assertEquals(7L, id);
        ArgumentCaptor<SysMenuEntity> captor = ArgumentCaptor.forClass(SysMenuEntity.class);
        Mockito.verify(sysMenuMapper).insert(captor.capture());
        Assertions.assertEquals(SysConstant.ROOT_PARENT_ID, captor.getValue().getParentId());
        // 缺省可见范围在请求侧补为通用，并随 BeanUtil 拷贝进实体
        Assertions.assertEquals(MenuVisibleScopeEnum.ALL, captor.getValue().getVisibleScope());
    }

    @Test
    void adminUpdateParentCannotBeSelfOrInferior() {
        Mockito.when(sysMenuMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysMenuMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(sysMenuMapper.selectById(3L)).thenReturn(menu(3L, 1L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminUpdate(upsert(1L, 3L, null)));
        Assertions.assertEquals(SysErrorCodeEnum.A01041, ex.getErrorCode());
    }

    @Test
    void adminUpdateMissingRejected() {
        Mockito.when(sysMenuMapper.exists(Mockito.any())).thenReturn(false);

        Assertions.assertThrows(NoRecordException.class,
                () -> service.adminUpdate(upsert(9L, 0L, null)));
    }

    @Test
    void adminUpdateDuplicatePermissionRejected() {
        Mockito.when(sysMenuMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysMenuMapper.selectOne(Mockito.any())).thenReturn(new SysMenuEntity().setId(2L));

        var ex = Assertions.assertThrows(HasRepeatRecordException.class,
                () -> service.adminUpdate(upsert(1L, 0L, "p1")));
        Assertions.assertEquals(SysErrorCodeEnum.A01032, ex.getErrorCode());
    }

    @Test
    void adminUpdateHappyPathDefaultsRootParent() {
        Mockito.when(sysMenuMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysMenuMapper.selectOne(Mockito.any())).thenReturn(null);

        // parentId 为空 → 归一为根节点，上级/成环校验直接放行
        service.adminUpdate(upsert(1L, null, null));

        ArgumentCaptor<SysMenuEntity> captor = ArgumentCaptor.forClass(SysMenuEntity.class);
        Mockito.verify(sysMenuMapper).updateById(captor.capture());
        Assertions.assertEquals(1L, captor.getValue().getId());
        Assertions.assertEquals(SysConstant.ROOT_PARENT_ID, captor.getValue().getParentId());
    }

    @Test
    void adminUpdateParentChainBreaksOnMissingNode() {
        Mockito.when(sysMenuMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysMenuMapper.selectOne(Mockito.any())).thenReturn(null);
        // 3 → 9 → (9 不存在) → 链自然中断，不视为成环
        Mockito.when(sysMenuMapper.selectById(3L)).thenReturn(menu(3L, 9L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL));
        Mockito.when(sysMenuMapper.selectById(9L)).thenReturn(null);

        service.adminUpdate(upsert(1L, 3L, null));

        Mockito.verify(sysMenuMapper).updateById(Mockito.any(SysMenuEntity.class));
    }

    @Test
    void adminUpdateParentCycleGuardRejected() {
        Mockito.when(sysMenuMapper.exists(Mockito.any())).thenReturn(true);
        Mockito.when(sysMenuMapper.selectOne(Mockito.any())).thenReturn(null);
        // 2 ↔ 3 互为父子的脏数据：上溯永不命中自身 → 保护圈数兜底拒绝
        var counter = new AtomicInteger();
        Mockito.when(sysMenuMapper.selectById(Mockito.anyLong())).thenAnswer(inv -> counter.getAndIncrement() % 2 == 0
                ? menu(2L, 3L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL)
                : menu(3L, 2L, MenuTypeEnum.DIR, MenuVisibleScopeEnum.ALL));

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminUpdate(upsert(1L, 2L, null)));
        Assertions.assertEquals(SysErrorCodeEnum.A01041, ex.getErrorCode());
    }

    @Test
    void adminSetStatusMissingRejected() {
        Mockito.when(sysMenuMapper.selectById(9L)).thenReturn(null);

        Assertions.assertThrows(NoRecordException.class,
                () -> service.adminSetStatus(new AdminSetStatusRequest<Long, EnabledStatusEnum>()
                        .setId(9L).setNewStatus(EnabledStatusEnum.DISABLED)));
    }

    @Test
    void getNonnullByIdMissingRejected() {
        Mockito.when(sysMenuMapper.selectById(9L)).thenReturn(null);

        Assertions.assertThrows(NoRecordException.class, () -> service.getNonnullById(9L));
    }

    @Test
    void adminDeleteWithChildrenRejected() {
        Mockito.when(sysMenuMapper.exists(Mockito.any())).thenReturn(true);

        var ex = Assertions.assertThrows(BusinessException.class,
                () -> service.adminDelete(List.of(1L)));
        Assertions.assertEquals(SysErrorCodeEnum.A01042, ex.getErrorCode());
    }

    @Test
    void adminDeleteCleansRoleRelations() {
        Mockito.when(sysMenuMapper.exists(Mockito.any())).thenReturn(false);

        service.adminDelete(List.of(7L));

        Mockito.verify(sysRoleMenuRelationService).deleteByMenuIds(List.of(7L));
        Mockito.verify(sysMenuMapper).deleteByIds(List.of(7L));
    }

    @Test
    void adminSetStatusOnlyUpdatesTemplate() {
        Mockito.when(sysMenuMapper.selectById(7L))
                .thenReturn(menu(7L, 0L, MenuTypeEnum.MENU, MenuVisibleScopeEnum.ALL));

        service.adminSetStatus(new AdminSetStatusRequest<Long, EnabledStatusEnum>()
                .setId(7L).setNewStatus(EnabledStatusEnum.DISABLED));

        ArgumentCaptor<SysMenuEntity> captor = ArgumentCaptor.forClass(SysMenuEntity.class);
        Mockito.verify(sysMenuMapper).updateById(captor.capture());
        Assertions.assertEquals(EnabledStatusEnum.DISABLED, captor.getValue().getStatus());
        Assertions.assertEquals(7L, captor.getValue().getId());
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    private static void withRoles(Long userId, Set<Long> roleIds, Runnable op) {
        ScopedValue.where(UserContextHolder.scoped(),
                        new SimpleUserContext().setUserId(userId).setRoleIds(roleIds))
                .run(op);
    }

    private static <T> T withRolesReturn(Long userId, Set<Long> roleIds,
                                         Supplier<T> op) {
        var ref = new Object() {
            T value;
        };
        withRoles(userId, roleIds, () -> ref.value = op.get());
        return ref.value;
    }

    private static Set<Long> ids(List<SysMenuDTO> list) {
        return list.stream().map(SysMenuDTO::getId)
                .collect(Collectors.toSet());
    }

    private AdminSysMenuUpsertRequest upsert(Long id, Long parentId, String permission) {
        return new AdminSysMenuUpsertRequest()
                .setId(id).setName("菜单").setParentId(parentId).setMenuType(MenuTypeEnum.BUTTON)
                .setStatus(EnabledStatusEnum.ENABLED).setPermission(permission).setPath("/x").setSort(1);
    }

    private SysMenuEntity menu(Long id, Long parentId, MenuTypeEnum type, MenuVisibleScopeEnum scope) {
        return new SysMenuEntity().setId(id).setParentId(parentId).setMenuType(type)
                .setVisibleScope(scope).setStatus(EnabledStatusEnum.ENABLED);
    }

    private SysMenuEntity permMenu(Long id, String permission) {
        return menu(id, 0L, MenuTypeEnum.BUTTON, MenuVisibleScopeEnum.ALL).setPermission(permission);
    }
}
