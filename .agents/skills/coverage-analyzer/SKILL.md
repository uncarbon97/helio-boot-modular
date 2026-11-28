---
name: coverage-analyzer
description: 解析 JaCoCo XML 覆盖率报告，输出模块/类级缺口排名与下一轮补测建议；供 unit-test-reviewer 模式 B 选目标。触发：分析覆盖率报告、选补测目标、验收是否达标
---

# JaCoCo 覆盖率报告分析

## 背景

- 输入是**实测真值**（`modules/*/target/site/jacoco/jacoco.xml`），不是静态推演；报告不存在或过旧时直接说，不臆测。
- 定位：本 skill 只做「读报告 → 排缺口 → 给建议」；写测试归 unit-test-reviewer（模式 B），失败排查也归它。本 skill 是指挥官的侦察兵，不动手。
- 家规：不跑编译/测试/构建；只读已有报告。
- 默认达标口径：**类级分支 ≥ 80% 且行 ≥ 85%**（用户可覆盖）。全局收敛口径：全局分支 ≥ 80%。

## 分析步骤

1. **报告有效性**：`stat` 各 `modules/*/target/site/jacoco/jacoco.xml` 的 mtime；报告缺失的模块点名报告（无法分析，需先跑测试）；报告早于最近一次主源/测试源改动也提示可能过期。
   检查过期可用：`git log -1 --format=%ct -- <模块路径>` 对比 mtime。
2. **跑分析脚本**（固定口径，别手搓不同算法）：

```bash
python3 -I <仓库根>/.agents/skills/coverage-analyzer/scripts/analyze_jacoco.py [N]
```

（默认 N=10；脚本随 skill 自包含存放，仓库根由脚本自身路径上溯推导，与 cwd 无关）

脚本做：总览（INSTRUCTION/BRANCH/LINE/METHOD/CLASS）→ 模块排名（行+分支）→ 缺口类 top-N。
若脚本不存在或口径不符，按本节伪码现场实现到 `/tmp`（勿入库未约定的文件），逻辑必须与下述一致：

- 解析所有 `modules/*/target/site/jacoco/jacoco.xml`；
- 豁免类（计入总数但**不进** top-N 排名，报告中单独一行列出）：`**/config/**`、`*Application*`、类名以 `Configuration` 结尾（含不在 config 包的，如 `iplocation/Ip2regionConfiguration`）、`**/dal/entity/**`、`**/dal/mapper/**`、`**/constant/**`、`**/enums/**`（枚举按类名末尾 Enum 判）、`**/properties/**`、`**/model/**`（DTO/VO/请求响应体）；
- 注意 JaCoCo XML 里类名是 `/` 分隔，匹配豁免规则前先归一成 `.`（已踩坑）；
- top-N 排序键：`missed_line + missed_branch` 降序；只列 missed_line ≥ 5 的类；
- 每类输出：模块、类名、行 cov/total、分支 cov/total、missed 行区间概要（从 XML sourcefile/line 取，能取到就给行号段）。

3. **对比上轮**（若有）：上轮结果若在对话上下文中，对比目标类是否达标、全局数字变化；不在就跳过，别编。
4. **输出格式**（固定四段，缺哪段删哪段）：

```
== 总览 ==           （五计数器 + 环比箭头）
== 模块 ==           （行/分支表）
== 缺口 top-N ==      （类、行/分支、missed 行段）
== 建议 ==            （下一轮补测目标 2~3 个类，按"纯逻辑好测优先、豁免类剔除"排序；
                      明确写"交给 unit-test-reviewer 模式 B 补：<类名>"）
```

5. **收敛判断**：全局分支 ≥ 80%，或连续两轮 top 缺口全是豁免项 → 建议收尾：跑一次全量 + 全模块报告复验。

## 边界

- 不写/不改任何测试代码；不跑 maven。
- 达标判定只对用户指定的目标类或 top-N 候选做，不擅自全库逐类评判。
- 报告里的数字引用要能回指 XML（脚本输出为准），转述时别四舍五入到失真（百分比取整即可，分数保留）。
