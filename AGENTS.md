@.agents/AGENTS.md

# 编程规约
## 上下文
- 用户态、租户态等上下文，遵循虚拟线程`ScopedValue`规范传递，并统一从`UserContextHolder`、`TenantContextHolder`等API中读写

## 代码格式
- `private`、`protected`方法，放置在当前类`私有方法`块注释下方

# 多租户
- 规划修改时，需要同时适配`LINE`行级多租户与`DATASOURCE`数据源级多租户，以及`TENANT_FIRST`租户优先模式与`USER_FIRST`用户优先模式
