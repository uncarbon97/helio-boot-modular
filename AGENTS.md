@.agents/AGENTS.md

# 编程规约
## 上下文
- 用户态、租户态等上下文，遵循虚拟线程`ScopedValue`规范传递，并统一从`UserContextHolder`、`TenantContextHolder`等API中读写

## 代码格式
- 新增的`public`方法，如果没有特别说明，均放置在当前类最后一个`public`方法之后
- 新增的`private`/`protected`方法，如果没有特别说明，均放置在当前类最后一个`private`方法之后

## 数据访问
- 数据库写操作（`delete`/`update`）禁止在 service 层直接向 Mapper 传 `Wrapper` 裸调用，必须封装为对应 Mapper 中的具名 `default` 方法；裸读（`select*`/`exists`）不受此限

# 多租户
- 规划修改时，需要同时适配`LINE`行级多租户与`DATASOURCE`数据源级多租户，以及`TENANT_FIRST`租户优先模式与`USER_FIRST`用户优先模式
