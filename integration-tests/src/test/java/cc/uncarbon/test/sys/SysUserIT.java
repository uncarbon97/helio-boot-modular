package cc.uncarbon.test.sys;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.query.AdminSysUserListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.AdminSysUserCreateRequest;
import cc.uncarbon.module.sys.model.request.AdminSysRoleUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysUserDTO;
import cc.uncarbon.module.sys.service.SysRoleService;
import cc.uncarbon.module.sys.service.SysUserService;
import cc.uncarbon.test.base.BaseIntegrationTest;
import cc.uncarbon.framework.helium.base.page.PageParam;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;

/**
 * 用户管理集成测试：CRUD、同名拦截、越权拦截、绑定角色、删除级联清理
 * 超管（用户 0）在平台自营域视角执行
 */
@Tag("sys")
class SysUserIT extends BaseIntegrationTest {

    @Resource
    private SysUserService sysUserService;
    @Resource
    private SysRoleService sysRoleService;

    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysRoleMapper sysRoleMapper;


    @Test
    void userCrudLifecycle() {
        String pin = "itu" + (System.nanoTime() % 1_000_000_000L);
        Long roleId = createRole("itr" + pin);

        // 新增默认禁用
        Long userId = create(pin, "13800001111");
        SysUserDTO dto = sysUserService.getOperableById(userId);
        Assertions.assertNotNull(dto);
        Assertions.assertEquals(SysUserStatusEnum.DISABLED, dto.getStatus());

        // 同 pin 重复 → A01030
        var dup = Assertions.assertThrows(HasRepeatRecordException.class, () -> create(pin, "13800002222"));
        Assertions.assertEquals(SysErrorCodeEnum.A01030, ((HasRepeatRecordException) dup).getErrorCode());

        // 启用
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> sysUserService.adminSetStatus(new AdminSetStatusRequest<Long, SysUserStatusEnum>()
                        .setId(userId).setNewStatus(SysUserStatusEnum.ENABLED)));
        Assertions.assertEquals(SysUserStatusEnum.ENABLED,
                sysUserService.getOperableById(userId).getStatus());

        // 超管给自己去掉超管角色 → A01020
        var self = Assertions.assertThrows(BusinessException.class,
                () -> withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                        () -> sysUserService.adminBindRole(
                                new AdminSysUserBindRoleRequest().setUserId(0L).setRoleIds(Set.of(roleId)))));
        Assertions.assertEquals(SysErrorCodeEnum.A01020, ((BusinessException) self).getErrorCode());

        // 普通用户绑定角色成功，返回最新角色名
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () -> {
            var ret = sysUserService.adminBindRole(
                    new AdminSysUserBindRoleRequest().setUserId(userId).setRoleIds(Set.of(roleId)));
            Assertions.assertFalse(ret.getRoleNames().isEmpty());
        });

        // 手机号筛选
        var page = sysUserService.adminList(new AdminSysUserListQuery()
                .setPageParam(new PageParam(1, 10)).setPhoneNo("13800001111"));
        Assertions.assertTrue(page.getRecords().stream().anyMatch(u -> userId.equals(u.getId())));

        // 删除 → 关联一并清理
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> sysUserService.adminDelete(List.of(userId)));
        Assertions.assertNull(sysUserService.getOperableById(userId));
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private Long create(String pin, String phoneNo) {
        var ref = new Object() {
            Long id;
        };
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () -> {
            // 父类链式 setter 返回父类型，子类字段单独赋值
            var request = new AdminSysUserCreateRequest();
            request.setPin(pin);
            request.setNickname("IT用户");
            request.setEmail("it@a.b.c");
            request.setPhoneNo(phoneNo);
            request.setInitPwd("it-pass-123");
            request.setDeptId(null);
            ref.id = sysUserService.adminCreate(request);
        });
        return ref.id;
    }

    private Long createRole(String code) {
        var ref = new Object() {
            Long id;
        };
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> ref.id = sysRoleService.adminCreate(
                        new AdminSysRoleUpsertRequest().setCode(code).setName("IT角色").setDescription("d")));
        return ref.id;
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.runIgnored(() -> {
            sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                            .likeRight(SysUserEntity::getPin, "itu"))
                    .forEach(u -> sysUserMapper.deleteById(u.getId()));
            sysRoleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                            .likeRight(SysRoleEntity::getCode, "itr"))
                    .forEach(r -> sysRoleMapper.deleteById(r.getId()));
        });
    }
}
