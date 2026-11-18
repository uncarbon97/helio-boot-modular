package cc.uncarbon.test.sys;

import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.module.adminapi.annotation.SysOperateLog;
import cc.uncarbon.module.sys.dal.entity.SysOperateLogEntity;
import cc.uncarbon.module.sys.dal.mapper.SysOperateLogMapper;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import cc.uncarbon.test.base.BaseIntegrationTest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 操作日志审计集成测试：SQL 运行超时时，@SysOperateLog 失败日志仍落库
 * <p>
 * 探针方法触发 MyBatis 语句级超时（@Options(timeout=1) + SLEEP(3)，MySQL JDBC 查询超时），
 * 断言异常正常抛出的同时，操作日志落一条 FAILED 记录（超管在平台自营域视角执行）
 */
@Tag("sys")
class SysOperateLogSqlTimeoutIT extends BaseIntegrationTest {

    private static final String PROBE_BIZ_TYPE = "IT-SQL超时探针";
    private static final String PROBE_BEHAVIOR = "慢查询探针";
    private static final String PROBE_FAIL_ACTION = "SQL 执行超时";

    /**
     * 探针与 Mapper 仅在本测试上下文装配，不污染生产包扫描
     */
    @TestConfiguration
    @MapperScan(basePackages = "cc.uncarbon.test.sys")
    static class ProbeConfig {

        @Bean
        SqlTimeoutProbe sqlTimeoutProbe(SqlTimeoutProbeMapper mapper) {
            return new SqlTimeoutProbe(mapper);
        }
    }

    static class SqlTimeoutProbe {

        private final SqlTimeoutProbeMapper mapper;

        SqlTimeoutProbe(SqlTimeoutProbeMapper mapper) {
            this.mapper = mapper;
        }

        @SysOperateLog(bizType = PROBE_BIZ_TYPE, behavior = PROBE_BEHAVIOR,
                success = "探针执行成功", fail = PROBE_FAIL_ACTION)
        public Integer probe() {
            return mapper.sleepThreeSeconds();
        }
    }

    @Resource
    private SqlTimeoutProbe sqlTimeoutProbe;
    @Resource
    private SysOperateLogMapper operateLogMapper;


    @Test
    void sqlTimeoutStillRecordsFailedOperateLog() {
        // 1. SQL 超时异常正常上抛，不被日志切面吞掉
        Exception thrown = Assertions.assertThrows(Exception.class, () ->
                withContext(testUser(0L, "admin"), testTenant(0L, "平台", "platform"),
                        () -> sqlTimeoutProbe.probe()));
        Assertions.assertTrue(mentionsTimeout(thrown),
                () -> "异常链应包含超时信息, 实际: " + thrown);

        // 2. 同步落一条 FAILED 操作日志；操作人回退线程上下文用户(0)
        AtomicReference<List<SysOperateLogEntity>> rowsRef = new AtomicReference<>();
        TenantContextHolder.runIgnored(() -> rowsRef.set(operateLogMapper.selectList(
                new LambdaQueryWrapper<SysOperateLogEntity>()
                        .eq(SysOperateLogEntity::getBizType, PROBE_BIZ_TYPE))));

        List<SysOperateLogEntity> rows = rowsRef.get();
        Assertions.assertEquals(1, rows.size(), () -> "应恰好落一条探针日志, 实际: " + rows.size());
        SysOperateLogEntity row = rows.get(0);
        Assertions.assertEquals(LogResultStatusEnum.FAILED, row.getResultStatus());
        Assertions.assertEquals(PROBE_FAIL_ACTION, row.getOperation());
        Assertions.assertEquals(Long.valueOf(0L), row.getUserId());
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    /**
     * 异常及其原因链的类名/消息里是否提及 timeout
     */
    private static boolean mentionsTimeout(Throwable thrown) {
        for (Throwable t = thrown; t != null; t = t.getCause()) {
            if (t == t.getCause()) {
                break;
            }
            String text = (t.getClass().getSimpleName() + " " + t.getMessage()).toLowerCase();
            if (text.contains("timeout")) {
                return true;
            }
        }
        return false;
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.runIgnored(() ->
                operateLogMapper.selectList(new LambdaQueryWrapper<SysOperateLogEntity>()
                                .eq(SysOperateLogEntity::getBizType, PROBE_BIZ_TYPE))
                        .forEach(l -> operateLogMapper.deleteById(l.getId())));
    }
}
