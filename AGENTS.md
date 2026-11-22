# 编程规约
## 上下文
- 用户态、租户态等上下文，遵循虚拟线程`ScopedValue`规范传递，并统一从`UserContextHolder`、`TenantContextHolder`等API中读写

## 代码格式
- 新增的`public`方法，如果没有特别说明，均放置在当前类最后一个`public`方法之后
- 新增的`private`/`protected`方法，如果没有特别说明，均放置在当前类最后一个`private`方法之后
- lombok `@Accessors(chain=true)` 的 setter 返回**声明类**类型（无协变覆盖）：request 多层继承时，子类字段须放在链首/单独赋值，父类字段链式收尾；跨层整链的末端静态类型是父类，不能直接塞回子类容器

## 接口
- 所有 HTTP 接口统一使用 `POST` 方法（`@PostMapping`），不使用 `GET`/`PUT`/`DELETE`；浏览器直链例外，允许 `GET`

## 数据访问
- 数据库写操作（`delete`/`update`）禁止在 service 层直接向 Mapper 传 `Wrapper` 裸调用，必须封装为对应 Mapper 中的具名 `default` 方法；裸读（`select*`/`exists`）不受此限
- 建表/加列中的时刻字段：MySQL `TIMESTAMP` 有 2038-01-19 上限，存未来时刻（如过期、预约）的列用 `DATETIME`（不携带时区，应用层手动完成时区转换）；PostgreSQL 对应 `timestamptz`，无 2038 问题

## 错误码
- 格式 `[A|B|Z][BB][CCC]`：A=用户输入类，B=系统/业务故障类，Z=框架层
- `[BB]` 分段：00-89 归本工程（注册于 `modules/commons` 的 `ErrorCodeBizGroup`）
- 新增错误码时，必须同步增补 `bootstrap/src/main/resources/i18n/errorcode/en-US.yml` 英文词条：key 即错误码字符串，占位符 `{}`，按 `---  # 枚举名` 分文档归类；中文无文件、默认取枚举文案；业务提示文案在同级 `bizmessage/` 目录

# 多租户
- 规划修改时，需要同时适配`LINE`行级多租户与`DATASOURCE`数据源级多租户，以及`TENANT_FIRST`租户优先模式与`USER_FIRST`用户优先模式

# 框架层
- Excel 能力在 `helium-starter-excel`，包根 `cc.uncarbon.framework.helium.excel`（2026-11-18 由 `...fesod` 包改名迁移）；ExcelRow 注解驱动导入导出的用法见 `.claude/2026-11-15 用户导入+审计日志导出/实现方案v2-2026-11-18.MD`

# 经验沉淀
- 凡花费多轮摸索才定位到的文件路径、规约、陷阱，将结论以一行条目增补进本文对应小节；无对应小节则新建 `##` 小节，不另开新文件
- 只沉淀稳定结论（在哪、是什么、怎么写），不复述一次性排查过程；增补前先查重，与既有条目相邻或重叠时合并改写
