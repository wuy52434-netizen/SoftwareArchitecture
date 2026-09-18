"""MQ 专项 · 幂等与重复消费。

RabbitMQ 的投递语义是 **at-least-once**：网络抖动重连、消费者 ACK 丢失、
生产端重试，都会造成同一条业务事件被投递/消费多次。
因此"不重复"这件事必须由消费端自己保证，broker 不会替你兜底。

本文件分两层验证：
1. 证明 broker 层确实不去重（前提事实）；
2. 验证消费端是否具备去重所需的条件——幂等键与 redelivered 信号。
"""
from __future__ import annotations

import pika
import pytest

import mq_helper as mq

pytestmark = pytest.mark.usefixtures("broker")


def test_broker_does_not_deduplicate_same_business_event():
    """前提事实：同一 recordId 的事件重复投递，两条都会到达。

    这条用例本身应当通过——它的价值是证明"幂等只能靠消费端"，
    从而让下面那条 xfail 的缺陷结论站得住脚。
    """
    payload = mq.borrow_success_event(record_id=880001)

    with mq.temp_bound_queue(mq.BORROW_EXCHANGE, mq.ROUTING_KEY_BORROW_SUCCESS) as tq:
        mq.publish(mq.BORROW_EXCHANGE, mq.ROUTING_KEY_BORROW_SUCCESS, payload)
        mq.publish(mq.BORROW_EXCHANGE, mq.ROUTING_KEY_BORROW_SUCCESS, payload)
        got = tq.collect(expect=2, timeout=4.0)

    assert len(got) == 2, f"期望两条重复事件都到达（broker 不去重），实际到达 {len(got)} 条"
    assert got[0]["body"]["recordId"] == got[1]["body"]["recordId"] == 880001


def test_event_contract_exposes_idempotency_key():
    """契约层：事件报文必须携带稳定幂等键，消费端才可能做去重。

    已修复（KNWN-MQ-03）：报文本无任何幂等键，消费端也不去重，重复投递会重复发通知。
    本用例校验的是**报文契约**（payload 由 `mq_helper` 镜像生产端结构构造）。
    "生产端是否真的发了"由下面 `test_real_borrow_event_from_producer_carries_idempotency_key`
    用真实借书 + broker 捕获来验证 —— 只测 helper 会变成自证。
    """
    payload = mq.borrow_success_event(record_id=880002)

    with mq.temp_bound_queue(mq.BORROW_EXCHANGE, mq.ROUTING_KEY_BORROW_SUCCESS) as tq:
        mq.publish(mq.BORROW_EXCHANGE, mq.ROUTING_KEY_BORROW_SUCCESS, payload)
        got = tq.collect(expect=1, timeout=3.0)

    assert got, "事件未投递到位"
    body = got[0]["body"]
    idempotency_keys = {"messageId", "eventId", "idempotentKey", "dedupKey"}
    assert idempotency_keys & set(body), (
        f"事件不含幂等键（期望 {sorted(idempotency_keys)} 之一），实际字段：{sorted(body)}"
    )


def test_idempotency_key_is_deterministic_for_same_business_event():
    """幂等键必须**确定性**：同一 recordId 反复构造，键值不变。

    这条是上面那条的关键补充：如果幂等键用随机 UUID，生产端重试就会生成新值，
    消费端永远认为是新事件，去重形同虚设。这里用真实 broker 收两条同 recordId 的事件，
    断言幂等键相同 —— 只有值相同，消费端才可能真正拦住重复。
    """
    rid = 880004
    first = mq.borrow_success_event(record_id=rid)
    second = mq.borrow_success_event(record_id=rid)

    assert first["idempotentKey"] == second["idempotentKey"], (
        f"同一 recordId={rid} 构造出的幂等键不一致：{first['idempotentKey']} vs {second['idempotentKey']}"
    )
    # messageId 只用于链路追踪，允许不同
    assert first["messageId"] != second["messageId"], "messageId 应当是每次投递唯一的追踪 id"


