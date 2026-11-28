package cc.uncarbon.module.adminapi.support.tenantswitch;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.module.adminapi.annotation.DenyIfTenantSwitching;
import cc.uncarbon.module.adminapi.errorcode.AdminApiErrorCodeEnum;
import cc.uncarbon.module.adminapi.helper.TenantSwitchHelper;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;


/**
 * {@link DenyIfTenantSwitching} 注解切面
 * 会话中存在租户切换标记即视为切换中，直接阻断
 *
 * @author Uncarbon
 */
@Aspect
@Component
public class DenyIfTenantSwitchingAspect {

    @Before(value = "@annotation(anno)")
    public void before(DenyIfTenantSwitching anno) {
        if (TenantSwitchHelper.isSwitching()) {
            throw new BusinessException(AdminApiErrorCodeEnum.A04005);
        }
    }

}
