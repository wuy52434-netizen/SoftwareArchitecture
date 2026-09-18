#!/usr/bin/env bash
# ============================================================================
# 图书自动借书机系统 —— 专项质量套件统一运行器
#
# 把散落在各目录的「一次性 Python 脚本」型专项套件（安全/功能/兼容/回归/冒烟/性能）
# 统一到一条命令下串行执行，并把各自的 *_results.json 汇总成一份总表。
#
#   bash test/scripts/run_special_suites.sh [security functional compat smoke regression perf]
#   不带参数 = 全部 6 项一次性跑完。
#
# 依赖：网关/服务已拉起（通常先用 bash test/scripts/run_all_tests.sh 拉起环境）；
#       .venv-library-test 测试解释器可用。
# ============================================================================
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PY="${LIB_TEST_PYTHON:-C:/Users/ChenZhiBang/.venv-library-test/Scripts/python.exe}"

# 本机被测服务必须直连（宿主机 HTTP_PROXY 会把本地流量也代理掉，见 run_all_tests.sh 注释）
export NO_PROXY="127.0.0.1,localhost,::1"
export no_proxy="$NO_PROXY"

# suite -> 脚本名（如果脚本名不符合 "$dir/run_$dir.py" 的默认规律）
declare -A SCRIPTS=(
  [security]="run_security_tests.py"
  [functional]="run_functional_tests.py"
  [compat]="run_compatibility_test.py"
  [regression]="run_regression.py"
  [smoke]="run_smoke.py"
  [perf]="run_perf_load.py"
)
declare -A LABELS=(
  [security]="安全测试"
  [functional]="功能测试"
  [compat]="兼容性测试"
  [regression]="回归测试"
  [smoke]="冒烟测试"
  [perf]="性能/Sentinel 验证"
)

SUITES=()
if [ $# -eq 0 ]; then
  SUITES=(security functional compat regression smoke perf)
else
  SUITES=("$@")
fi

hr() { printf '%s\n' "------------------------------------------------------------"; }

echo ""
echo "图书系统 · 专项质量套件统一运行"
echo "解释器: $PY"
echo "目标:   网关 http://127.0.0.1:8080"
hr

declare -A TOT PASS FAILED
FAILED_CT=0

for s in "${SUITES[@]}"; do
  script="${SCRIPTS[$s]}"
  dir="$ROOT/test/$s"
  if [ ! -f "$dir/$script" ]; then
    echo "[FAIL] 未找到 $dir/$script —— 跳过"
    FAILED_CT=$((FAILED_CT+1)); continue
  fi
  echo ""
  echo ">>> [${LABELS[$s]}] ($s)"
  ( cd "$dir" && "$PY" "$script" )
  code=$?
  # 从结果 JSON 汇总（套件结果 JSON 统一命名为 <套件>_results.json）
  jsonfile="$dir/${s}_results.json"
  if [ -f "$jsonfile" ]; then
    summary=$(cd "$dir" && "$PY" -c "import json,sys,os;d=json.load(open(os.path.basename('$jsonfile'),encoding='utf-8'));print(d['summary']['total'],d['summary']['pass'],d['summary']['fail'])" 2>/dev/null)
    read -r t p fa <<< "$summary"
    TOT[$s]="${t:-0}"; PASS[$s]="${p:-0}"; FAILED[$s]="${fa:-0}"
  fi
  if [ $code -eq 0 ]; then
    echo "  [PASS] $s (exit=0)"
  else
    echo "  [CHECK] $s exit=$code —— 结果见 $jsonfile"
    FAILED_CT=$((FAILED_CT+1))
  fi
done

echo ""
echo "======================================== 汇总 ========================================"
printf '%-14s %-10s %-8s %-6s\n' "套件" "用例数" "通过" "失败"
TALL=0; TPALL=0; TEALL=0
for s in "${SUITES[@]}"; do
  t="${TOT[$s]:-0或未汇总}"; p="${PASS[$s]:--}"; c="${FAILED[$s]:--}"
  [ "$t" = "0或未汇总" ] && t="-"
  printf '%-14s %-10s %-8s %-6s\n' "${LABELS[$s]}($s)" "$t" "$p" "$c"
done
echo "============================================"
[ $FAILED_CT -eq 0 ] && echo "专项套件全部执行完毕。" || echo "有 $FAILED_CT 项执行异常/结果为 FAIL，请查看上方明细。"
exit $FAILED_CT