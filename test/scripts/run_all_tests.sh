#!/usr/bin/env bash
# ============================================================================
# 图书自动借书机系统 —— 一键拉起环境并真跑全部测试套件
#
#   bash test/scripts/run_all_tests.sh              # 全量
#   bash test/scripts/run_all_tests.sh mq search    # 只跑指定套件
#   bash test/scripts/run_all_tests.sh --no-build   # 不重新 build 镜像
#
# 前置：Docker Desktop 已启动。
#
# 与"跳过式"运行的区别：本脚本会把环境真正拉起来。任何一步失败都会显式报错，
# 不会把"环境没起来"伪装成"用例跳过"。
# ============================================================================
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# Git Bash(MSYS) 会把 `/e/...` 形式的参数"自动转换"给原生 Windows 程序，
# 结果变成 `E:\e\...`，docker-compose / python.exe 都找不到文件。
# 因此凡是交给原生程序的路径一律用 cygpath 转成 Windows 形式。
if command -v cygpath >/dev/null 2>&1; then
  ROOT_WIN="$(cygpath -w "$ROOT")"
else
  ROOT_WIN="$ROOT"
fi
COMPOSE="$ROOT_WIN\\docker\\docker-compose.yml"

# compose 命令在两种环境下不一样：
#   * 本机 Git Bash：只有 standalone 的 `docker-compose`（v5.x）
#   * GitHub Actions ubuntu runner：只有 compose 插件 `docker compose`
# 自动探测，避免脚本只能在某一台机器上跑。
if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
  # 插件形态下路径要用 POSIX 形式（Linux runner 不需要 Windows 路径）
  COMPOSE="$ROOT/docker/docker-compose.yml"
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "[FATAL] 既没有 docker compose 插件也没有 docker-compose 命令"
  exit 2
fi

PY="${LIB_TEST_PYTHON:-C:/Users/ChenZhiBang/.venv-library-test/Scripts/python.exe}"
GATEWAY="http://127.0.0.1:8080"
ES="http://127.0.0.1:9200"

# 本机被测服务必须直连。宿主机可能设置了 HTTP_PROXY（沙箱/抓包工具），
# 走代理会让请求行变成 absolute-form（http://127.0.0.1:9200/...），
# 服务端无法路由，表现为"忽好忽坏"的 400 与读超时
# （已实测：同一请求前一次 200、后一次 400 no handler found）。
# Python 客户端已在代码里 trust_env=False 兜底，这里再给 curl 探测加一层防护。
export NO_PROXY="127.0.0.1,localhost,::1"
export no_proxy="$NO_PROXY"

BUILD_FLAG="--build"
SUITES=()
for arg in "$@"; do
  case "$arg" in
    --no-build) BUILD_FLAG="" ;;
    *) SUITES+=("$arg") ;;
  esac
