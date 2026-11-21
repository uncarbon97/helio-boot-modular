#!/usr/bin/env python3
"""JaCoCo 覆盖率报告分析（coverage-analyzer skill 固定口径）。

解析 modules/*/target/site/jacoco/jacoco.xml，输出总览、模块排名、缺口类 top-N。
纯读取，不跑构建。用法: python3 -I scripts/analyze_jacoco.py [N]   (默认 N=10)
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET

# 本脚本位于 <仓库根>/.agents/skills/coverage-analyzer/scripts/analyze_jacoco.py
# 上溯 4 级：scripts → coverage-analyzer → skills → .agents → 仓库根
ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), *([os.pardir] * 4)))

EXEMPT_SUBSTR = (
    ".config.", "Application", ".dal.entity", ".dal.mapper",
    ".constant", ".enums", ".properties", ".model.",
)


def is_exempt(class_fqn: str, source_file: str) -> bool:
    # JaCoCo 的类名用 "/" 分隔，统一成 "." 再匹配
    fqn = class_fqn.replace("/", ".")
    if any(s in fqn for s in EXEMPT_SUBSTR):
        return True
    # enums 目录外的枚举类：类名或源文件名以 Enum 结尾
    if fqn.rsplit(".", 1)[-1].endswith("Enum") or source_file.endswith("Enum.java"):
        return True
    # Spring 配置类（不在 config 包的，如 iplocation/Ip2regionConfiguration）
    return fqn.rsplit(".", 1)[-1].endswith("Configuration") or source_file.endswith("Configuration.java")


def counters(el) -> dict:
    return {c.get("type"): (int(c.get("missed")), int(c.get("covered")))
            for c in el.findall("counter")}


def main(n: int) -> int:
    files = sorted(glob.glob(os.path.join(ROOT, "modules", "*", "target", "site", "jacoco", "jacoco.xml")))
    if not files:
        print("未找到任何 jacoco.xml（先跑单测并输出报告）")
        return 1

    total, per_mod, classes, exempt = {}, {}, [], []
    for f in files:
        mod = f.split(os.sep + "modules" + os.sep)[1].split(os.sep)[0]
        root = ET.parse(f).getroot()
        for pkg in root.iter("package"):
            # 类 -> 源文件行覆盖（拿 missed 行段）
            lines_by_src = {}
            for sf in pkg.findall("sourcefile"):
                seg, cur = [], None
                for ln in sf.findall("line"):
                    if ln.get("mi") != "0":  # missed instructions
                        cur = int(ln.get("nr"))
                        seg.append(cur)
                    # 只收集连续段由后面压缩
                lines_by_src[sf.get("name")] = seg
            for cls in pkg.findall("class"):
                c = counters(cls)
                src = cls.get("sourcefilename") or ""
                classes_row = (mod, cls.get("name"), src, c, lines_by_src.get(src, []))
                (exempt if is_exempt(cls.get("name"), src) else classes).append(classes_row)
                mod_d = per_mod.setdefault(mod, {})
                for k, v in c.items():
                    a, b = mod_d.get(k, (0, 0))
                    mod_d[k] = (a + v[0], b + v[1])
                for k, v in c.items():
                    a, b = total.get(k, (0, 0))
                    total[k] = (a + v[0], b + v[1])

    def pct(cov, tot):
        return f"{cov * 100 // max(tot, 1)}%"

    print("== 总览 ==")
    for k in ("INSTRUCTION", "BRANCH", "LINE", "METHOD", "CLASS"):
        m, cv = total.get(k, (0, 0))
        print(f"  {k}: {cv}/{m + cv} = {pct(cv, m + cv)} (missed {m})")

    print("\n== 模块 ==")
    for mod, d in sorted(per_mod.items()):
        lm, lc = d.get("LINE", (0, 0))
        bm, bc = d.get("BRANCH", (0, 0))
        print(f"  {mod}: line {pct(lc, lm + lc)} ({lc}/{lm + lc}), "
              f"branch {pct(bc, bm + bc)} ({bc}/{bm + bc})")

    def compress(nums, cap=6):
        """连续行号压成段，最多 cap 段"""
        segs, s, p = [], None, None
        for x in nums:
            if s is None:
                s = p = x
            elif x == p + 1:
                p = x
            else:
                segs.append((s, p))
                s = p = x
        if s is not None:
            segs.append((s, p))
        out = [f"{a}" if a == b else f"{a}-{b}" for a, b in segs]
        return ",".join(out[:cap]) + ("…" if len(out) > cap else "")

    ranked = sorted(
        ((c.get("LINE", (0, 0))[0] + c.get("BRANCH", (0, 0))[0], mod, fqn.rsplit("/", 1)[-1], c, segs)
         for mod, fqn, src, c, segs in classes if c.get("LINE", (0, 0))[0] >= 5),
        reverse=True,
    )[:n]

    print(f"\n== 缺口 top-{len(ranked)}（豁免类不计入） ==")
    for score, mod, name, c, segs in ranked:
        lm, lc = c.get("LINE", (0, 0))
        bm, bc = c.get("BRANCH", (0, 0))
        br = f", br {bc}/{bc + bm}" if bc + bm else ""
        seg = f"  missed行段[{compress(segs)}]" if segs else ""
        print(f"  {mod}: {name}  line {lc}/{lc + lm}{br}{seg}")

    if exempt:
        print(f"\n== 豁免（计入总数，不参与排名，共 {len(exempt)} 类） ==")
        names = sorted({f"{m}:{f.rsplit('/', 1)[-1]}" for m, f, s, c, segs in exempt
                        if c.get("LINE", (0, 0))[0] >= 5})
        for x in names[:15]:
            print(f"  {x}")
        if len(names) > 15:
            print(f"  …共 {len(names)} 个")

    missing = [d for d in sorted(os.listdir(os.path.join(ROOT, "modules")))
               if os.path.isdir(os.path.join(ROOT, "modules", d))
               and not glob.glob(os.path.join(ROOT, "modules", d, "target", "site", "jacoco", "jacoco.xml"))]
    if missing:
        print(f"\n!! 以下模块无报告（需先跑测试）: {', '.join(missing)}")
    return 0


if __name__ == "__main__":
    sys.exit(main(int(sys.argv[1]) if len(sys.argv) > 1 else 10))
