---
name: entity-ddl-sync-check
description: 扫描 *Entity 字段（含基类隐含列），与 attachments/db 下 MySQL/PostgreSQL init.sql 逐表逐列双向比对，确认无遗漏 DDL 变更。触发：实体变更后、DDL 变更后、发布前核对
---

# 实体与 DDL 同步检查

## 背景

- 实体按内容定位（含 `@TableName` 注解的类），不依赖包名、目录层级与文件名；`@TableName` 定表名、`@TableField(value=...)` 定列名；`@TableField(exist = false)` 字段不落库，排除。
- 基类（`Abstract*Entity`）隐含列已固化如下，本 skill 不依赖 starters 源码；若框架基类定义有变，先复核再更新此表：
  - `AbstractGenericEntity`：del_flag, created_at, created_by, updated_at, updated_by
  - `AbstractTenantGenericEntity extends ↑`：+ tenant_id
  - `AbstractRelationEntity`：仅 created_at, created_by（updatedAt/updatedBy 为 exist=false）
  - `AbstractTenantRelationEntity extends ↑`：+ tenant_id
- 忽略无实体的表：`#业务表模板`、`#关联表模板`、`helium_field_crypto_canary`（框架自管）。
- `line-tenant-tune.sql` 只调索引不改列，不参与列比对。

## 前置

```bash
ROOT=$(git rev-parse --show-toplevel) && cd "$ROOT"
TMP=$(mktemp -d)
```

## 步骤

1. 提取实体侧字段（内容定位，任意包名/层级通用）：

```bash
grep -rl '@TableName' --include='*.java' --exclude-dir=target . > "$TMP/entity-sources.txt"
grep -H -E "TableName|class .* extends|private [A-Za-z<>, ]+ [a-zA-Z]+;" $(cat "$TMP/entity-sources.txt")
```

   按 extends 关系叠加上表基类列；camelCase 字段按 MyBatis-Plus 惯例或 `@TableField` 值映射为 snake_case。

2. 提取 DDL 列（两方言各跑一次）：

```bash
awk '/^CREATE TABLE/{gsub(/`/,""); tbl=$3; print "TABLE " tbl; intbl=1; next}
     intbl && (/^\) ENGINE/ || /^\);/){intbl=0; next}
     intbl && (/^  `/ || /^  [a-zA-Z_]/){gsub(/`/,""); print "  " $1}' \
  attachments/db/MySQL/init.sql   # PostgreSQL/init.sql 同理
```

3. 三向比对，均须为零差集：
   - 每个实体「自有字段+基类列」↔ 该表 DDL 列（双向）
   - MySQL ↔ PostgreSQL 同名列集合（双向）
4. 有差集时：实体新增字段 → 补两份 init.sql（列+注释+必要索引）；只补单方言 → 同步另一份。模板表不动。

## 已知合法偏差（勿报为缺漏）

- `sys_login_log`、`sys_operate_log` 无 updated_at/updated_by 列（实体里 exist=false，日志禁更新）。
- 关联表无 del_flag；`sys_menu`、`tenant_meta`、`tenant_package`、`tenant_package_menu_relation` 无 tenant_id（全局表/租户本体/套餐全租户共享）。
