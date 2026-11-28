---
name: integration-test-reviewer
description: 集成测试静态审查与新写：清理谓词污染共享库、@AfterEach 兜底、Excel 断言错列、种子数据依赖、withContext/runIgnored 语义、配置前提对齐、javadoc 陈旧。触发：审查现有 IT、新写 IT、IT 莫名失败/flaky 排查
---

# 集成测试静态审查与编写

## 背景

- 不跑编译/测试（家规），一切结论以主源码行号 + 种子 SQL 佐证为准；宁可少报不臆测。
- 模块 `integration-tests`：`@SpringBootTest(classes=Bootstrap)` 全量上下文 + 真 MySQL/Redis；`@ActiveProfiles("it")` 加载 application-it.yml（覆盖 dev/local，故数据源/Redis/文件存储/field-crypto 必须在该 yml 齐备）。
- 跑法：`mvn -Dskip-unit-tests=true -Prun-it verify`；failsafe 只跑 `src/test/java` 下 `*IT.java`（根 pom surefire skipTests=true 不影响）。按组：`-Dit.groups=tenant`。环境变量 `DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD/REDIS_HOST`。
- 基类 `BaseIntegrationTest`：`withContext(user, tenant, op)` 经 ContextBinder 绑上下文；`testUser/testTenant` 造简易上下文。需要 mock 用 `@MockitoBean`（Spring Boot 4，全限定名 `org.springframework.test.context.bean.override.mockito.MockitoBean`）。
- 多租户组合由环境变量切：`HELIUM_TENANT_ISOLATION_STRATEGY`（默认 LINE）、`HELIUM_TENANT_LOGIN_MODE`（默认 TENANT_FIRST）；DATASOURCE 策略需额外租户数据源路由。
- 单测 skill（unit-test-reviewer）清单中通用项照旧适用：幻影符号、构造器参数位、父类链式 setter 变宽、空测试、夹具撑起意图、boolean 默认值。本 skill 只列 IT 特有增量。

## 检查清单（IT 特有，按命中率排序，两模式通用）

1. **清理谓词宽度**：`@AfterEach`/尾部清理的 `likeRight(code,"it")` 之类前缀扫删，宽度必须 ≤ 造数前缀——过宽会误删共享库真实业务行（如 it 开头的真实租户/账号）。修法：造数用足窄前缀（ittc/itiso/itf/imp*），清理谓词同宽。
2. **清理兜底位置**：断言失败也要能清 → `@AfterEach`（null 守卫）+ 字段记录造出的 id/code；happy-path 尾部内联清理在首个断言失败时即漏清。
3. **Excel 断言错列**：导出断言用列序号 `cell(5)` 极脆——实体列增删后错位，且错位后取到相邻列值断言仍可能碰巧绿。修法：按表头名定位列（`ParsedSheet.columnIndexOf("操作内容")`，starters `ExcelIoUtil`）。
4. **种子数据依赖**：断言依赖的库内初始行（超管 id=0、SuperAdmin 角色、内置字典、初始菜单）须在 bootstrap/src/main/resources 初始化 SQL 里 grep 实存，含 id/编码/状态取值。
5. **withContext / runIgnored 语义**：断言的隔离行为要与实际绑定的上下文一致；跨租户清理/全局解析用 runIgnored（ScopedValue 全局作用域），且跨租户读用户表只取必要列（id）——异根密钥密文在测试库解密会抛异常中断清理。
6. **配置前提对齐**：用例假设须与 application-it.yml 逐项核对——field-crypto `data-key-kids` 数量 vs 密钥轮换用例需要、canary 关闭原因、隔离策略默认 LINE（承诺 DATASOURCE 跨库行为的用例在默认配置下走不到目标分支，javadoc 须注明"默认 LINE 下冒烟"）。
7. **javadoc/测试名陈旧**：引用已删的 SQL 路径（如 attachments/db/upgrade/...）、旧配置键（`helium.tenant.strategy` → 实际 `isolation-strategy`）、承诺超出实际行为的命名。
8. **共享库幂等/flaky**：断言"查询结果恰好 1 行/id 相等"依赖历史跑失败无残留（如平台态 `findByDigestSha256` 同 digest 残留元数据行）；优先每用例新建租户隔离造数，平台态断言用"包含/最新"语义。

## 模式 A：审查现有 IT

1. **机械预检**：复用 unit-test-reviewer 的 import 解析脚本（范围本模块 `*IT.java` + 协作类）。
2. **逐文件人审**：先读被测主类（endpoints/modules 实现）+ 种子 SQL + application-it.yml，再按上面清单 1→8 + 单测通用项过；每个疑点回读主源行佐证。
3. **业务符合性核对**：断言值（错误码、状态、导出列内容、树结构）逐条回读主源行；测试名/注释 vs 实际走到分支；IT 层分支缺口只报不擅自补。
4. **修复原则**：主源是基准，改测试保留原意图；改完清残留 import；清理逻辑改动须核对造数前缀同步收窄。
5. 输出清单：文件:行号、错误类型、修法一句话；附带范围外观察（主源问题只报不改）。

## 模式 B：新写 IT

1. **先读三样再动笔**：被测主类、种子 SQL（能用种子就不自造）、基建（BaseIntegrationTest、同包现有 IT 的造数/清理模式、ExcelIoUtil 等 starters 工具）。不读不写。
2. **列场景清单**：正常链路 + 负向断言（"仅列启用"类必须断言停用项被排除）+ 清理路径；断言期望值回读主源行。
3. **造数原则**：唯一窄前缀（nano time + 模块前缀）；新建租户优先于平台态造数；`@AfterEach` + runIgnored 兜底清理，跨租户只取 id。
4. **断言定位**：结构化产物按语义定位（表头名/字段名），不按物理序号。
5. **写完自查**：清单 1→8 + 单测通用项过一遍；输出测试文件路径 + 覆盖场景对照表 + 对共享库的读写足迹（建了什么、清了什么、前缀多宽）。

## 已验证的修复案例（2026-11 全量审查）

- 清理谓词过宽：`TenantCrudIT`（`it`→`ittc`，:137/:141/:211）、`TenantLineIsolationIT`（pin `it`→`itiso`）
- 内联清理无兜底：`AdminAuthIT`（租户/角色清理改 `@AfterEach` + 字段 + null 守卫）、`SysUserImportIT`（改 `@AfterEach` runIgnored 前缀扫删，只取 id）
- Excel 断言错列：`SysOperateLogExportIT:67`（`cell(5)` 实为「额外业务信息」列 → `columnIndexOf("操作内容")`）
- 负向断言缺失：`SysDeptIT:107`（名承诺"仅列启用"，补建停用部门并断言排除）
- javadoc 陈旧：`TenantLineIsolationIT:17`（引用已删 SQL 路径 + 旧配置键）、`TenantDataSourceIT`（补"默认 LINE 下冒烟"说明）
- flaky 观察（未改）：`FileUpDownloadIT` 平台态 digest 断言 id 相等，历史失败残留同 digest 行会挂
