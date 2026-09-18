"""MQ 专项 · 消息投递与路由。

用测试专属的临时队列订阅同一 exchange + routing key，验证生产端发出的消息
能否按预期路由到位。不消费业务队列，因此不受 notify-service / stats-service
正在运行的消费者影响。
"""
from __future__ import annotations

import pytest

import mq_helper as mq


def test_borrow_success_event_routed_to_queue(broker):
    """借书成功事件（borrow.success）必须被投递到订阅该路由键的队列。"""
    payload = mq.borrow_success_event()

    with mq.temp_bound_queue(mq.BORROW_EXCHANGE, mq.ROUTING_KEY_BORROW_SUCCESS) as tq:
        mq.publish(mq.BORROW_EXCHANGE, mq.ROUTING_KEY_BORROW_SUCCESS, payload)
        got = tq.collect(expect=1, timeout=3.0)

    assert len(got) == 1, f"借书成功事件未路由到位，收到 {len(got)} 条"
    assert got[0]["body"]["recordId"] == payload["recordId"]
    assert got[0]["content_type"] == "application/json", (
        f"消息内容类型应为 application/json，实际 {got[0]['content_type']}"
    )


def test_borrow_return_event_routed_to_queue(broker):
    """还书成功事件（borrow.return）必须被投递到订阅该路由键的队列。"""
    payload = mq.borrow_success_event(eventType="RETURN_SUCCESS", returnDate="2026-09-18")

    with mq.temp_bound_queue(mq.BORROW_EXCHANGE, mq.ROUTING_KEY_BORROW_RETURN) as tq:
        mq.publish(mq.BORROW_EXCHANGE, mq.ROUTING_KEY_BORROW_RETURN, payload)
        got = tq.collect(expect=1, timeout=3.0)

    assert len(got) == 1, f"还书成功事件未路由到位，收到 {len(got)} 条"
    assert got[0]["body"]["eventType"] == "RETURN_SUCCESS"


def test_unmatched_routing_key_is_not_delivered(broker):
    """未定义的 routing key 不应被投递到任何订阅了合法路由键的队列。

    这是一条"负向用例"：它能证明测试真的在区分路由键，
    而不是无论发什么都断言成功（否则前面的正向用例没有意义）。
    """
    with mq.temp_bound_queue(mq.BORROW_EXCHANGE, mq.ROUTING_KEY_BORROW_SUCCESS) as tq:
        mq.publish(mq.BORROW_EXCHANGE, "borrow.__nonexistent__", mq.borrow_success_event())
        got = tq.collect(expect=1, timeout=2.0)

    assert got == [], f"非法 routing key 的消息被错误投递，收到 {got}"


def test_borrow_success_broadcasts_to_multiple_queues(broker):
    """一对多扇出：借书成功事件应同时送达通知与统计链路。

    borrow.exchange 上 borrow.success 绑定了 queue.borrow.success / queue.notify.sms /
    queue.notify.email / queue.stats.daily，验证扇出是否完整。
    """
    expected_targets = set(mq.BORROW_SUCCESS_FANOUT_QUEUES)
    actual_targets = {
        b["destination"]
        for b in mq.list_bindings()
        if b.get("source") == mq.BORROW_EXCHANGE
        and b.get("routing_key") == mq.ROUTING_KEY_BORROW_SUCCESS
    }
    missing = expected_targets - actual_targets
    assert not missing, f"借书成功事件的扇出绑定缺失：{sorted(missing)}"


def test_stats_exchange_fanout_broadcasts(broker):
    """stats.exchange 为 fanout，绑定其上的队列都应收到广播（忽略 routing key）。"""
    payload = {"eventType": "STATS_TEST_BROADCAST", "timestamp": 0}

    with mq.temp_bound_queue(mq.STATS_EXCHANGE, "") as tq:
        mq.publish(mq.STATS_EXCHANGE, "", payload)
        got = tq.collect(expect=1, timeout=3.0)

    assert len(got) == 1, f"fanout 广播未送达，收到 {len(got)} 条"
    assert got[0]["body"]["eventType"] == "STATS_TEST_BROADCAST"
