---
name: errorcode-i18n-check
description: 扫描当前项目源码内所有 StructuredErrorCode 枚举，比对 errorcode 翻译文件，补齐缺失键并校验 YAML。触发：错误码翻译检查、新增错误码后、i18n 校验
---

# 错误码翻译完整性检查

## 背景

- 错误码枚举实现 `StructuredErrorCode`，错误码格式 `[A][BB][CCC]`：A=用户输入错、B=服务内部错。
- 翻译文件只有 `bootstrap/src/main/resources/i18n/errorcode/en-US.yml`（多文档 YAML，`---  # 枚举名` 分节）。
- **zh-CN 没有 errorcode yml 属设计**：`I18nAwareErrorMessageFormatter` 查不到翻译时兜底回枚举里的中文 `errorMsgFriendly`，勿补建。
- 枚举按内容定位（`implements StructuredErrorCode`），不依赖包名、目录层级与文件名；仅核对当前项目源码内的枚举（排除 target）。

## 步骤

1. 收集本仓库全部枚举码（内容定位，任意包名/层级通用）：

```bash
ROOT=$(git rev-parse --show-toplevel) && cd "$ROOT"
TMP=$(mktemp -d)
grep -rlE 'implements .*StructuredErrorCode' --include='*.java' --exclude-dir=target . > "$TMP/enum-sources.txt"
grep -hoE '^\s+[AB][0-9]{5}\(' $(cat "$TMP/enum-sources.txt") | tr -d ' (' | sort -u > "$TMP/enum.txt"
```

2. 收集 yml 已有键，双向比对（仅 A/B 码）：

```bash
grep -oE '^[AB][0-9]{5}' bootstrap/src/main/resources/i18n/errorcode/en-US.yml | sort -u > "$TMP/yml.txt"
comm -23 "$TMP/enum.txt" "$TMP/yml.txt"   # 枚举有、yml 缺 → 需补
comm -13 "$TMP/enum.txt" "$TMP/yml.txt"   # yml 有、枚举无 → 多余键，人工确认后删
```

3. 缺失键按枚举归入对应 `---  # 枚举名` 小节；英文文案参考枚举中文描述翻译，**保留 `{}` 填充占位符**。`OK("OK")` 类已英文的成功码可不补。

4. 校验（必须用 `safe_load_all`，文件是多文档）：

```bash
python3 -c "
import yaml
docs = list(yaml.safe_load_all(open('bootstrap/src/main/resources/i18n/errorcode/en-US.yml')))
print('docs=%d keys=%d' % (len(docs), sum(len(d) for d in docs if d)))"
```

5. 复跑第 2 步，确认差集为空。
