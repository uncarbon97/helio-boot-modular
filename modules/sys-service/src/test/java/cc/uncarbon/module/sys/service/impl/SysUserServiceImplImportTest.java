package cc.uncarbon.module.sys.service.impl;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.db.fieldcrypto.blindindex.BlindIndexCalculator;
import cc.uncarbon.framework.helium.excel.util.ExcelIoUtil;
import cc.uncarbon.framework.helium.tenant.enums.TenantLoginModeEnum;
import cc.uncarbon.module.commons.errorcode.DefaultErrorCodeEnum;
import cc.uncarbon.module.commons.exception.HasRepeatRecordException;
import cc.uncarbon.module.commons.facade.AuditNicknameFiller;
import cc.uncarbon.module.sys.AuditNicknameFillerTestSupport;
import cc.uncarbon.module.sys.MybatisPlusTestSupport;
import cc.uncarbon.module.sys.dal.entity.SysRoleEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.entity.SysUserTenantRelationEntity;
import cc.uncarbon.module.sys.dal.mapper.SysRoleMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.dal.mapper.SysUserTenantRelationMapper;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.errorcode.SysErrorCodeEnum;
import cc.uncarbon.module.sys.model.internal.UserRoleScope;
import cc.uncarbon.module.sys.model.request.AdminSysUserImportRequest;
import cc.uncarbon.module.sys.model.response.SysUserImportResult;
import cc.uncarbon.module.sys.service.SysRoleMenuRelationService;
import cc.uncarbon.module.sys.service.SysUserDeptRelationService;
import cc.uncarbon.module.sys.service.SysUserRoleRelationService;
import cc.uncarbon.module.sys.support.UserRoleHelper;
import cc.uncarbon.module.tenant.facade.TenantConfigFacade;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;

