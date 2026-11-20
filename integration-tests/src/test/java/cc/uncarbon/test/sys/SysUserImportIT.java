package cc.uncarbon.test.sys;

import cc.uncarbon.framework.helium.db.enums.YesOrNoEnum;
import cc.uncarbon.framework.helium.excel.util.ExcelIoUtil;
import cc.uncarbon.module.sys.dal.entity.SysUserEntity;
import cc.uncarbon.module.sys.dal.mapper.SysUserMapper;
import cc.uncarbon.module.sys.enums.SysUserStatusEnum;
import cc.uncarbon.module.sys.model.request.AdminSysUserImportRequest;
import cc.uncarbon.module.sys.model.response.SysUserImportResult;
import cc.uncarbon.module.sys.service.SysUserService;
import cc.uncarbon.test.base.BaseIntegrationTest;
import jakarta.annotation.Resource;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 用户批量导入集成测试：落库、默认启用、PII 可空、首登改密、重复导入幂等失败
 * 超管（用户 0）在平台自营域视角执行
 */
@Tag("sys")
class SysUserImportIT extends BaseIntegrationTest {

    @Resource
    private SysUserService sysUserService;
    @Resource
    private SysUserMapper sysUserMapper;


    @Test
    void importUsersHappyPathAndReimportFails() {
        String pinA = "imp" + (System.nanoTime() % 1_000_000_000L);
        String pinB = pinA + "b";
        byte[] xlsx = workbook(List.of(
                List.<Object>of(pinA, "导入甲", "男", "13900000001", "imp.a@example.com"),
                List.<Object>of(pinB, "导入乙", "女", "", "")));
        AdminSysUserImportRequest attr = new AdminSysUserImportRequest().setInitPwd("Passw0rd");

        var first = new AtomicReference<SysUserImportResult>();
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () ->
                first.set(sysUserService.adminImportUsers(new ByteArrayInputStream(xlsx), "users.xlsx", attr)));

        Assertions.assertEquals(2, first.get().getTotalRows());
        Assertions.assertEquals(2, first.get().getSuccessCount());
        Assertions.assertEquals(0, first.get().getFailedCount());

        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () -> {
            SysUserEntity a = sysUserMapper.getByPin(pinA);
            Assertions.assertNotNull(a);
            // autoEnable 默认 true
            Assertions.assertEquals(SysUserStatusEnum.ENABLED, a.getStatus());
            Assertions.assertEquals("13900000001", a.getPhoneNo());
            Assertions.assertEquals(YesOrNoEnum.YES, a.getPwdMustChange());

            SysUserEntity b = sysUserMapper.getByPin(pinB);
            Assertions.assertNotNull(b);
            // 手机号/邮箱可空
            Assertions.assertNull(b.getPhoneNo());
            Assertions.assertNull(b.getEmail());
        });

        // 重复导入同一文件 → 整文件失败，无半截数据
        var second = new AtomicReference<SysUserImportResult>();
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () ->
                second.set(sysUserService.adminImportUsers(new ByteArrayInputStream(xlsx), "users.xlsx", attr)));
        Assertions.assertEquals(0, second.get().getSuccessCount());
        Assertions.assertEquals(2, second.get().getFailedCount());

        // 清理
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () ->
                sysUserService.adminDelete(List.of(
                        sysUserMapper.getByPin(pinA).getId(), sysUserMapper.getByPin(pinB).getId())));
    }


    private byte[] workbook(List<List<Object>> rows) {
        var out = new ByteArrayOutputStream();
        ExcelWriter writer = ExcelIoUtil.newWriter(out);
        try {
            WriteSheet sheet = ExcelIoUtil.newSheet(0, "用户",
                    java.util.stream.Stream.of("账号", "昵称", "性别", "手机号", "邮箱").map(List::of).toList());
            writer.write(rows, sheet);
        } finally {
            ExcelIoUtil.finishQuietly(writer);
        }
        return out.toByteArray();
    }
}
