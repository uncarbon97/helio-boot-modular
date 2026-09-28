package cc.uncarbon.test.sys;

import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.web.context.SimpleVisitorContext;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import cc.uncarbon.module.sys.enums.LoginLogTypeEnum;
import cc.uncarbon.module.sys.model.query.AdminSysLoginLogListQuery;
import cc.uncarbon.module.sys.model.query.AdminSysOperateLogListQuery;
import cc.uncarbon.module.sys.model.request.SysLoginLogCreateRequest;
import cc.uncarbon.module.sys.model.request.SysOperateLogCreateRequest;
import cc.uncarbon.module.sys.model.valueobj.SysLoginLogDTO;
import cc.uncarbon.module.sys.model.valueobj.SysOperateLogDTO;
import cc.uncarbon.module.sys.service.SysLoginLogService;
import cc.uncarbon.module.sys.service.SysOperateLogService;
import cc.uncarbon.test.base.BaseIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import cc.uncarbon.module.sys.dal.mapper.SysLoginLogMapper;
import cc.uncarbon.module.sys.dal.mapper.SysOperateLogMapper;
import cc.uncarbon.module.sys.service.impl.SysLoginLogServiceImpl;
import cc.uncarbon.framework.helium.base.page.PageParam;
import cc.uncarbon.module.commons.enums.UserTypeCodeEnum;
import cc.uncarbon.module.sys.service.impl.SysOperateLogServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.uncarbon.module.sys.dal.entity.SysLoginLogEntity;
import cc.uncarbon.module.sys.dal.entity.SysOperateLogEntity;

/**
 * 登录/操作日志集成测试：落库、UA 超长截断、UA 解析回显、条件分页
 */
@Tag("sys")
class SysLogIT extends BaseIntegrationTest {

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "Chrome/120.0.0.0 Safari/537.36";

    @Resource
    private SysLoginLogService sysLoginLogService;
    @Resource
    private SysOperateLogService sysOperateLogService;

    @Resource
    private SysLoginLogMapper loginLogMapper;
    @Resource
    private SysOperateLogMapper operateLogMapper;


    @Test
    void loginLogLifecycle() {
        String pin = "itl" + (System.nanoTime() % 1_000_000_000L);
        String longUa = UA + "x".repeat(300);

        Long logId = sysLoginLogService.create(new SysLoginLogCreateRequest()
                .setLoginLogType(LoginLogTypeEnum.PASSWORD_LOGIN)
                .setUserPin(pin).setUserTypeCode("ADMIN_USER")
                .setVisitorContext(new SimpleVisitorContext()
                        .setClientIp("127.0.0.1").setUserAgent(longUa))
                .setResultStatus(LogResultStatusEnum.SUCCESS)
                .setTenantId(0L));

        // UA 截断到 255
        var entity = loginLogMapper.selectById(logId);
        Assertions.assertEquals(SysLoginLogServiceImpl.USER_AGENT_MAX_LENGTH,
                entity.getVisitorUserAgent().length());

        // DTO 回显 UA 解析结果
        SysLoginLogDTO dto = sysLoginLogService.getNonnullById(logId);
        Assertions.assertNotNull(dto.getVisitorBrowser());
        Assertions.assertNotNull(dto.getVisitorOs());

        // 账号筛选 + 结果状态筛选
        var page = sysLoginLogService.adminList(new AdminSysLoginLogListQuery()
                .setPageParam(new PageParam(1, 10))
                .setUserPin(pin).setResultStatus(LogResultStatusEnum.SUCCESS));
        Assertions.assertTrue(page.getRecords().stream().anyMatch(l -> logId.equals(l.getId())));
    }

    @Test
    void operateLogLifecycle() {
        String bizNo = "itb" + (System.nanoTime() % 1_000_000_000L);
        String longUa = UA + "y".repeat(300);

        Long logId = sysOperateLogService.create(new SysOperateLogCreateRequest()
                .setBizType("IT业务").setBehavior("IT行为").setBizNo(bizNo)
                .setOperation("测试操作").setUserTypeCode(UserTypeCodeEnum.ADMIN_USER)
                .setVisitorIp("127.0.0.1").setVisitorUserAgent(longUa)
                .setResultStatus(LogResultStatusEnum.SUCCESS));

        // UA 截断到 250
        var entity = operateLogMapper.selectById(logId);
        Assertions.assertEquals(SysOperateLogServiceImpl.USER_AGENT_MAX_LENGTH,
                entity.getVisitorUserAgent().length());

        // bizNo 筛选
        var page = sysOperateLogService.adminList(new AdminSysOperateLogListQuery()
                .setPageParam(new PageParam(1, 10))
                .setBizNo(bizNo).setBeginAt(Instant.now().minusSeconds(60)).setEndAt(Instant.now().plusSeconds(60)));
        Assertions.assertTrue(page.getRecords().stream().anyMatch(l -> logId.equals(l.getId())));
        SysOperateLogDTO dto = sysOperateLogService.getNonnullById(logId);
        Assertions.assertEquals("IT行为", dto.getBehavior());
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    @AfterEach
    void cleanup() {
        TenantContextHolder.runIgnored(() -> {
            loginLogMapper.selectList(new LambdaQueryWrapper<SysLoginLogEntity>()
                            .likeRight(SysLoginLogEntity::getUserPin, "itl"))
                    .forEach(l -> loginLogMapper.deleteById(l.getId()));
            operateLogMapper.selectList(new LambdaQueryWrapper<SysOperateLogEntity>()
                            .likeRight(SysOperateLogEntity::getBizNo, "itb"))
                    .forEach(l -> operateLogMapper.deleteById(l.getId()));
        });
    }
}
