package cc.uncarbon.module.adminapi.support.tenantswitch;

import cc.uncarbon.framework.helium.base.exception.BusinessException;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.module.adminapi.annotation.DenyIfTenantSwitching;
import cc.uncarbon.module.adminapi.errorcode.AdminApiErrorCodeEnum;
import cc.uncarbon.module.adminapi.model.internal.TenantSwitchInfo;
import cc.uncarbon.module.commons.satoken.StpKit;
import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;

/**
 * {@link DenyIfTenantSwitchingAspect} 租户切换阻断切面测试
 * <p>
 * 会话通过 {@link SaTokenContextMockUtil} Mock 上下文 + 内存版 Dao 模拟
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DenyIfTenantSwitchingAspectTest {

    private static final Long USER_ID = 9L;

    private final DenyIfTenantSwitchingAspect aspect = new DenyIfTenantSwitchingAspect();
    private final DenyIfTenantSwitching anno = Mockito.mock(DenyIfTenantSwitching.class);

    @AfterEach
    void tearDown() {
        SaTokenContextMockUtil.clearContext();
        StpKit.ADMIN.logout(USER_ID);
    }

    @Test
    void passesWhenNotSwitching() {
        SaTokenContextMockUtil.setMockContext(() -> {
            StpKit.ADMIN.login(USER_ID);
            // 会话无切换标记

            Assertions.assertDoesNotThrow(() -> aspect.before(anno));
        });
    }

    @Test
    void blocksWhenSwitching() {
        SaTokenContextMockUtil.setMockContext(() -> {
            StpKit.ADMIN.login(USER_ID);
            StpKit.ADMIN.getSession().set(TenantSwitchInfo.CAMEL_NAME,
                    new TenantSwitchInfo().setSwitchedAt(Instant.now()));

            var ex = Assertions.assertThrows(BusinessException.class, () -> aspect.before(anno));
            Assertions.assertEquals(AdminApiErrorCodeEnum.A04005, ex.getErrorCode());
        });
    }

    @Test
    void blocksWhenActiveTenantContextPresent() {
        // 切换切入后会话同时带生效租户上下文与切换标记
        SaTokenContextMockUtil.setMockContext(() -> {
            StpKit.ADMIN.login(USER_ID);
            var session = StpKit.ADMIN.getSession();
            session.set(TenantContext.CAMEL_NAME,
                    new SimpleTenantContext(5L, "租户五", "t5"));
            session.set(TenantSwitchInfo.CAMEL_NAME,
                    new TenantSwitchInfo().setSwitchedAt(Instant.now()));

            Assertions.assertThrows(BusinessException.class, () -> aspect.before(anno));
        });
    }
}
