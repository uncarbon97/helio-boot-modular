package cc.uncarbon.test.sys;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.query.AdminSysUserListQuery;
import cc.uncarbon.module.sys.model.request.AdminSysRoleUpsertRequest;
import cc.uncarbon.module.sys.model.request.AdminSysUserBindRoleRequest;
import cc.uncarbon.module.sys.model.request.AdminSysUserCreateRequest;
import cc.uncarbon.module.sys.model.valueobj.SysUserDTO;
import cc.uncarbon.module.sys.service.SysRoleService;
import cc.uncarbon.module.sys.service.SysUserService;
import cc.uncarbon.test.base.BaseIntegrationTest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

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
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () -> {
            SysUserDTO dto = sysUserService.getOperableById(userId);
            Assertions.assertNotNull(dto);
            Assertions.assertEquals(SysUserStatusEnum.DISABLED, dto.getStatus());
        });

        // 同 pin 重复 → A01030
        var dup = Assertions.assertThrows(HasRepeatRecordException.class, () -> create(pin, "13800002222"));
        Assertions.assertEquals(SysErrorCodeEnum.A01030, ((HasRepeatRecordException) dup).getErrorCode());

        // 启用
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> sysUserService.adminSetStatus(new AdminSetStatusRequest<Long, SysUserStatusEnum>()
                        .setId(userId).setNewStatus(SysUserStatusEnum.ENABLED)));
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () ->
                Assertions.assertEquals(SysUserStatusEnum.ENABLED,
                        sysUserService.getOperableById(userId).getStatus()));

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
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () -> {
            var page = sysUserService.adminList(new AdminSysUserListQuery()
                    .setPageParam(new PageParam(1, 10)).setPhoneNoTail4("13800001111"));
            Assertions.assertTrue(page.getRecords().stream().anyMatch(u -> userId.equals(u.getId())));
        });

        // 删除 → 关联一并清理
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> sysUserService.adminDelete(List.of(userId)));
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> Assertions.assertNull(sysUserService.getOperableById(userId)));
    }


    /**
     * 局部更新端到端回归：派生列（masked/盲索引）不携带来源时不得被 NULL 覆写，
     * 且 updated_at 照常盖章——曾因派生列 fill 无条件拼接进 SET，登录即触发派生列被抹
     */
    @Test
    void partialUpdateKeepsDerivedColumns() {
        String pin = "itu" + (System.nanoTime() % 1_000_000_000L);
        Long userId = create(pin, "13800003333");

        SysUserEntity before = selectUser(userId);
        Assertions.assertNotNull(before.getPhoneNoMasked());
        Assertions.assertNotNull(before.getPhoneNoFullBlindIndex());
        Assertions.assertNotNull(before.getPhoneNoTail4BlindIndex());
        Assertions.assertNull(before.getUpdatedAt(), "创建时不应盖 updated_at（由更新路径盖章）");

        // 登录路径：updateById 部分实体
        TenantContextHolder.runIgnored(() -> sysUserMapper.updateLastLoginAt(userId, Instant.now()));
        // 状态路径：空实体 + wrapper 局部更新
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> sysUserService.adminSetStatus(new AdminSetStatusRequest<Long, SysUserStatusEnum>()
                        .setId(userId).setNewStatus(SysUserStatusEnum.ENABLED)));

        SysUserEntity after = selectUser(userId);
        Assertions.assertEquals(before.getPhoneNo(), after.getPhoneNo());
        Assertions.assertEquals(before.getPhoneNoMasked(), after.getPhoneNoMasked());
        Assertions.assertEquals(before.getPhoneNoFullBlindIndex(), after.getPhoneNoFullBlindIndex());
        Assertions.assertEquals(before.getPhoneNoTail4BlindIndex(), after.getPhoneNoTail4BlindIndex());
        Assertions.assertEquals(before.getEmail(), after.getEmail());
        Assertions.assertEquals(before.getEmailMasked(), after.getEmailMasked());
        Assertions.assertEquals(before.getEmailFullBlindIndex(), after.getEmailFullBlindIndex());
        Assertions.assertNotNull(after.getLastLoginAt());
        Assertions.assertEquals(SysUserStatusEnum.ENABLED, after.getStatus());
        Assertions.assertNotNull(after.getUpdatedAt(), "局部更新应照常盖 updated_at");
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    @SneakyThrows
    private SysUserEntity selectUser(Long userId) {
        return TenantContextHolder.callIgnored(() -> sysUserMapper.selectById(userId));
    }

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
            // 只取 id，避免残留的异密钥密文行在结果映射时解密爆炸
            sysUserMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
                            .select(SysUserEntity::getId)
                            .likeRight(SysUserEntity::getPin, "itu"))
                    .forEach(u -> sysUserMapper.deleteById(u.getId()));
            sysRoleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                            .likeRight(SysRoleEntity::getCode, "itr"))
                    .forEach(r -> sysRoleMapper.deleteById(r.getId()));
        });
    }
}
