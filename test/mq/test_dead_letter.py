"""MQ 专项 · 死信队列（DLX）。

背景：``RabbitMQConfig.borrowSuccessQueue()`` 声明了

    x-dead-letter-exchange = ""                      （默认交换机）
    x-dead-letter-routing-key = "dead.letter.queue"

也就是说：queue.borrow.success 上的消息一旦被 nack/reject（requeue=false）或超过
TTL，broker 会把它转发到**默认交换机**，routing key 为 ``dead.letter.queue``。
问题在于——全仓库从未声明过一个叫 ``dead.letter.queue`` 的队列。
默认交换机上没有该名字的队列，消息就被静默丢弃，等于"配了死信但死信无处可去"。

本文件用两类断言锁死这个风险：静态配置完整性 + 动态路由可达性。
"""
from __future__ import annotations

import time

import pika
import pytest

import mq_helper as mq

pytestmark = pytest.mark.usefixtures("mgmt")


def _get_with_retry(channel, queue: str, timeout: float = 3.0):
    """basic_get 轮询重试，避免消息尚未入队就断言失败。"""
    deadline = time.time() + timeout
    while time.time() < deadline:
        result = channel.basic_get(queue=queue, auto_ack=False)
        if result[0] is not None:
            return result
        time.sleep(0.05)
    return None


def test_borrow_success_queue_declares_complete_dlx_configuration():
    """DLX 配置必须成对出现：光有 exchange 没有 routing key，或反之，都是无效配置。"""
    args = mq.dead_letter_args(mq.QUEUE_BORROW_SUCCESS)

    assert "x-dead-letter-exchange" in args, (
        f"{mq.QUEUE_BORROW_SUCCESS} 未配置 x-dead-letter-exchange，"
        f"被拒绝的消息会直接消失"
    )
    assert "x-dead-letter-routing-key" in args, (
        f"{mq.QUEUE_BORROW_SUCCESS} 配置了 DLX 但没有 routing key，"
        f"转发时无路由依据"
    )
    assert args["x-dead-letter-routing-key"] == mq.QUEUE_DEAD_LETTER, (
        f"死信路由键期望 {mq.QUEUE_DEAD_LETTER}，实际 {args['x-dead-letter-routing-key']}"
    )


def test_dlx_target_queue_is_declared():
    """死信转发目标队列必须真实存在。

    已修复（KNWN-MQ-01）：notify-service 的 RabbitMQConfig 现已声明 dead.letter.queue，
    默认交换机会按"队列名 == routing key"自动把死信路由进去。
    """
    target = mq.dead_letter_args(mq.QUEUE_BORROW_SUCCESS).get("x-dead-letter-routing-key")
    assert target, "queue.borrow.success 未配置死信路由键"
    assert mq.get_queue(target) is not None, (
        f"死信目标队列 {target} 不存在 —— 死信消息会被默认交换机退回并丢弃"
    )


def test_dlx_route_is_actually_routable(broker):
    """动态验证：完全按队列 DLX 配置走一遍，消息是否有人接收。

    这一步比读 arguments 更有说服力——它模拟了 broker 真实的死信转发行为。
    """
    args = mq.dead_letter_args(mq.QUEUE_BORROW_SUCCESS)
    dlx = args.get("x-dead-letter-exchange", "")
    key = args.get("x-dead-letter-routing-key")
    assert key, "队列未配置死信路由键，无需验证"

    returned = mq.publish_mandatory(dlx, key, {"probe": "dead-letter", "queue": mq.QUEUE_BORROW_SUCCESS})

    assert not returned, (
        f"死信按配置转发到 exchange='{dlx}'(默认交换机) routing_key='{key}' 后无可路由队列，"
        f"broker 已退回消息：{returned}"
    )


def test_rejected_message_actually_lands_in_dead_letter_queue(broker):
    """端到端：真正 nack 一条消息，验证它落在 dead.letter.queue 里。

    前两条只验证"配置正确 / 路由可达"，这条走完整链路：
    入队 → basic_get → ``basic_nack(requeue=False)`` → broker 按 DLX 转发 → 从死信队列取回原消息。
    只有这条能证明"死信不再丢失"。

    实现要点：**不能**直接往 queue.borrow.success 投递再 basic_get —— 那个队列上挂着
    notify-service 的在线消费者，消息会被它抢走，测试永远取不到（曾因此报
    "未能取到刚投递的消息并 nack"）。这里读取生产队列**实际生效**的 DLX 参数，
    原样应用到一个测试私有队列上，再走一遍 nack → 死信 的完整流程。
    """
    marker = f"dlq-probe-{int(time.time() * 1000)}"
    payload = mq.borrow_success_event(record_id=880777, probeMarker=marker)

    mq.purge(mq.QUEUE_DEAD_LETTER)

    conn = mq.connect()
    queue_name = None
    try:
        channel = conn.channel()
        queue_name = mq.declare_probe_queue_with_dlx(channel, mq.QUEUE_BORROW_SUCCESS)

        # 走默认交换机 + 队列名直投：只进这个测试私有队列，不与业务消费者抢
        channel.basic_publish(
            exchange="",
            routing_key=queue_name,
            body=__import__("json").dumps(payload).encode("utf-8"),
            properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
        )

        got = _get_with_retry(channel, queue_name)
        assert got is not None, "探针队列未收到消息，无法触发死信"
        channel.basic_nack(delivery_tag=got[0].delivery_tag, requeue=False)
    finally:
        conn.close()

    dead = mq.drain(mq.QUEUE_DEAD_LETTER, timeout=6.0)
    assert dead, (
        f"nack(requeue=false) 后 {mq.QUEUE_DEAD_LETTER} 里没有消息 —— "
        f"死信仍被丢弃（DLX 配置或目标队列声明有问题）"
    )
    assert any((m.get("body") or {}).get("probeMarker") == marker for m in dead), (
        f"死信队列里的消息不是刚才那条（marker={marker}）：{dead}"
    )
