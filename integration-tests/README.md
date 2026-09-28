# integration-tests | 集成测试

前置：外部 MySQL / Redis 已启动，连接信息见 `src/test/resources/application-test.yml`；
数据库已执行建表脚本与 `attachments/db/upgrade/4.0.0-tenant-login-mode.sql`。

## 大模块分类

集成测试按 JUnit5 `@Tag` 分类，便于分模块单独启动：

| Tag | 覆盖范围 | 测试类 |
|---|---|---|
| `sys` | 用户/角色/部门/菜单/字典/日志 | `cc.uncarbon.test.sys.*` |
| `auth` | 登录、登录日志、登录失败限制 | `cc.uncarbon.test.auth.*` |
| `tenant` | 租户/套餐/切换/行级隔离/对账 | `cc.uncarbon.test.tenant.*`、`cc.uncarbon.test.tenant.TenantLineIsolationIT`、`cc.uncarbon.test.tenant.TenantDataSourceIT` |
| `file` | 文件存储点/上传下载/秒传 | `cc.uncarbon.test.file.*` |

## 启动方式

```bash
# 全部集成测试
mvn -pl integration-tests -am -Prun-it verify

# 只跑某个大模块（-Dit.groups=sys|auth|tenant|file）
mvn -pl integration-tests -am -Prun-it verify -Dit.groups=tenant

# 数据库/Redis 连接信息覆盖
MASTER_DB_HOST=192.168.1.10 MASTER_DB_NAME=helium_test MASTER_DB_USERNAME=helium MASTER_DB_PASSWORD=helium \
REDIS_HOST=192.168.1.10 \
mvn -pl integration-tests -am -Prun-it verify

# 多租户四组合矩阵（隔离策略 × 登录模式）
HELIUM_TENANT_ISOLATION_STRATEGY=LINE       HELIUM_TENANT_LOGIN_MODE=TENANT_FIRST mvn ... -Prun-it verify
HELIUM_TENANT_ISOLATION_STRATEGY=LINE       HELIUM_TENANT_LOGIN_MODE=USER_FIRST   mvn ... -Prun-it verify
# DATASOURCE 策略需要先在 application-test.yml 配置租户数据源路由，再切换
HELIUM_TENANT_ISOLATION_STRATEGY=DATASOURCE HELIUM_TENANT_LOGIN_MODE=TENANT_FIRST mvn ... -Prun-it verify
```

各业务模块内的单元测试默认跳过（与既有构建行为一致），单独执行：

```bash
mvn test -Dskip-unit-tests=false
mvn test -Dskip-unit-tests=false -pl modules/sys-impl -am
```

## 约定

- 测试数据统一使用 `it` 前缀（账号/编码/名称），`@AfterEach` 清理，避免污染库
- 服务层直调（不起 HTTP），上下文通过 `BaseIntegrationTest.withContext` 绑定
- 断言错误码时比较 `BusinessException#getErrorCode()` 与枚举常量
