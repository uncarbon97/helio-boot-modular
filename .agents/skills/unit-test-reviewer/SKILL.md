---
name: unit-test-reviewer
description: 单元测试静态审查与新写：Lombok 构造器/链式 setter、Mockito default 方法、幻影常量、空测试、断言业务符合性。触发：审查现有 UT、新写 UT、新写后自查、测试编译/断言莫名失败排查
---

# 单元测试静态审查与编写

## 背景

- 不跑编译/测试（家规），一切结论以主源码行号佐证为准；宁可少报不臆测。
- 本项目栈：JUnit 5 + Mockito + Lombok + MyBatis-Plus，多模块 Maven；上游框架（helium-starters）直读源码、不反编译 jar。其仓库根目录**不写死在本 skill**，由开发者本人 CLAUDE.md 注入，下文记作 `$UPSTREAM`（本 skill 内出现 `$UPSTREAM` 处，执行时以注入路径代入；未注入则跳过对应步骤并单独标注"无法核验"）。
- 高频出错集中在「Lombok 生成物」「Mockito mock 语义」「父类实体链式 setter」三处，见下面清单。

## 检查清单（按命中率排序，两模式通用）

1. **幻影常量/方法**：测试引用的错误码、枚举名、方法必须先在主源 grep 确认存在。
   典型：引用不存在的 `A01053`（枚举实际最大 `A01052`）；或断言的行为在被测实现里根本没有（如 request 上的 `@Size` 校验在 controller 层生效，service 单测直调不触发 → assertThrows 必失败）。
2. **父类实体链式 setter 类型变宽**：`@Data @Accessors(chain=true)` 的父类（如 `AbstractTenantGenericEntity.setTenantId`、`AbstractGenericEntity` 的审计字段 setter）链式返回**父类型**；链中混入子类 setter → 编译错（cannot find symbol）或 `thenReturn` 参数类型不匹配。
   修法：子类 setter 链完先得到子类型，父类 setter（`setTenantId` 等）用单独语句调用。任何「实体链中途变宽」的编译错先查这层继承。
3. **Lombok `@AllArgsConstructor` 参数位**：参数个数=字段数、顺序=字段声明顺序。测试传参逐位核对，字段增改后旧测试常缺位。
   典型：字段扩到 4 个后测试仍传 3 参 → 编译错误；补位时注意字符串落在哪个字段，否则 equals 断言错位失败。
   注意：类若改用 `@Builder` 或手写构造器，此结论需按实际生成物重核。
4. **Mockito 拦截接口 default 方法**：`@Mock` 的接口 mock 对 default 方法同样拦截、不执行方法体；给 default 内部委托的方法（如 `selectList`）打桩无效。
   修法：直接打桩 default 方法本身（如 `selectPins(any())`），注意返回类型是 default 方法声明的类型（常是 `List<String>` 而非实体列表）。
5. **原始 boolean 默认 false**：测试名/断言若暗示"默认开启"（如 `EnabledByDefault`），先核对字段是 `private boolean x;`（默认 false）还是 `= true` 初始化；`if (attr.isX())` 分支不成立 → verify 次数为 0 → 失败。
   修法：显式 `.setX(true)` 并改测试名；若产品意图确是默认开启，改主源初始化器（需用户确认）。
6. **夹具数据要撑起测试意图**：被测链路新增校验（如 magic-number 文件头校验）后，"好文件"夹具也必须合法，否则第一个文件就失败，短路/分支测试全部失去意义且断言碰巧仍绿。
7. **空测试**：方法体只有打桩、没有任何被测调用与断言 → 恒绿零覆盖（LENIENT 下连 UnnecessaryStubbing 都不报）。确认是被测类不再使用的残留后删除，删完清未用 import/helper。
8. **同根因连带**（启发式，非不变式）：单元测试的语义错误常在同主题 IT 里复现（如 IT 也误以为默认 true），修复单测时顺手核对同主题 IT。

## 模式 A：审查现有 UT

1. **机械预检**（先跑，排除低级错误再人审）：

