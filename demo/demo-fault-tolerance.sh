#!/bin/bash
# ============================================================
# 场景 4.2 演示：通知/统计服务故障不影响核心借还书
# 演示 RabbitMQ 异步解耦 + try-catch 容错
# ============================================================

API_URL="http://localhost/api/borrow"
BOOK_ID=7

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║   场景 4.2：统计服务故障不影响核心借还书（异步解耦）         ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# ======================== 第一步：正常状态基线 ========================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "【第一步】正常状态下借书 — 确认全链路畅通"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "[服务状态] 查看当前所有服务："
echo "  borrow-service : $(docker inspect -f '{{.State.Status}}' library-borrow-service 2>/dev/null)"
echo "  stats-service  : $(docker inspect -f '{{.State.Status}}' library-stats-service 2>/dev/null)"
echo "  rabbitmq       : $(docker inspect -f '{{.State.Status}}' library-rabbitmq 2>/dev/null)"
echo ""

echo "[正常借书测试]"
RESULT=$(curl -s -X POST $API_URL -H "Content-Type: application/json" -d "{\"bookId\":${BOOK_ID}}")
echo "  响应: $(echo $RESULT | python3 -c "import sys,json;d=json.load(sys.stdin);print(f\"code={d['code']}, message={d['message']}\")" 2>/dev/null)"
echo ""

sleep 2
echo "[stats-service 日志] 消息消费情况："
docker logs library-stats-service --tail 3 2>&1 | grep -E "统计|处理|消息" | tail -2
echo ""

read -p "按 Enter 继续 → 模拟 stats-service 宕机..."
echo ""

# ======================== 第二步：停掉统计服务 ========================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "【第二步】停止 stats-service（模拟统计服务宕机）"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

docker stop library-stats-service > /dev/null 2>&1
echo "[操作] docker stop library-stats-service"
echo "[服务状态]"
echo "  borrow-service : $(docker inspect -f '{{.State.Status}}' library-borrow-service 2>/dev/null)"
echo "  stats-service  : $(docker inspect -f '{{.State.Status}}' library-stats-service 2>/dev/null) ← 已停止"
echo "  rabbitmq       : $(docker inspect -f '{{.State.Status}}' library-rabbitmq 2>/dev/null)"
echo ""

read -p "按 Enter 继续 → 在 stats-service 宕机状态下借书..."
echo ""

# ======================== 第三步：宕机状态下借书 ========================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "【第三步】stats-service 宕机状态下借书 — 核心业务不受影响"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

BOOK_ID2=8
RESULT2=$(curl -s -X POST $API_URL -H "Content-Type: application/json" -d "{\"bookId\":${BOOK_ID2}}")
CODE2=$(echo $RESULT2 | python3 -c "import sys,json;print(json.load(sys.stdin).get('code',''))" 2>/dev/null)
MSG2=$(echo $RESULT2 | python3 -c "import sys,json;print(json.load(sys.stdin).get('message',''))" 2>/dev/null)

echo "[借书请求] bookId=${BOOK_ID2}"
if [ "$CODE2" = "200" ]; then
    echo "  结果: ✅ 借阅成功！（stats-service 宕机不影响核心借书）"
else
    echo "  结果: code=$CODE2, message=$MSG2"
fi
echo ""

echo "[borrow-service 日志] 消息发送情况："
docker logs library-borrow-service --tail 5 2>&1 | grep -E "借阅|事件|发送" | tail -3
echo ""

echo "[RabbitMQ] 消息堆积在队列中（等待 stats-service 恢复后消费）："
QUEUE_MSG=$(docker exec library-rabbitmq rabbitmqctl list_queues name messages 2>/dev/null | grep "stats")
echo "  $QUEUE_MSG"
echo ""

read -p "按 Enter 继续 → 恢复 stats-service..."
echo ""

# ======================== 第四步：恢复服务 ========================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "【第四步】恢复 stats-service — 消息自动重新消费"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

docker start library-stats-service > /dev/null 2>&1
echo "[操作] docker start library-stats-service"
echo "[等待服务启动...]"
sleep 15

echo "[服务状态]"
echo "  stats-service  : $(docker inspect -f '{{.State.Status}}' library-stats-service 2>/dev/null) ← 已恢复"
echo ""

echo "[stats-service 日志] 堆积消息被自动消费："
docker logs library-stats-service --tail 10 2>&1 | grep -E "统计|处理|消息|借书|还书" | tail -5
echo ""

echo "[RabbitMQ] 队列消息已被消费（应为0）："
QUEUE_MSG2=$(docker exec library-rabbitmq rabbitmqctl list_queues name messages 2>/dev/null | grep "stats")
echo "  $QUEUE_MSG2"
echo ""

# ======================== 结论 ========================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "【结论】"
echo ""
echo "  ┌─────────────────────────────────────────────────────────┐"
echo "  │ 1. stats-service 宕机时，借书操作仍然正常完成           │"
echo "  │ 2. 借阅事件通过 RabbitMQ 异步发送，不阻塞核心流程      │"
echo "  │ 3. 事件发送代码包裹在 try-catch 中，异常只打日志不抛出  │"
echo "  │ 4. 消息持久化在 RabbitMQ 队列，服务恢复后自动消费       │"
echo "  │ 5. 最终一致性：统计数据最终会补齐，不会丢失             │"
echo "  └─────────────────────────────────────────────────────────┘"
echo ""
echo "  关键代码位置："
echo "  • BorrowService.java - sendBorrowEvent() 方法"
echo "    try { rabbitTemplate.convertAndSend(...); }"
echo "    catch (Exception e) { log.warn(\"不影响核心流程\"); }"
echo ""
echo "  • RabbitMQ 队列：queue.stats.daily（持久化队列）"
echo "  • 消费者：StatsConsumerService.handleStatsMessage()"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
