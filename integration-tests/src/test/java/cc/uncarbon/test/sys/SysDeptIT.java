package cc.uncarbon.test.sys;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.commons.exception.NoRecordException;
import cc.uncarbon.module.commons.model.request.AdminSetStatusRequest;
import cc.uncarbon.framework.helium.db.enums.EnabledStatusEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.request.AdminSysDeptUpsertRequest;
import cc.uncarbon.module.sys.model.valueobj.SysDeptDTO;
import cc.uncarbon.module.sys.service.SysDeptService;
import cc.uncarbon.test.base.BaseIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import cc.uncarbon.module.sys.dal.mapper.SysDeptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.uncarbon.module.sys.dal.entity.SysDeptEntity;

/**
 * 部门管理集成测试：CRUD、同名拦截、成环拦截、级联停启用、删除约束
 * 在平台自营域（租户 0）视角执行；LINE/DATASOURCE/NONE 均适用
 */
@Tag("sys")
class SysDeptIT extends BaseIntegrationTest {

    @Resource
    private SysDeptService sysDeptService;

    @Resource
    private SysDeptMapper sysDeptMapper;


    @Test
    void deptCrudLifecycle() {
        Long rootId = create("IT根部门", null);
        Assertions.assertNotNull(rootId);
        // 根部门 parentId 置空返回
        SysDeptDTO root = sysDeptService.getNonnullById(rootId);
        Assertions.assertNull(root.getParentId());

        Long childId = create("IT子部门", rootId);
        Assertions.assertNotNull(childId);

        // 同一上级下同名 → A01040
        var dup = Assertions.assertThrows(BusinessException.class,
                () -> withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                        () -> sysDeptService.adminCreate(
                                new AdminSysDeptUpsertRequest().setName("IT根部门").setParentId(null))));
        Assertions.assertEquals(SysErrorCodeEnum.A01040, ((BusinessException) dup).getErrorCode());

        // 上级不存在 → NoRecord
        Assertions.assertThrows(NoRecordException.class,
                () -> withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                        () -> sysDeptService.adminCreate(
                                new AdminSysDeptUpsertRequest().setName("IT孤儿").setParentId(99_999_999L))));

        // 上级为自身 → A01039
        var cycle = Assertions.assertThrows(BusinessException.class,
                () -> withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                        () -> sysDeptService.adminUpdate(
                                new AdminSysDeptUpsertRequest().setId(rootId).setName("IT根部门").setParentId(rootId))));
        Assertions.assertEquals(SysErrorCodeEnum.A01039, ((BusinessException) cycle).getErrorCode());

        // 有下级不能删 → A01038
        var delParent = Assertions.assertThrows(BusinessException.class,
                () -> withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                        () -> sysDeptService.adminDelete(List.of(rootId))));
        Assertions.assertEquals(SysErrorCodeEnum.A01038, ((BusinessException) delParent).getErrorCode());

        // 幂等停启用
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () -> {
            sysDeptService.adminSetStatus(new AdminSetStatusRequest<Long, EnabledStatusEnum>()
                    .setId(childId).setNewStatus(EnabledStatusEnum.ENABLED));
            Assertions.assertEquals(EnabledStatusEnum.ENABLED, sysDeptService.getNonnullById(childId).getStatus());
        });

        // 父停用后，子不能再启用（先停父再启用子 → A01037）
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () -> {
            sysDeptService.adminSetStatus(new AdminSetStatusRequest<Long, EnabledStatusEnum>()
                    .setId(rootId).setNewStatus(EnabledStatusEnum.DISABLED));
        });
        var enableChild = Assertions.assertThrows(BusinessException.class,
                () -> withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                        () -> sysDeptService.adminSetStatus(new AdminSetStatusRequest<Long, EnabledStatusEnum>()
                                .setId(childId).setNewStatus(EnabledStatusEnum.ENABLED))));
        Assertions.assertEquals(SysErrorCodeEnum.A01037, ((BusinessException) enableChild).getErrorCode());

        // 叶子可删
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> sysDeptService.adminDelete(List.of(childId)));
        Assertions.assertNull(sysDeptService.getById(childId));
    }

    @Test
    void selectOptionListsEnabledOnly() {
        Long id = create("IT启用部门", null);

        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () -> {
            List<SysDeptDTO> all = sysDeptService.adminListSelectOption(false);
            Assertions.assertTrue(all.stream().anyMatch(d -> id.equals(d.getId())));
        });
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    private Long create(String name, Long parentId) {
        var ref = new Object() {
            Long id;
        };
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                () -> ref.id = sysDeptService.adminCreate(
                        new AdminSysDeptUpsertRequest()
                                .setName(name + "_" + System.nanoTime())
                                .setParentId(parentId).setSort(99)));
        return ref.id;
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.runIgnored(() ->
                sysDeptMapper.selectList(new LambdaQueryWrapper<SysDeptEntity>()
                                .likeRight(SysDeptEntity::getName, "IT"))
                        .forEach(d -> sysDeptMapper.deleteById(d.getId())));
    }
}