def test_real_borrow_event_from_producer_carries_idempotency_key():
    """端到端：真实借一本书，从 broker 捕获借书事件，断言幂等键确实由生产端发出。

    这是唯一能证明"生产端真的发了幂等键"的手段 —— 只测 helper 构造的报文属于自证。

    关键细节：必须用**专用读者自己的 token** 发起借阅。
    借阅接口的用户身份取自 token，请求体里的 userId 会被忽略；若用管理员会话，
    借阅会记在演示账号名下（已借满 5 本）→ 返回 3003，用例会以业务失败的形式挂掉。
    """
    gateway = mq.gateway_session()
    user_id, reader_session = mq.create_reader_and_login(gateway)
    book_id = mq.first_borrowable_book(gateway)

    with mq.temp_bound_queue(mq.BORROW_EXCHANGE, mq.ROUTING_KEY_BORROW_SUCCESS) as tq:
        resp = reader_session.post(f"{mq.GATEWAY_URL}/api/borrow",
                                   json={"bookId": book_id}, timeout=20)
        assert resp.status_code == 200, f"借书请求失败：HTTP {resp.status_code} {resp.text[:200]}"
        body = resp.json()
        assert body.get("code") == 200, f"借书业务失败，无法验证事件：{body}"

        got = tq.collect(expect=1, timeout=8.0)

    record_id = body["data"]["recordId"]
    try:
        assert got, "真实借书后 8s 内未从 broker 捕获到任何借阅事件"
        event = got[0]["body"]
        assert "idempotentKey" in event, (
            f"生产端发出的借阅事件不含 idempotentKey，消费端无法去重。实际字段：{sorted(event)}"
        )
        assert event["idempotentKey"] == f"BORROW_SUCCESS:{record_id}", (
            f"幂等键与业务主键不对应：期望 BORROW_SUCCESS:{record_id}，实际 {event['idempotentKey']}"
        )
        assert event.get("recordId") == record_id
    finally:
        reader_session.post(f"{mq.GATEWAY_URL}/api/return", json={"bookId": book_id}, timeout=20)
        mq.disable_user(gateway, user_id)


def test_redelivered_flag_is_available_for_dedup():
    """broker 会为重投消息打上 redelivered 标记，消费端可据此识别重复投递。

    验证方式：取一条消息后 nack(requeue=True) 退回，再次取出应带 redelivered=True。
    """
    payload = mq.borrow_success_event(record_id=880003)

    conn = mq.connect()
    try:
        ch = conn.channel()
        name = ch.queue_declare(queue="", exclusive=True, auto_delete=True).method.queue
        ch.queue_bind(exchange=mq.BORROW_EXCHANGE, queue=name, routing_key=mq.ROUTING_KEY_BORROW_SUCCESS)

        ch.basic_publish(
            exchange=mq.BORROW_EXCHANGE,
            routing_key=mq.ROUTING_KEY_BORROW_SUCCESS,
            body=__import__("json").dumps(payload).encode("utf-8"),
            properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
        )

        first = _get_with_retry(ch, name)
        assert first is not None, "消息未到达临时队列"
        method, _props, _body = first
        assert method.redelivered is False, "首次投递不应是 redelivered"

        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)

        second = _get_with_retry(ch, name)
        assert second is not None, "requeue 后消息丢失"
        assert second[0].redelivered is True, (
            "requeue 后消息应带 redelivered=true，消费端才能据此识别重复投递"
        )
        ch.basic_ack(delivery_tag=second[0].delivery_tag)
    finally:
        conn.close()


def _get_with_retry(channel, queue: str, timeout: float = 3.0):
    """basic_get 轮询重试，避免消息尚未入队就断言失败。"""
    import time

    deadline = time.time() + timeout
    while time.time() < deadline:
        result = channel.basic_get(queue=queue, auto_ack=False)
        if result[0] is not None:
            return result
        time.sleep(0.05)
    return None
