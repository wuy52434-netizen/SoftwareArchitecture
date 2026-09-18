"""MQ 专项 · 拓扑与绑定一致性。

验证目标：生产端（borrow-service 的 RabbitTemplate.convertAndSend）声明的
exchange / routing key，在 broker 上确实存在对应的 exchange、queue 与 binding。
生产端与消费端拓扑错配是消息中间件最常见的事故根因。
"""
from __future__ import annotations

import pytest

import mq_helper as mq

pytestmark = pytest.mark.usefixtures("mgmt")


def test_exchanges_declared_with_expected_type():
    """三个 exchange 必须存在，且类型与代码声明一致。"""
    expected = {
        mq.BORROW_EXCHANGE: "topic",
        mq.NOTIFY_EXCHANGE: "direct",
        mq.STATS_EXCHANGE: "fanout",
    }
    actual = {}
    for name in expected:
        info = mq.get_exchange(name)
        assert info is not None, f"exchange {name} 未在 broker 上声明"
        actual[name] = info["type"]
    assert actual == expected, f"exchange 类型不符，期望 {expected}，实际 {actual}"


def test_exchanges_are_durable():
    """交换机必须持久化，否则 broker 重启后拓扑丢失、消息无处分发。"""
    for name in (mq.BORROW_EXCHANGE, mq.NOTIFY_EXCHANGE, mq.STATS_EXCHANGE):
        info = mq.get_exchange(name)
        assert info is not None, f"exchange {name} 未声明"
        assert info["durable"] is True, f"exchange {name} 未开启持久化"


def test_all_declared_queues_exist_and_are_durable():
    """代码里声明的 7 个业务队列都必须真实存在且持久化。"""
    missing = [q for q in mq.ALL_QUEUES if mq.get_queue(q) is None]
    assert not missing, f"以下队列未在 broker 上声明：{missing}"

    not_durable = [q for q in mq.ALL_QUEUES if mq.get_queue(q)["durable"] is not True]
    assert not not_durable, f"以下队列未开启持久化：{not_durable}"


def test_borrow_success_routing_key_has_binding():
    """核心链路：生产端发 borrow.success，必须至少有一个队列接得住。"""
    targets = {
        b["destination"]
        for b in mq.list_bindings()
        if b.get("source") == mq.BORROW_EXCHANGE and b.get("routing_key") == mq.ROUTING_KEY_BORROW_SUCCESS
    }
    assert targets, (
        f"{mq.BORROW_EXCHANGE} 上不存在 routing_key={mq.ROUTING_KEY_BORROW_SUCCESS} 的绑定，"
        f"借书成功事件会静默丢弃"
    )
    assert mq.QUEUE_BORROW_SUCCESS in targets, (
        f"借书成功事件未绑定到 {mq.QUEUE_BORROW_SUCCESS}，实际绑定到 {sorted(targets)}"
    )


def test_return_success_routing_key_has_binding():
    """核心链路：生产端发 borrow.return，必须有队列承接。"""
    targets = {
        b["destination"]
        for b in mq.list_bindings()
        if b.get("source") == mq.BORROW_EXCHANGE and b.get("routing_key") == mq.ROUTING_KEY_BORROW_RETURN
    }
    assert targets, (
        f"{mq.BORROW_EXCHANGE} 上不存在 routing_key={mq.ROUTING_KEY_BORROW_RETURN} 的绑定，"
        f"还书成功事件会静默丢弃"
    )


def test_borrow_success_queue_declares_dead_letter_policy():
    """queue.borrow.success 必须声明死信策略，否则失败消息无处可去。"""
    args = mq.dead_letter_args(mq.QUEUE_BORROW_SUCCESS)
    assert "x-dead-letter-exchange" in args, (
        f"{mq.QUEUE_BORROW_SUCCESS} 未配置 x-dead-letter-exchange"
    )
    assert "x-dead-letter-routing-key" in args, (
        f"{mq.QUEUE_BORROW_SUCCESS} 未配置 x-dead-letter-routing-key"
    )


def test_dead_letter_target_queue_exists():
    """死信路由键指向的队列必须真实存在，否则死信等于丢信。

    已修复（KNWN-MQ-01）：原先全仓库从未声明 `dead.letter.queue`，
    死信被转发到默认交换机后无队列承接、被 broker 静默丢弃。
    现在 notify-service 的 RabbitMQConfig 显式声明了该队列
    （默认交换机按"队列名 == routing key"自动路由）。
    """
    args = mq.dead_letter_args(mq.QUEUE_BORROW_SUCCESS)
    target = args.get("x-dead-letter-routing-key")
    assert target, (
        f"{mq.QUEUE_BORROW_SUCCESS} 未配置死信路由键，死信无路由依据"
    )
    assert mq.get_queue(target) is not None, (
        f"死信路由键指向 {target}，但 broker 上不存在该队列 —— 死信消息会被直接丢弃"
    )


def test_stats_exchange_is_fanout_for_broadcast():
    """stats.exchange 为 fanout，用于统计广播；其绑定不应带 routing key。"""
    info = mq.get_exchange(mq.STATS_EXCHANGE)
    assert info is not None and info["type"] == "fanout"
    keys = [b.get("routing_key") for b in mq.list_bindings() if b.get("source") == mq.STATS_EXCHANGE]
    assert all(k == "" for k in keys), f"fanout exchange 的绑定不应带 routing key，实际 {keys}"