/**
 * {@link SysUserServiceImpl#adminImport} 校验分支与入库行为测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysUserServiceImplImportTest {

    @BeforeAll
    static void initTableInfoCache() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysUserTenantRelationMapper sysUserTenantRelationMapper;
    @Mock
    private SysDeptServiceImpl sysDeptService;
    @Mock
    private SysUserDeptRelationService sysUserDeptRelationService;
    @Mock
    private SysUserRoleRelationService sysUserRoleRelationService;
    @Mock
    private SysRoleMenuRelationService sysRoleMenuRelationService;
    @Mock
    private TenantConfigFacade tenantConfigFacade;
    @Mock
    private UserRoleHelper userRoleHelper;
    @Spy
    private AuditNicknameFiller auditNicknameFiller = AuditNicknameFillerTestSupport.passThrough();
    @Mock
    private BlindIndexCalculator blindIndexCalculator;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private SysUserServiceImpl service;

    @BeforeEach
    void initService() {
        Mockito.when(tenantConfigFacade.getLoginMode()).thenReturn(TenantLoginModeEnum.TENANT_FIRST);
        // 事务模板直通执行回调
        Mockito.when(transactionTemplate.execute(any())).thenAnswer(inv ->
                ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
        // insert 打桩回填自增ID
        Mockito.when(sysUserMapper.insert(any(SysUserEntity.class))).thenAnswer(inv -> {
            SysUserEntity entity = inv.getArgument(0);
            entity.setId((long) (System.nanoTime() % 1_000_000_000L));
            return 1;
        });
        Mockito.when(sysUserMapper.selectList(any())).thenReturn(List.of());
    }


    @Test
    void importHappyPathEnabledByDefault() {
        byte[] xlsx = workbook(List.of(
                List.<Object>of("happy01", "甲", "男", "13900000001", "happy01@example.com"),
                List.<Object>of("happy02", "乙", "女", "", "")));

        var result = importUsers(xlsx, new AdminSysUserImportRequest().setInitPwd("Passw0rd"));

        Assertions.assertEquals(2, result.getTotalRows());
        Assertions.assertEquals(2, result.getSuccessCount());
        Assertions.assertEquals(0, result.getFailedCount());
        Assertions.assertEquals(2, result.getImportedUserIds().size());
        Mockito.verify(sysUserMapper, Mockito.times(2)).insert(any(SysUserEntity.class));
        Mockito.verify(sysUserMapper, Mockito.times(1))
                .updateStatusBatch(anyCollection(), eq(SysUserStatusEnum.ENABLED));
        Mockito.verify(sysUserTenantRelationMapper, Mockito.times(2))
                .insert(any(SysUserTenantRelationEntity.class));
    }

    @Test
    void importExplicitDisableSkipsEnableUpdate() {
        byte[] xlsx = workbook(List.of(List.<Object>of("disab01", "甲", "男", "", "")));

        var result = importUsers(xlsx, new AdminSysUserImportRequest().setInitPwd("Passw0rd").setAutoEnable(false));

        Assertions.assertEquals(1, result.getSuccessCount());
        Mockito.verify(sysUserMapper, Mockito.never()).updateStatusBatch(anyCollection(), any());
    }

    @Test
    void anyRowErrorRejectsWholeFile() {
        byte[] xlsx = workbook(List.of(
                List.<Object>of("ab", "坏行", "男", "", ""),
                List.<Object>of("good01", "好", "女", "", ""),
                List.<Object>of("dup001", "重1", "男", "", ""),
                List.<Object>of("dup001", "重2", "男", "", "")));

        var result = importUsers(xlsx, new AdminSysUserImportRequest().setInitPwd("Passw0rd"));

        Assertions.assertEquals(4, result.getTotalRows());
        Assertions.assertEquals(0, result.getSuccessCount());
        Assertions.assertEquals(4, result.getFailedCount());
        Assertions.assertEquals(2, result.getErrors().size());
        Mockito.verify(sysUserMapper, Mockito.never()).insert(any(SysUserEntity.class));
    }

    @Test
    void existingDbPinRejected() {
        Mockito.when(sysUserMapper.selectList(any())).thenReturn(
                List.of(new SysUserEntity().setPin("taken01")));
        byte[] xlsx = workbook(List.of(List.<Object>of("taken01", "甲", "男", "", "")));

        var result = importUsers(xlsx, new AdminSysUserImportRequest().setInitPwd("Passw0rd"));

        Assertions.assertEquals(0, result.getSuccessCount());
        Assertions.assertEquals(1, result.getErrors().size());
        Assertions.assertEquals("账号已存在", result.getErrors().getFirst().getReason());
        Mockito.verify(sysUserMapper, Mockito.never()).insert(any(SysUserEntity.class));
    }

    @Test
    void badGenderAndPhoneCollected() {
        byte[] xlsx = workbook(List.of(List.<Object>of("gen001", "甲", "不明", "12345", "")));

        var result = importUsers(xlsx, new AdminSysUserImportRequest().setInitPwd("Passw0rd"));

        Assertions.assertEquals(0, result.getSuccessCount());
        Assertions.assertEquals(2, result.getErrors().size());
    }

    @Test
    void defaultRolesBoundAndOverRoleRejected() {
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(UserRoleScope.mockSuperAdmin());
        byte[] xlsx = workbook(List.of(List.<Object>of("role001", "甲", "男", "", "")));

        importUsers(xlsx, new AdminSysUserImportRequest().setInitPwd("Passw0rd").setDefaultRoleIds(List.of(5L)));

        Mockito.verify(sysUserRoleRelationService).cleanAndBindByUser(any(), eq(List.of(5L)));

        // 非超管越权绑定 → A01022
        Mockito.when(userRoleHelper.getCurrentUserRole()).thenReturn(
                new UserRoleScope(List.of(7L), List.of(new SysRoleEntity().setId(7L))));
        byte[] xlsx2 = workbook(List.of(List.<Object>of("role002", "乙", "男", "", "")));
        var ex = Assertions.assertThrows(BusinessException.class, () ->
                importUsers(xlsx2, new AdminSysUserImportRequest().setInitPwd("Passw0rd").setDefaultRoleIds(List.of(5L))));
        Assertions.assertEquals(SysErrorCodeEnum.A01022, ex.getErrorCode());
    }

    @Test
    void concurrentPinDuringInsertRollsBack() {
        Mockito.when(sysUserMapper.selectList(any()))
                .thenReturn(List.of())
                .thenReturn(List.of(new SysUserEntity().setPin("race001")));

        byte[] xlsx = workbook(List.of(List.<Object>of("race001", "甲", "男", "", "")));

        Assertions.assertThrows(HasRepeatRecordException.class,
                () -> importUsers(xlsx, new AdminSysUserImportRequest().setInitPwd("Passw0rd")));
    }

    @Test
    void wrongExtensionOrBadPwdRejected() {
        Assertions.assertThrows(BusinessException.class, () -> service.adminImport(
                new ByteArrayInputStream(new byte[0]), "users.xls",
                new AdminSysUserImportRequest().setInitPwd("Passw0rd")));

        byte[] xlsx = workbook(List.of(List.<Object>of("ext001", "甲", "男", "", "")));
        var ex = Assertions.assertThrows(BusinessException.class, () -> service.adminImport(
                new ByteArrayInputStream(xlsx), "users.xlsx",
                new AdminSysUserImportRequest().setInitPwd("123")));
        Assertions.assertEquals(SysErrorCodeEnum.A01053, ex.getErrorCode());
    }

    @Test
    void templateContainsHeaderAndExampleRow() {
        var out = new ByteArrayOutputStream();
        service.adminWriteImportTemplate(out);

        var sheet = ExcelIoUtil.parse(new ByteArrayInputStream(out.toByteArray()), 10,
                DefaultErrorCodeEnum.A00007, DefaultErrorCodeEnum.A00006);
        Assertions.assertTrue(sheet.hasAllHeaders(List.of("账号", "昵称", "性别", "手机号", "邮箱")));
        Assertions.assertEquals(1, sheet.rows().size());
    }


    private byte[] workbook(List<List<Object>> rows) {
        var out = new ByteArrayOutputStream();
        ExcelWriter writer = ExcelIoUtil.newWriter(out);
        try {
            WriteSheet sheet = ExcelIoUtil.newSheet(0, "用户",
                    Stream.of("账号", "昵称", "性别", "手机号", "邮箱").map(List::of).toList());
            writer.write(rows, sheet);
        } finally {
            ExcelIoUtil.finish(writer);
        }
        return out.toByteArray();
    }

    private SysUserImportResult importUsers(byte[] xlsx, AdminSysUserImportRequest attr) {
        return service.adminImport(new ByteArrayInputStream(xlsx), "users.xlsx", attr);
    }
}
