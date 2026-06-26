package cc.uncarbon.module.adminapi.aop;

import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.base.context.UserContextHolder;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContextHolder;
import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.framework.helium.web.context.VisitorContextHolder;
import cc.uncarbon.module.adminapi.annotation.SysOperateLog;
import cc.uncarbon.module.sys.enums.LogResultStatusEnum;
import cc.uncarbon.module.sys.model.request.SysOperateLogCreateRequest;
import cc.uncarbon.module.sys.service.SysOperateLogService;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ArrayUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.lang.ScopedValue;

/**
 * 系统操作日志切面
 * 配合 {@link SysOperateLog} 注解，将操作审计落库到 sys_operate_log 表
 *
 * @author Uncarbon
 */
@Aspect
@RequiredArgsConstructor
@Component
@Slf4j
public class SysOperateLogAspect {

    private static final String LOG_PREFIX = "[系统操作日志切面]";

    /**
     * 最大文本保存长度
     */
    private static final int MAX_STRING_SAVE_LENGTH = 3000;

    private final SysOperateLogService sysOperateLogService;
    private final ThreadPoolTaskExecutor taskExecutor;


    /**
     * 顺利运行
     */
    @AfterReturning(pointcut = "@annotation(annotation)", returning = "ret")
    public void returning(SysOperateLog annotation, Object ret) {
        if (!ArrayUtil.contains(annotation.when(), SysOperateLog.When.SUCCESS)) {
            // 操作日志保存时机不包含「成功时」
            return;
        }
        dispatch(annotation, null);
    }

    /**
     * 出现异常
     */
    @AfterThrowing(value = "@annotation(annotation)", throwing = "e")
    public void throwing(SysOperateLog annotation, Throwable e) {
        if (!ArrayUtil.contains(annotation.when(), SysOperateLog.When.FAILED)) {
            // 操作日志保存时机不包含「失败时」
            return;
        }
        dispatch(annotation, e);
    }

    /*
    ----------------------------------------------------------------
                        私有方法 private methods
    ----------------------------------------------------------------
     */

    /**
     * 分发：构造新增DTO，并按注解选择同步/异步保存
     */
    private void dispatch(SysOperateLog annotation, Throwable e) {
        VisitorContext visitorContext = VisitorContextHolder.getContext();
        if (visitorContext == null) {
            // 非HTTP请求上下文，跳过
            return;
        }
        // 上下文快照
        UserContext userContext = UserContextHolder.getContext();
        TenantContext tenantContext = TenantContextHolder.getContext();

        SysOperateLogCreateRequest request = buildCreateRequest(annotation, userContext, visitorContext, e);
        if (annotation.syncSave()) {
            // 同步保存
            saveSync(request);
        } else {
            // 异步保存：需在工作线程内回绑上下文，否则自动填充会丢失
            saveAsync(request, new AspectContext(userContext, tenantContext));
        }
    }

    /**
     * 同步保存系统操作日志
     */
    private void saveSync(SysOperateLogCreateRequest request) {
        try {
            sysOperateLogService.create(request);
        } catch (Exception e) {
            log.error(LOG_PREFIX + "同步保存系统操作日志失败 >>", e);
        }
    }

    /**
     * 异步保存系统操作日志
     */
    private void saveAsync(SysOperateLogCreateRequest dto, AspectContext ctx) {
        taskExecutor.submit((Runnable) () -> {
            try {
                // 在工作线程内重新建立 ScopedValue 作用域，使框架自动填充能够读到上下文
                ScopedValue.where(UserContextHolder.scoped(), ctx.getUserContext())
                        .where(TenantContextHolder.scoped(), ctx.getTenantContext())
                        .call(() -> {
                            sysOperateLogService.create(dto);
                            return null;
                        });
            } catch (Exception ex) {
                log.error(LOG_PREFIX + "异步保存系统操作日志失败 >>", ex);
            }
        });
    }

    /**
     * 构造新增
     */
    private static SysOperateLogCreateRequest buildCreateRequest(
            SysOperateLog annotation, UserContext userContext, VisitorContext visitorContext, Throwable e) {
        return new SysOperateLogCreateRequest()
                .setMainModule(annotation.mainModule())
                .setSubModule(annotation.subModule())
                .setBizNo(annotation.bizNo())
                .setBizExtra(annotation.bizExtra())
                // 记录操作内容
                .setOperation(annotation.operation())
                // 记录操作人
                .setUserId(userContext != null ? userContext.getUserId() : null)
                .setUserTypeCode(userContext != null ? userContext.getUserTypeCode() : null)
                // 记录HTTP请求方法与路径
                .setRequestMethod(visitorContext.getHttpRequestMethod())
                .setRequestPath(visitorContext.getHttpRequestPath())
                // 记录访客信息
                .setVisitorIp(visitorContext.getIp())
                .setVisitorUserAgent(visitorContext.getUserAgent())
                // 默认置为成功；异常时置为失败并记录失败原因
                .setResultStatus(e == null ? LogResultStatusEnum.SUCCESS : LogResultStatusEnum.FAILED)
                .setFailedMsg(e == null ? null
                        : CharSequenceUtil.subPre(ExceptionUtil.getMessage(e), MAX_STRING_SAVE_LENGTH));
    }

    /**
     * 本切面上下文快照，用于异步线程回绑
     * 内部使用
     */
    @Getter
    @RequiredArgsConstructor
    private static final class AspectContext {

        /**
         * 包装当前用户态，可能为null
         */
        private final UserContext userContext;

        /**
         * 租户上下文，可能为null
         */
        private final TenantContext tenantContext;
    }

}
