package cc.uncarbon.module.adminapi.annotation;

import java.lang.annotation.*;


/**
 * 放在 Controller 方法上，校验当前用户未处于「租户切换中」状态，处于切换态时直接阻断请求
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DenyIfTenantSwitching {
}