```bash
# 所有 cc.uncarbon import 是否解析到真实源文件（git 索引取文件，模块布局无关；含嵌套类裁剪、starters）
# UPSTREAM=CLAUDE.md 注入的上游仓库根目录；未注入则置空串，自动跳过 starters 扫描
UPSTREAM='<$UPSTREAM-按CLAUDE.md注入>' python3 - <<'EOF'
import os, re, subprocess
files = set()
for p in subprocess.run(["git", "ls-files", "*.java"], capture_output=True, text=True).stdout.split():
    m = re.search(r"src/(?:main|test)/java/(.+)\.java$", p)
    if m:
        files.add(m.group(1).replace("/", "."))
upstream = os.environ.get("UPSTREAM", "").strip()
if upstream:
    for p in subprocess.run(["git", "-C", upstream, "ls-files", "*.java"],
                            capture_output=True, text=True).stdout.split():
        m = re.search(r"src/main/java/(.+)\.java$", p)
        if m:
            files.add(m.group(1).replace("/", "."))
imp = re.compile(r'^import\s+(?:static\s+)?([\w.]+)\s*;', re.M)
for p in [x for x in subprocess.run(["git", "ls-files", "*src/test*"], capture_output=True,
                                    text=True).stdout.split() if x.endswith("Test.java")]:
    for i in imp.findall(open(p, encoding="utf-8").read()):
        if not i.startswith("cc.uncarbon"):
            continue
        parts = i.split(".")
        cands = [".".join(parts[:k]) for k in range(len(parts), 0, -1) if parts[k-1][0].isupper()]
        if not any(c in files for c in cands):
            print(p, "->", i)
EOF
```

   覆盖仓库内全部模块（含 bootstrap、integration-tests、endpoints）。命中的先判真伪（外部依赖/同包免 import 的不算），再修引用。
2. **逐文件人审**：先读被测主类（含 Lombok 注解、继承链、mapper 接口 default 方法），再按上面清单 1→7 过测试；每个疑点回读主源对应行佐证。
3. **业务符合性核对**（区别于机械错误）：
   - 断言值 vs 主源实际产出：逐断言回读主源行，确认期望值是主源真实会产出的（不是想当然的"应该"值）。
   - 测试名/注释 vs 实际行为：名字说"租户隔离"，链路真的走到 tenant 过滤分支吗。
   - 分支覆盖缺口：主源 if/短路/异常分支列表 vs 现有用例，列出无覆盖分支（只报，不擅自补写，除非任务要求）。
   - 夹具取值是否贴近真实业务域（错误码、状态枚举用生产会出现的值，不用占位符语义的值）。
4. **修复原则**：主源码是行为基准；测试与主源冲突时改测试但保留原意图（显式传参/改名），不臆造主源没有的行为；改动后检查残留 import 与死桩。
5. 全部修完输出清单：文件:行号、错误类型、修法一句话。

## 模式 B：新写 UT

1. **先读主源再动笔**：被测类全读 + 协作对象（mapper default 方法、Lombok 注解、实体继承链到 starters 基类）。不读主源不写测试。
2. **列分支清单**：从主源枚举 if / 短路 / 异常 / 循环边界分支，每分支至少一个用例；清单先写出来再落代码，断言期望值全部回读主源行得出。
3. **夹具先行核对**：夹具必须过被测链路全部前置校验（清单 6）；mock 返回类型对齐实际声明类型（清单 4）。
4. **落码风格**：沿用同模块现有测试的模式（`MybatisPlusTestSupport`、`@MockitoSettings(LENIENT)`、私有 stub helper）；实体构造遵循清单 2 的父类 setter 规则。
5. **写完按清单 1→7 自查**一遍，再输出：测试文件路径 + 覆盖分支对照表。

## 已验证的修复案例

- 2026-11 全量审查：构造器 3 参 vs 4 字段（`AdminSysUserListQueryTest`）；default 方法 `selectPins` 打桩落空 ×3（`SysUserServiceImplImportTest`）；`autoEnable` 默认 false vs `EnabledByDefault`（单测 + IT 连带）；magic-number 校验使空内容 jpg 自炸（`UploadFileCheckerTest`）；幻影 `A01053` + 服务层无该校验（`SysUserServiceImplImportTest`）；空测试 `isSuperAdminBranches`（`TenantUserRoleFacadeImplTest`）
- 父类链式 setter 变宽：`FileMetaServiceImplTest` / `DynamicFileStorageRegistrarTest` 的 `setTenantId` 链（thenReturn 父类型、setId cannot find symbol 两种形态）
