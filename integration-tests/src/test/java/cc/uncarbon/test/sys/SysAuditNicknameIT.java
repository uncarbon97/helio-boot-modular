package cc.uncarbon.test.sys;

import cc.uncarbon.framework.helium.i18n.util.I18nMessageUtil;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.sys.dal.entity.SysDeptEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysDeptMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.model.request.AdminSysDeptUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysDeptDTO;
import cc.uncarbon.module.sys.service.SysDeptService;
import cc.uncarbon.test.base.BaseIntegrationTest;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 审计操作人昵称填充集成测试：列表/详情回填、已删除用户国际化文案、跨租户昵称解析
 * 在平台自营域（租户 0）视角执行；LINE/DATASOURCE/NONE 均适用
 */
@Tag("sys")
class SysAuditNicknameIT extends BaseIntegrationTest {

    private static final Long SUPER_ADMIN_USER_ID = 0L;
    private static final Long GHOST_USER_ID = 999_999_999L;

    @Resource
    private SysDeptService sysDeptService;
    @Resource
    private SysDeptMapper sysDeptMapper;
    @Resource
    private SysUserMapper sysUserMapper;


    @Test
    void listAndDetailCarryCreatorNickname() {
        Long deptId = createDept("IT审计昵称部门");

        withContext(testUser(SUPER_ADMIN_USER_ID, "admin"), testTenant(0L, "平台", "platform"), () -> {
            String expected = expectedDisplayOf(SUPER_ADMIN_USER_ID);

            SysDeptDTO detail = sysDeptService.getNonnullById(deptId);
            Assertions.assertEquals(expected, detail.getCreatedBy());

            List<SysDeptDTO> list = sysDeptService.adminList();
            Assertions.assertEquals(expected, list.stream()
                    .filter(d -> deptId.equals(d.getId()))
                    .findFirst().orElseThrow().getCreatedBy());
        });
    }

    @Test
    void deletedUpdatorShowsI18nText() {
        Long deptId = createDept("IT审计昵称部门");

        // 直接改库伪造 updatedBy 指向不存在用户（绕过服务层校验与自动填充）
        withContext(testUser(SUPER_ADMIN_USER_ID, "admin"), testTenant(0L, "平台", "platform"), () ->
                sysDeptMapper.update(null, new LambdaUpdateWrapper<SysDeptEntity>()
                        .eq(SysDeptEntity::getId, deptId)
                        .set(SysDeptEntity::getUpdatedBy, GHOST_USER_ID)));

        withContext(testUser(SUPER_ADMIN_USER_ID, "admin"), testTenant(0L, "平台", "platform"), () -> {
            SysDeptDTO detail = sysDeptService.getNonnullById(deptId);
            Assertions.assertEquals(
                    I18nMessageUtil.messageOf("sys.user.deleted-nickname", "已删除用户({})", GHOST_USER_ID),
                    detail.getUpdatedBy());
        });
    }

    /**
     * 昵称按用户ID全局解析：租户 1 域内由平台域用户(0)创建的记录，也能带出操作人昵称
     */
    @Test
    void nicknameResolvedAcrossTenants() {
        var ref = new Object() {
            Long id;
        };
        withContext(testUser(SUPER_ADMIN_USER_ID, "admin"), testTenant(1L, "租户一", "tenant-one"),
                () -> ref.id = sysDeptService.adminCreate(
                        new AdminSysDeptUpsertRequest()
                                .setName("IT审计昵称部门_" + System.nanoTime())
                                .setSort(99)));
        Long deptId = ref.id;

        withContext(testUser(SUPER_ADMIN_USER_ID, "admin"), testTenant(1L, "租户一", "tenant-one"), () -> {
            SysDeptDTO detail = sysDeptService.getNonnullById(deptId);
            Assertions.assertEquals(expectedDisplayOf(SUPER_ADMIN_USER_ID), detail.getCreatedBy());
        });
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    private Long createDept(String name) {
        var ref = new Object() {
            Long id;
        };
        withContext(testUser(SUPER_ADMIN_USER_ID, "admin"), testTenant(0L, "平台", "platform"),
                () -> ref.id = sysDeptService.adminCreate(
                        new AdminSysDeptUpsertRequest()
                                .setName(name + "_" + System.nanoTime())
                                .setSort(99)));
        return ref.id;
    }

    /**
     * 计算用户展示名（与填充器口径一致：昵称优先，缺失回退账号 pin），忽略租户隔离
     */
    private String expectedDisplayOf(Long userId) {
        var ref = new Object() {
            String display;
        };
        TenantContextHolder.runIgnored(() -> {
            SysUserEntity user = sysUserMapper.selectById(userId);
            ref.display = CharSequenceUtil.isNotBlank(user.getNickname())
                    ? user.getNickname()
                    : user.getPin();
        });
        return ref.display;
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.runIgnored(() ->
                sysDeptMapper.selectList(new LambdaQueryWrapper<SysDeptEntity>()
                                .likeRight(SysDeptEntity::getName, "IT"))
                        .forEach(d -> sysDeptMapper.deleteById(d.getId())));
    }
}
