package cc.uncarbon.module.adminapi.support.auditlog;

import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.bizlog.model.LogRecordModel;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.web.context.SimpleVisitorContext;
import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.framework.helium.web.context.VisitorContextHolder;
import cc.uncarbon.module.adminapi.model.internal.TenantSwitchInfo;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import cc.uncarbon.module.sys.model.request.SysOperateLogCreateRequest;
import cc.uncarbon.module.sys.service.SysOperateLogService;
import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;

import static java.lang.ScopedValue.where;

/**
 * {@link SysOperateLogRecorder} 业务日志转换与跨租户代操作留痕分支测试
 * <p>
 * sa-token 会话通过 {@link SaTokenContextMockUtil} Mock 上下文 + 内存版 Dao 模拟
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysOperateLogRecorderTest {

    private static final Long USER_ID = 9L;

    @Mock
    private SysOperateLogService sysOperateLogService;

    private SysOperateLogRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new SysOperateLogRecorder(sysOperateLogService);
    }

    @AfterEach
    void tearDown() {
        SaTokenContextMockUtil.clearContext();
        StpKit.ADMIN.logout(USER_ID);
    }


    /*
    ----------------------------------------------------------------
                        基础转换
    ----------------------------------------------------------------
     */

    @Test
    void recordMapsCoreFieldsAndOperatorId() {
        VisitorContext visitor = new SimpleVisitorContext()
                .setClientIp("10.0.0.8").setUserAgent("UA-Test")
                .setHttpRequestMethod("POST").setHttpRequestPath("/admin/v1/demo");

        where(VisitorContextHolder.scoped(), visitor).run(() ->
                recorder.record(model("100", "ORDER", "下单", false, "{\"k\":\"v\"}", "123")));

        SysOperateLogCreateRequest saved = captureSaved();
        Assertions.assertEquals("ORDER", saved.getBizType());
        Assertions.assertEquals("下单", saved.getBehavior());
        Assertions.assertEquals("100", saved.getBizNo());
        Assertions.assertEquals("操作", saved.getOperation());
        Assertions.assertEquals(LogResultStatusEnum.SUCCESS, saved.getResultStatus());
        // 操作人取框架解析出的 operatorId
        Assertions.assertEquals(123L, saved.getUserId());
        // 请求信息取自访客上下文
        Assertions.assertEquals("POST", saved.getRequestMethod());
        Assertions.assertEquals("/admin/v1/demo", saved.getRequestPath());
        Assertions.assertEquals("10.0.0.8", saved.getVisitorIp());
        Assertions.assertEquals("UA-Test", saved.getVisitorUserAgent());
        // 无用户上下文，不补充租户切换痕迹，extra 原样保留
        Assertions.assertEquals("{\"k\":\"v\"}", saved.getBizExtra());
        Assertions.assertNull(saved.getUserTypeCode());
    }

    @Test
    void recordFallsBackToUserContextWhenOperatorMissing() {
        bindUser(user("APP_USER"), () ->
                recorder.record(model("110", "ORDER", "下单", false, null, "  ")));

        SysOperateLogCreateRequest saved = captureSaved();
        Assertions.assertEquals(USER_ID, saved.getUserId());
        Assertions.assertEquals("APP_USER",
                saved.getUserTypeCode() == null ? null : saved.getUserTypeCode().name());
        // APP_USER 不走租户切换留痕
        Assertions.assertNull(saved.getBizExtra());
    }

    @Test
    void recordFallsBackToUserContextWhenOperatorNotNumeric() {
        bindUser(user("ADMIN_USER"), () ->
                recorder.record(model("111", "ORDER", "下单", false, null, "admin-u9")));

        // operator 非数字 → 回退用户上下文
        Assertions.assertEquals(USER_ID, captureSaved().getUserId());
    }

    @Test
    void recordMarksFailedResultStatus() {
        recorder.record(model("101", "ORDER", "下单", true, null, null));

        Assertions.assertEquals(LogResultStatusEnum.FAILED, captureSaved().getResultStatus());
    }

    @Test
    void recordWithoutAnyContextKeepsNullUserInfo() {
        recorder.record(model("102", "ORDER", "下单", false, null, null));

        SysOperateLogCreateRequest saved = captureSaved();
        Assertions.assertNull(saved.getUserId());
        Assertions.assertNull(saved.getUserTypeCode());
        Assertions.assertNull(saved.getRequestMethod());
        Assertions.assertNull(saved.getBizExtra());
    }

    @Test
    void recordSwallowsPersistenceFailure() {
        Mockito.doThrow(new RuntimeException("db down"))
                .when(sysOperateLogService).create(Mockito.any());

        Assertions.assertDoesNotThrow(() ->
                recorder.record(model("103", "ORDER", "下单", false, null, null)));
    }

    @Test
    void queryMethodsReturnEmpty() {
        Assertions.assertEquals(List.of(), recorder.queryLog("100", "ORDER"));
        Assertions.assertEquals(List.of(), recorder.queryLogByBizNo("100", "ORDER", "下单"));
    }


    /*
    ----------------------------------------------------------------
                        跨租户代操作留痕（管理员）
    ----------------------------------------------------------------
     */

    @Test
    void recordEnrichesFullSwitchTraceWhenOriginalTenantPresent() {
        // 切换切入场景：活动租户 5，切换前为平台自营域 0（originalTenantContext 非空）
        SaTokenContextMockUtil.setMockContext(() -> {
            StpKit.ADMIN.login(USER_ID);
            var session = StpKit.ADMIN.getSession();
            session.set(TenantContext.CAMEL_NAME, new SimpleTenantContext(5L, "租户五", "t5"));
            session.set(TenantSwitchInfo.CAMEL_NAME, new TenantSwitchInfo()
                    .setOriginalTenantContext(new SimpleTenantContext(0L, "平台", "platform"))
                    .setSwitchedAt(Instant.now()));

            bindUser(user("ADMIN_USER"), () ->
                    recorder.record(model("104", "ORDER", "下单", false, "{\"k\":\"v\"}", null)));

            JSONObject extra = JSONUtil.parseObj(captureSaved().getBizExtra());
            Assertions.assertEquals(5, extra.getInt("activeTenantId"));
            Assertions.assertEquals(Boolean.TRUE, extra.getBool("switched"));
            Assertions.assertEquals(0, extra.getInt("originalTenantId"));
            // 原 extra 内容被保留
            Assertions.assertEquals("v", extra.getStr("k"));
        });
    }

    @Test
    void recordEnrichesSwitchedWithoutOriginalTenant() {
        // USER_FIRST 视角 / 超管平台视角切入：originalTenantContext 为 null，只标记 switched
        SaTokenContextMockUtil.setMockContext(() -> {
            StpKit.ADMIN.login(USER_ID);
            var session = StpKit.ADMIN.getSession();
            session.set(TenantContext.CAMEL_NAME, new SimpleTenantContext(7L, "租户七", "t7"));
            session.set(TenantSwitchInfo.CAMEL_NAME, new TenantSwitchInfo().setSwitchedAt(Instant.now()));

            bindUser(user("ADMIN_USER"), () ->
                    recorder.record(model("105", "ORDER", "下单", false, null, null)));

            JSONObject extra = JSONUtil.parseObj(captureSaved().getBizExtra());
            Assertions.assertEquals(7, extra.getInt("activeTenantId"));
            Assertions.assertEquals(Boolean.TRUE, extra.getBool("switched"));
            Assertions.assertNull(extra.get("originalTenantId"));
        });
    }

    @Test
    void recordEnrichesActiveTenantOnlyWithoutSwitchMark() {
        // 管理员处于普通租户视角（非切换态）：只补充活动租户
        SaTokenContextMockUtil.setMockContext(() -> {
            StpKit.ADMIN.login(USER_ID);
            StpKit.ADMIN.getSession().set(TenantContext.CAMEL_NAME,
                    new SimpleTenantContext(5L, "租户五", "t5"));

            bindUser(user("ADMIN_USER"), () ->
                    recorder.record(model("106", "ORDER", "下单", false, null, null)));

            JSONObject extra = JSONUtil.parseObj(captureSaved().getBizExtra());
            Assertions.assertEquals(5, extra.getInt("activeTenantId"));
            Assertions.assertNull(extra.get("switched"));
        });
    }

    @Test
    void recordKeepsExtraWhenSessionMissingTenantContext() {
        SaTokenContextMockUtil.setMockContext(() -> {
            StpKit.ADMIN.login(USER_ID);

            bindUser(user("ADMIN_USER"), () ->
                    recorder.record(model("107", "ORDER", "下单", false, "{\"k\":\"v\"}", null)));

            // 会话中无生效租户上下文，保持原 extra
            Assertions.assertEquals("{\"k\":\"v\"}", captureSaved().getBizExtra());
        });
    }

    @Test
    void recordKeepsExtraWhenSessionUnavailable() {
        // 未登录（无 Mock 上下文）时 getSession 抛异常，捕获后保持原 extra
        bindUser(user("ADMIN_USER"), () ->
                recorder.record(model("108", "ORDER", "下单", false, "{\"k\":\"v\"}", null)));

        Assertions.assertEquals("{\"k\":\"v\"}", captureSaved().getBizExtra());
    }

    @Test
    void recordWrapsInvalidJsonExtraAsRawExtra() {
        SaTokenContextMockUtil.setMockContext(() -> {
            StpKit.ADMIN.login(USER_ID);
            StpKit.ADMIN.getSession().set(TenantContext.CAMEL_NAME,
                    new SimpleTenantContext(5L, "租户五", "t5"));

            bindUser(user("ADMIN_USER"), () ->
                    recorder.record(model("109", "ORDER", "下单", false, "{bad json", null)));

            JSONObject extra = JSONUtil.parseObj(captureSaved().getBizExtra());
            Assertions.assertEquals("{bad json", extra.getStr("rawExtra"));
            Assertions.assertEquals(5, extra.getInt("activeTenantId"));
        });
    }


    /*
    ----------------------------------------------------------------
                        私有方法
    ----------------------------------------------------------------
     */

    private SimpleUserContext user(String userTypeCode) {
        return new SimpleUserContext()
                .setUserId(USER_ID).setUserPin("u9").setUserTypeCode(userTypeCode);
    }

    /**
     * 绑定用户上下文后执行
     */
    private void bindUser(UserContext userContext, Runnable op) {
        where(UserContextHolder.scoped(), userContext).run(op);
    }

    private LogRecordModel model(String bizNo, String bizType, String behavior,
                                 boolean fail, String extra, String operator) {
        return LogRecordModel.builder()
                .bizNo(bizNo).bizType(bizType).behavior(behavior)
                .action("操作").operator(operator).fail(fail).extra(extra)
                .build();
    }

    private SysOperateLogCreateRequest captureSaved() {
        ArgumentCaptor<SysOperateLogCreateRequest> captor =
                ArgumentCaptor.forClass(SysOperateLogCreateRequest.class);
        Mockito.verify(sysOperateLogService, Mockito.atLeastOnce()).create(captor.capture());
        return captor.getValue();
    }
}
