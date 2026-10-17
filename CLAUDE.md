@AGENTS.md
@.agents/AGENTS.md

# 编码规范
## 代码风格
- `private`、`protected`方法，放置在当前类`私有方法`块注释下方

# 多租户适配
- 规划修改时，需要同时适配`LINE`行级多租户与`DATASOURCE`数据源级多租户，以及`TENANT_FIRST`租户优先模式与`USER_FIRST`用户优先模式