done
if [ ${#SUITES[@]} -eq 0 ]; then
  SUITES=(unit api contract mq search)
fi

declare -a NAMES=() RESULTS=()

hr() { printf '%s\n' "------------------------------------------------------------"; }
step() { echo ""; hr; echo ">>> $*"; hr; }

record() { NAMES+=("$1"); RESULTS+=("$2"); }

# ------------------------------------------------------------------ 0. 环境检查
step "0/6 检查 Docker 与 Python"
if ! docker info >/dev/null 2>&1; then
  echo "[FATAL] Docker 不可用。请先启动 Docker Desktop，再重新运行本脚本。"
  exit 2
fi
echo "[ok] docker: $(docker --version)"
# $PY 可能是绝对路径（本机 venv）也可能是命令名（CI 的 python3）。
# 用 `-f` 只能判断文件路径，对命令名会误报"不存在"，因此两种形式都校验。
if [ -n "$PY" ] && { [ -f "$PY" ] || command -v "$PY" >/dev/null 2>&1; }; then
  :
else
  echo "[FATAL] 找不到测试解释器（既不是文件也不是命令）：$PY"
  echo "        本机请先创建：uv venv --python 3.12 C:/Users/ChenZhiBang/.venv-library-test"
  exit 2
fi
echo "[ok] python: $("$PY" -V 2>&1)"

# ------------------------------------------------------------------ 1. 拉起基础设施
step "1/6 拉起基础设施（MySQL / Redis / RabbitMQ / Elasticsearch / Nacos / Prometheus）"
"${COMPOSE_CMD[@]}" -f "$COMPOSE" up -d $BUILD_FLAG \
  mysql redis rabbitmq elasticsearch nacos prometheus sentinel-dashboard grafana
[ $? -ne 0 ] && { echo "[FATAL] docker compose up 失败"; exit 1; }

wait_tcp() {
  local host="$1" port="$2" label="$3" tries="${4:-60}"
  for _ in $(seq 1 "$tries"); do
    if "$PY" -c "
import socket,sys
try:
    socket.create_connection(('$host',$port),timeout=2).close()
except OSError:
    sys.exit(1)
" 2>/dev/null; then
      echo "[ok] $label 已就绪"
      return 0
    fi
    sleep 3
  done
  echo "[FATAL] $label 等待超时（$host:$port）"
  return 1
}

# 取 HTTP 状态码。
# 坑：本机 Git Bash 上 `curl -o /dev/null` 会返回 exit 23（write error），
# 若写成 `$(curl ... || echo 000)` 就会把输出叠加成 "200000" 而永远匹配不上。
# 因此这里只截取输出末尾 3 位，不依赖 curl 的退出码。
probe_http() {
  local url="$1" raw
  raw=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null) || true
  printf '%s' "${raw: -3}"
}

wait_tcp 127.0.0.1 3307 "MySQL(3307)" || exit 1
wait_tcp 127.0.0.1 6380 "Redis(6380)" || exit 1
wait_tcp 127.0.0.1 5672 "RabbitMQ(5672)" || exit 1
wait_tcp 127.0.0.1 15672 "RabbitMQ Management(15672)" || exit 1
wait_tcp 127.0.0.1 9200 "Elasticsearch(9200)" || exit 1
wait_tcp 127.0.0.1 8848 "Nacos(8848)" || exit 1

# ES 需要额外等待可用（端口通 ≠ 集群 ready）
echo "等待 ES 集群状态..."
for _ in $(seq 1 40); do
  if [ "$(probe_http "$ES/_cluster/health?wait_for_status=yellow&timeout=2s")" = "200" ]; then
    echo "[ok] ES 集群可用"; break
  fi
  sleep 3
done

# MySQL 需要等初始化脚本跑完
echo "等待 MySQL 初始化..."
for _ in $(seq 1 40); do
  if docker exec library-mysql mysqladmin ping -uroot -proot123 --silent >/dev/null 2>&1; then
    echo "[ok] MySQL 可连接"; break
  fi
  sleep 3
done

# ------------------------------------------------------------------ 2. 拉起微服务
step "2/6 拉起 7 个微服务 + 网关 + 前端 nginx"
# 前端 dist 是 nginx 的挂载源；缺失时 e2e 会全量 setup error（而不是静默跳过）
if [ ! -f "$ROOT/frontend/dist/index.html" ]; then
  echo "[warn] frontend/dist/index.html 不存在，e2e 套件将无法运行。"
  echo "       请先执行：cd frontend && npm run build"
fi
"${COMPOSE_CMD[@]}" -f "$COMPOSE" up -d \
  api-gateway user-service book-service borrow-service search-service notify-service stats-service nginx
[ $? -ne 0 ] && { echo "[FATAL] 微服务启动失败"; exit 1; }

echo "等待网关就绪..."
GATEWAY_OK=0
for _ in $(seq 1 80); do
  code=$(probe_http "$GATEWAY/api/books?page=1&per_page=1")
  if [ "$code" = "200" ] || [ "$code" = "401" ] || [ "$code" = "403" ]; then
    echo "[ok] 网关已就绪（HTTP $code）"; GATEWAY_OK=1; break
  fi
  sleep 5
done
if [ "$GATEWAY_OK" != "1" ]; then
  echo "[FATAL] 网关未就绪，最近日志："
  docker logs --tail 40 library-api-gateway 2>&1 || true
  exit 1
fi

echo "等待前端 nginx 就绪..."
FRONTEND_OK=0
for _ in $(seq 1 40); do
  if [ "$(probe_http "http://127.0.0.1:8090/")" = "200" ]; then
    echo "[ok] 前端已就绪（http://127.0.0.1:8090）"; FRONTEND_OK=1; break
  fi
  sleep 3
done
if [ "$FRONTEND_OK" != "1" ]; then
  echo "[warn] 前端未就绪。若跑 e2e 套件会因环境守卫而跳过，请检查 frontend/dist 是否已构建。"
fi

# 让服务完成注册与拓扑声明（Spring AMQP 建交换机/队列需要一点时间）
echo "等待 Nacos 注册与 RabbitMQ 拓扑声明（20s）..."
sleep 20

# ------------------------------------------------------------------ 3. 灌 ES 索引
step "3/6 灌入 Elasticsearch 索引"
"$PY" "$ROOT/seed_es_books.py" 2>&1 | tail -12
echo "当前索引文档数：$(curl -s "$ES/books/_count" | head -c 120)"

# ------------------------------------------------------------------ 4. 逐个套件真跑
run_suite() {
  local name="$1"; shift
  step "运行套件：$name"
  ( cd "$ROOT" && "$@" )
  local code=$?
  record "$name" "$code"
  if [ $code -eq 0 ]; then
    echo "[PASS] $name"
  else
    echo "[FAIL] $name (exit=$code)"
  fi
  return $code
}

has_suite() {
  local want="$1"
  for s in "${SUITES[@]}"; do [ "$s" = "$want" ] && return 0; done
  return 1
}

if has_suite unit; then
  step "mvn verify（单元测试 + JaCoCo 覆盖率门禁）"
  # -Dfile.encoding=UTF-8：否则 Maven 输出的中文模块名在日志里是乱码，
  # 排查问题时读不出是哪个模块挂了
  ( cd "$ROOT" && bash tmp/mvn.sh -B -ntp -Dfile.encoding=UTF-8 verify ) 2>&1 | tail -30
  code=${PIPESTATUS[0]}
  record "unit+jacoco" "$code"
  [ $code -eq 0 ] && echo "[PASS] unit+jacoco" || echo "[FAIL] unit+jacoco (exit=$code)"
  echo ""
  echo "覆盖率汇总："
  "$PY" "$ROOT/test/tools/coverage_summary.py" || true
fi

if has_suite api; then
  run_suite "api（接口自动化 61 条）" \
    "$PY" -m pytest test/api -q --self-contained-html --html=test/api/report-ci.html
fi

if has_suite contract; then
  run_suite "contract（JSON Schema 契约 6 条）" \
    "$PY" -m pytest test/contract -q --self-contained-html --html=test/contract/report-ci.html
fi

if has_suite mq; then
  # 凭据必须与 compose 的 rabbitmq 服务一致（admin/admin123），
  # 默认 guest/guest 会被 broker 以 PLAIN 认证拒绝，伪装成"拓扑缺失"
  run_suite "mq（RabbitMQ 专项 26 条）" \
    env RABBITMQ_USER=admin RABBITMQ_PASSWORD=admin123 \
    "$PY" -m pytest test/mq -q --self-contained-html --html=test/mq/report-ci.html
fi

if has_suite search; then
  run_suite "search（Elasticsearch 专项 16 条）" \
    "$PY" -m pytest test/search -q --self-contained-html --html=test/search/report-ci.html
fi

if has_suite e2e; then
  run_suite "e2e（Playwright 前端 14 条）" \
    env WEB_BASE_URL=http://127.0.0.1:8090 \
    "$PY" -m pytest test/e2e -q --self-contained-html --html=test/e2e/report-ci.html
fi

# ------------------------------------------------------------------ 5. 汇总
step "测试汇总"
FAILED=0
for i in "${!NAMES[@]}"; do
  if [ "${RESULTS[$i]}" -eq 0 ]; then
    printf '  [PASS] %s\n' "${NAMES[$i]}"
  else
    printf '  [FAIL] %s (exit=%s)\n' "${NAMES[$i]}" "${RESULTS[$i]}"
    FAILED=$((FAILED + 1))
  fi
done

if [ $FAILED -eq 0 ]; then
  echo ""
  echo "全部套件通过。"
  exit 0
fi
echo ""
echo "$FAILED 个套件失败。HTML 报告在各套件目录下的 report-ci.html。"
exit 1
