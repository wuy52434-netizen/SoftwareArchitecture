#!/usr/bin/env python3
"""聚合各 Maven 模块的 JaCoCo 报告，输出项目整体覆盖率。

用法：
    python test/tools/coverage_summary.py            # 打印汇总表
    python test/tools/coverage_summary.py --min 0.30 # 整体行覆盖率低于 30% 时退出码 1

数据来源：backend/<module>/target/site/jacoco/jacoco.csv（由 mvn verify 生成）。
"""
from __future__ import annotations

import argparse
import csv
import sys
from pathlib import Path

BACKEND = Path(__file__).resolve().parents[2] / "backend"


def read_module(csv_path: Path):
    """读取单个模块的 jacoco.csv，返回 (line_covered, line_missed, branch_covered, branch_missed)。"""
    line_c = line_m = br_c = br_m = 0
    with csv_path.open(encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            line_c += int(row["LINE_COVERED"])
            line_m += int(row["LINE_MISSED"])
            br_c += int(row["BRANCH_COVERED"])
            br_m += int(row["BRANCH_MISSED"])
    return line_c, line_m, br_c, br_m


def pct(covered: int, missed: int) -> float:
    total = covered + missed
    return (covered / total * 100) if total else 0.0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--min", type=float, default=None,
                        help="整体行覆盖率下限，例如 0.30 表示 30%%")
    parser.add_argument("--markdown", action="store_true", help="以 Markdown 表格输出")
    args = parser.parse_args()

    modules = []
    for module_dir in sorted(p for p in BACKEND.iterdir() if p.is_dir()):
        csv_path = module_dir / "target" / "site" / "jacoco" / "jacoco.csv"
        if csv_path.exists():
            modules.append((module_dir.name, read_module(csv_path)))

    if not modules:
        print("未找到任何 JaCoCo 报告，请先执行：mvn verify", file=sys.stderr)
        return 1

    total = [sum(col) for col in zip(*(m[1] for m in modules))]
    overall_line = pct(total[0], total[1])
    overall_branch = pct(total[2], total[3])

    if args.markdown:
        print("| 模块 | 行覆盖 | 行覆盖率 | 分支覆盖率 |")
        print("|---|---|---|---|")
        for name, (lc, lm, bc, bm) in modules:
            print(f"| {name} | {lc}/{lc + lm} | {pct(lc, lm):.1f}% | {pct(bc, bm):.1f}% |")
        print(f"| **合计** | **{total[0]}/{total[0] + total[1]}** | "
              f"**{overall_line:.1f}%** | **{overall_branch:.1f}%** |")
    else:
        width = max(len(n) for n, _ in modules)
        for name, (lc, lm, bc, bm) in modules:
            print(f"{name:<{width}}  行 {lc:>5}/{lc + lm:<5} {pct(lc, lm):>5.1f}%   "
                  f"分支 {bc:>4}/{bc + bm:<5} {pct(bc, bm):>5.1f}%")
        print("-" * (width + 34))
        print(f"{'整体':<{width}}  行 {total[0]:>5}/{total[0] + total[1]:<5} {overall_line:>5.1f}%   "
              f"分支 {total[2]:>4}/{total[2] + total[3]:<5} {overall_branch:>5.1f}%")

    if args.min is not None and overall_line / 100 < args.min:
        print(f"\n[FAIL] 整体行覆盖率 {overall_line:.1f}% 低于阈值 {args.min * 100:.1f}%", file=sys.stderr)
        return 1

    print(f"\n[OK] 整体行覆盖率 {overall_line:.1f}%，分支覆盖率 {overall_branch:.1f}%")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
