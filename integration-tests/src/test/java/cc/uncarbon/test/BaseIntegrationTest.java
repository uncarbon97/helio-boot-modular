package cc.uncarbon.test;

import cc.uncarbon.framework.helium.base.context.SimpleUserContext;
import cc.uncarbon.framework.helium.base.context.UserContext;
import cc.uncarbon.framework.helium.tenant.context.SimpleTenantContext;
import cc.uncarbon.framework.helium.tenant.context.TenantContext;
import cc.uncarbon.framework.helium.web.context.VisitorContext;
import cc.uncarbon.module.Bootstrap;
import cc.uncarbon.module.context.ContextBinder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 集成测试基类。
 *
 * <p>Spring Boot 4 测试规范（易踩坑点）：
 * <p>- 需要 mock 时用 {@link org.springframework.test.context.bean.override.mockito.MockitoBean}（注意全限定名）
 *
 * @author Uncarbon
 */
@SpringBootTest(classes = Bootstrap.class)
@ActiveProfiles(value = "test")
public abstract class BaseIntegrationTest {

    /**
     * 在指定上下文的作用域内执行测试
     */
    protected void withContext(@Nullable UserContext user, @Nullable TenantContext tenant, @NonNull Runnable op) {
        ContextBinder.runWithContext(null, user, tenant, op);
    }

    /**
     * 在指定上下文的作用域内执行测试
     */
    protected void withContext(@Nullable UserContext user, @Nullable TenantContext tenant,
                               @Nullable VisitorContext visitor, @NonNull Runnable body) {
        ContextBinder.runWithContext(visitor, user, tenant, body);
    }

    /**
     * 构造简易测试用户上下文
     */
    protected UserContext testUser(@NonNull Long userId, @Nullable String userPin) {
        return new SimpleUserContext().setUserId(userId).setUserPin(userPin);
    }

    /**
     * 构造简易测试租户上下文
     */
    protected TenantContext testTenant(@NonNull Long tenantId, @Nullable String tenantName, @Nullable String tenantCode) {
        return new SimpleTenantContext(tenantId, tenantName, tenantCode);
    }
}
