package cc.uncarbon.test.sys;

import cc.uncarbon.module.commons.enums.UserTypeCodeEnum;
import cc.uncarbon.module.commons.support.excel.ExcelIoUtil;
import cc.uncarbon.module.sys.dal.entity.SysOperateLogEntity;
import cc.uncarbon.module.sys.dal.mapper.SysOperateLogMapper;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import cc.uncarbon.module.sys.model.request.AdminSysOperateLogExportRequest;
import cc.uncarbon.module.sys.model.request.SysOperateLogCreateRequest;
import cc.uncarbon.module.sys.service.SysOperateLogService;
import cc.uncarbon.test.base.BaseIntegrationTest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 审计日志批量导出集成测试：筛选一致性、流式产物可回读、公式注入清洗
 */
@Tag("sys")
class SysOperateLogExportIT extends BaseIntegrationTest {

    @Resource
    private SysOperateLogService sysOperateLogService;
    @Resource
    private SysOperateLogMapper sysOperateLogMapper;

    private String bizType;

    @Test
    void exportRespectsFiltersAndSanitizesFormula() {
        bizType = "itu-export-" + (System.nanoTime() % 1_000_000_000L);
        withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () -> {
            sysOperateLogService.create(new SysOperateLogCreateRequest()
                    .setBizType(bizType)
                    .setBehavior("测试导出")
                    .setOperation("=1+1")
                    .setUserId(0L)
                    .setUserTypeCode(UserTypeCodeEnum.ADMIN_USER)
                    .setResultStatus(LogResultStatusEnum.SUCCESS));
            sysOperateLogService.create(new SysOperateLogCreateRequest()
                    .setBizType(bizType)
                    .setBehavior("测试导出2")
                    .setOperation("普通文本")
                    .setUserId(0L)
                    .setResultStatus(LogResultStatusEnum.SUCCESS));

            var out = new ByteArrayOutputStream();
            var request = new AdminSysOperateLogExportRequest();
            request.setBizType(bizType);
            sysOperateLogService.adminExportExcel(request, out);

            byte[] bytes = out.toByteArray();
            // xlsx 为 zip 容器
            Assertions.assertEquals('P', bytes[0]);
            Assertions.assertEquals('K', bytes[1]);

            var sheet = ExcelIoUtil.parse(new ByteArrayInputStream(bytes), 100);
            Assertions.assertEquals(2, sheet.rows().size());
            // 操作内容列（列序 5）做过公式注入清洗
            Assertions.assertTrue(sheet.rows().stream().anyMatch(r -> "'=1+1".equals(r.cell(5))));
            Assertions.assertTrue(sheet.rows().stream().anyMatch(r -> "普通文本".equals(r.cell(5))));
        });
    }

    @AfterEach
    void cleanUp() {
        if (bizType != null) {
            withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"), () ->
                    sysOperateLogMapper.delete(new LambdaQueryWrapper<SysOperateLogEntity>()
                            .eq(SysOperateLogEntity::getBizType, bizType)));
        }
    }
}
