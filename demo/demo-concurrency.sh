#!/bin/bash
# ============================================================
# 场景 4.1 演示：高并发借同一本书 — 分布式锁防超借
#
# 演示方式：
#   终端1: 运行本脚本
#   终端2: docker exec library-redis redis-cli MONITOR（观察锁）
#   终端3: docker logs -f library-borrow-service（观察日志）
# ============================================================

BOOK_ID=5
API_URL="http://localhost/api/borrow"
CONCURRENT=10
TMPDIR=$(mktemp -d)

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║   场景 4.1：高并发借同一本书 — Redis 分布式锁防止超借       ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "┌─────────────────────────────────────────────────────────────┐"
echo "│ 建议同时打开另外两个终端观察：                              │"
echo "│   终端2: docker exec library-redis redis-cli MONITOR        │"
echo "│   终端3: docker logs -f library-borrow-service              │"
echo "└─────────────────────────────────────────────────────────────┘"
echo ""

# ======================== 第一步：准备 ========================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "【第一步】准备测试环境 — 设置库存为 1"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 设置库存为1
docker exec library-mysql mysql -uroot -proot123 -e \
  "UPDATE library.book_info SET available_copies=1, status='available' WHERE id=${BOOK_ID}" 2>/dev/null
# 清缓存
docker exec library-redis redis-cli DEL "book:detail:${BOOK_ID}" > /dev/null 2>&1

echo "[数据库] 图书ID=${BOOK_ID} 库存已设为 1"
echo ""

TITLE=$(curl -s "http://localhost/api/books/${BOOK_ID}" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['title'])" 2>/dev/null)
echo "[图书] ${TITLE}，可借数量：1"
echo ""

read -p "按 Enter 发起 ${CONCURRENT} 个并发借书请求..."
echo ""

# ======================== 第二步：并发请求 ========================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "【第二步】同时发起 ${CONCURRENT} 个并发借书请求"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "[$(date +%H:%M:%S)] 发射！${CONCURRENT} 个请求同时出发..."
echo ""

# 真正的并发：所有请求同时发出
for i in $(seq 1 $CONCURRENT); do
    curl -s -X POST $API_URL \
        -H "Content-Type: application/json" \
        -d "{\"bookId\":${BOOK_ID}}" \
        -o "${TMPDIR}/result_${i}.json" &
done

# 等待全部完成
wait
echo "[$(date +%H:%M:%S)] 全部请求已返回"
echo ""

# ======================== 第三步：分析结果 ========================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "【第三步】分析结果"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

SUCCESS=0
FAIL=0

echo "┌──────┬──────────┬──────────────────────────────────────────┐"
echo "│ 序号 │   结果   │ 响应信息                                 │"
echo "├──────┼──────────┼──────────────────────────────────────────┤"

for i in $(seq 1 $CONCURRENT); do
    if [ -f "${TMPDIR}/result_${i}.json" ]; then
        CODE=$(python3 -c "import json;d=json.load(open('${TMPDIR}/result_${i}.json'));print(d.get('code',''))" 2>/dev/null)
        MSG=$(python3 -c "import json;d=json.load(open('${TMPDIR}/result_${i}.json'));print(d.get('message','')[:36])" 2>/dev/null)
    else
        CODE="ERR"
        MSG="请求失败"
    fi

    if [ "$CODE" = "200" ]; then
        STATUS="✅ 成功"
        SUCCESS=$((SUCCESS+1))
    else
        STATUS="❌ 拒绝"
        FAIL=$((FAIL+1))
    fi
    printf "│  %2d  │ %s │ %-40s │\n" "$i" "$STATUS" "$MSG"
done

echo "└──────┴──────────┴──────────────────────────────────────────┘"
echo ""
echo "┌─────────────────────────────────────────────┐"
echo "│ 统计结果：                                  │"
echo "│   并发请求数：${CONCURRENT}                            │"
printf "│   成功借阅：  %-2d 次                         │\n" $SUCCESS
printf "│   被拒绝：    %-2d 次                         │\n" $FAIL
echo "└─────────────────────────────────────────────┘"
echo ""

# ======================== 第四步：验证数据 ========================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "【第四步】验证数据一致性"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "[MySQL 查询] 图书最终库存："
docker exec library-mysql mysql -uroot -proot123 -e \
  "SELECT id, title, available_copies, status FROM library.book_info WHERE id=${BOOK_ID}" 2>/dev/null | tail -2
echo ""

echo "[关键验证] available_copies >= 0 ?  — 没有超卖！"
FINAL=$(docker exec library-mysql mysql -uroot -proot123 -Nse \
  "SELECT available_copies FROM library.book_info WHERE id=${BOOK_ID}" 2>/dev/null)
if [ "$FINAL" -ge 0 ]; then
    echo "  ✅ 库存=${FINAL}，数据一致性保证！"
else
    echo "  ❌ 库存=${FINAL}，出现超卖（不应发生）"
fi
echo ""

# ======================== 结论 ========================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "【技术原理】"
echo ""
echo "  ┌─────────────────────────────────────────────────────────┐"
echo "  │                                                         │"
echo "  │  请求1 ──→ tryLock(bookId:5) ──→ 获得锁 ──→ 借书成功  │"
echo "  │  请求2 ──→ tryLock(bookId:5) ──→ 等待超时 ──→ 拒绝    │"
echo "  │  请求3 ──→ tryLock(bookId:5) ──→ 等待超时 ──→ 拒绝    │"
echo "  │  ...                                                    │"
echo "  │  请求N ──→ tryLock(bookId:5) ──→ 获得锁 ──→ 库存=0    │"
echo "  │                                           ──→ 拒绝     │"
echo "  │                                                         │"
echo "  │  Redis Key: borrow:lock:{bookId}:{copyId}              │"
echo "  │  锁超时: 10秒    等待超时: 3秒                          │"
echo "  │  解锁方式: Lua 脚本原子操作（防误删）                   │"
echo "  │                                                         │"
echo "  └─────────────────────────────────────────────────────────┘"
echo ""
echo "  关键代码: BorrowService.java"
echo "    RedisLock.LockResult lockResult = "
echo "        redisLock.tryLock(lockKey, 3, 10, TimeUnit.SECONDS);"
echo ""

# 清理临时文件
rm -rf $TMPDIR
