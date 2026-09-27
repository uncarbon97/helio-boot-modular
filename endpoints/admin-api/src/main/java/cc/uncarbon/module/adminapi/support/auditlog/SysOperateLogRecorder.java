package cc.uncarbon.module.adminapi.support.auditlog;

import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.bizlog.model.LogRecordModel;
import cc.uncarbon.framework.helium.bizlog.service.ILogRecordDataService;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.framework.helium.web.context.VisitorContextHolder;
import cc.uncarbon.module.adminapi.model.internal.TenantSwitchInfo;
import cc.uncarbon.module.commons.enums.UserTypeCodeEnum;
import cc.uncarbon.module.commons.satoken.StpKit;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import cc.uncarbon.module.sys.model.request.SysOperateLogCreateRequest;
import cc.uncarbon.module.sys.service.SysOperateLogService;
import cn.dev33.satoken.session.SaSession;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务日志落库适配器
 * <p>
 * 将 bizlog 框架渲染出的 {@link LogRecordModel} 转换并委托 {@link SysOperateLogService} 落库；
 * 操作人、用户信息取自当前线程的上下文，请求信息取自访客上下文。
 *
 * @author Uncarbon
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class SysOperateLogRecorder implements ILogRecordDataService {

    private final SysOperateLogService sysOperateLogService;

    @Override
    public void record(LogRecordModel model) {
        try {
            sysOperateLogService.create(convert(model));
        } catch (Exception e) {
            // 落库失败不影响业务主流程
            log.error("[SysOperateLog] 保存操作日志失败, bizNo={}, bizType={}", model.getBizNo(), model.getBizType(), e);
        }
    }

    @Override
    public List<LogRecordModel> queryLog(String bizNo, String bizType) {
        // 当前实现不负责查询
        return List.of();
    }

    @Override
    public List<LogRecordModel> queryLogByBizNo(String bizNo, String bizType, String behavior) {
        // 当前实现不负责查询
        return List.of();
    }


    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 业务日志模型 -> 新增请求
     */
    private SysOperateLogCreateRequest convert(LogRecordModel model) {
        var request = new SysOperateLogCreateRequest()
                .setBizType(model.getBizType())
                .setBehavior(model.getBehavior())
                .setBizNo(model.getBizNo())
                .setOperation(model.getAction())
                .setBizExtra(model.getExtra())
                .setResultStatus(model.isFail() ? LogResultStatusEnum.FAILED : LogResultStatusEnum.SUCCESS);

        // 操作人：优先取框架解析出的 operatorId，取不到再回退当前登录用户
        UserContext userContext = UserContextHolder.getContext();
        Long userId = parseOperatorId(model.getOperator());
        if (userId == null && userContext != null) {
            userId = userContext.getUserId();
        }
        request.setUserId(userId);
        if (userContext != null) {
            request.setUserTypeCode(UserTypeCodeEnum.ofName(userContext.getUserTypeCode()));
        }

        // 请求信息取自访客上下文（由 ContextBindingFilter 绑定）
        VisitorContext visitorContext = VisitorContextHolder.getContext();
        if (visitorContext != null) {
            request.setRequestMethod(visitorContext.getHttpRequestMethod())
                    .setRequestPath(visitorContext.getHttpRequestPath())
                    .setVisitorIp(visitorContext.getClientIp())
                    .setVisitorUserAgent(visitorContext.getUserAgent());
        }

        enrichTenantSwitchTrace(request, model.getExtra());
        return request;
    }

    /**
     * 跨租户代操作留痕：管理用户切入其他租户视角后的操作，结构化补充活动/原始租户标记；
     * 日志行本身只落活动租户的 tenant_id，无此标记则平台侧无从追溯「谁代操作了哪个租户」
     */
    private void enrichTenantSwitchTrace(SysOperateLogCreateRequest request, String rawExtra) {
        if (request.getUserTypeCode() != UserTypeCodeEnum.ADMIN_USER) {
            return;
        }
        try {
            SaSession session = StpKit.ADMIN.getSession();
            if (!(session.get(TenantContext.CAMEL_NAME) instanceof TenantContext active)) {
                return;
            }
            JSONObject extra = buildExtraJson(rawExtra);
            extra.set("activeTenantId", active.getTenantId());
            if (session.get(TenantSwitchInfo.CAMEL_NAME) instanceof TenantSwitchInfo info) {
                extra.set("switched", true);
                if (info.getOriginalTenantContext() != null) {
                    extra.set("originalTenantId", info.getOriginalTenantContext().getTenantId());
                }
            }
            request.setBizExtra(extra.toString());
        } catch (Exception e) {
            // 会话不可用（无登录态等）时保持原 extra，不影响落库主流程
            log.debug("[SysOperateLog] 补充租户切换痕迹跳过, bizNo={}", request.getBizNo(), e);
        }
    }

    /**
     * 注解 extra 模板 -> JSON 对象；非合法 JSON 时包一层 rawExtra，避免原值丢失
     */
    private JSONObject buildExtraJson(String rawExtra) {
        if (CharSequenceUtil.isBlank(rawExtra)) {
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(rawExtra);
        } catch (Exception e) {
            JSONObject ret = new JSONObject();
            ret.set("rawExtra", rawExtra);
            return ret;
        }
    }

    /**
     * 操作人ID文本 -> 数值，解析失败返回 null
     */
    private Long parseOperatorId(String operator) {
        if (operator == null || operator.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(operator.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
