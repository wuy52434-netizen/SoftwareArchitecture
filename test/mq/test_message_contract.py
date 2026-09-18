"""MQ 专项 · 事件消息契约。

生产端 BorrowService.sendBorrowEvent 与消费端 NotifyConsumerService 之间是隐式契约：
消费端直接 ``Map<String, Object>`` 接收，没有类型约束。字段一旦改名或类型漂移，
只会在运行时静默出错（手机号为空就不发短信、日期解析失败就整条消息抛异常被吞掉），
因此必须用契约测试把字段名、类型和取值范围钉住。
"""
from __future__ import annotations

import time

import pytest

import mq_helper as mq

# BorrowService.sendBorrowEvent 实际投递的字段集合
REQUIRED_FIELDS = (
    "eventType",
    "recordId",
    "userId",
    "bookId",
    "copyId",
    "bookTitle",
    "borrowDate",
    "dueDate",
    "timestamp",
)

VALID_EVENT_TYPES = {"BORROW_SUCCESS", "RETURN_SUCCESS", "BORROW_FAIL"}

pytestmark = pytest.mark.usefixtures("broker")


@pytest.fixture
def received_event():
    """投递一条借书成功事件并取回实际落到队列上的报文。"""
    payload = mq.borrow_success_event()
    with mq.temp_bound_queue(mq.BORROW_EXCHANGE, mq.ROUTING_KEY_BORROW_SUCCESS) as tq:
        mq.publish(mq.BORROW_EXCHANGE, mq.ROUTING_KEY_BORROW_SUCCESS, payload)
        got = tq.collect(expect=1, timeout=3.0)
    assert len(got) == 1, "事件未投递到位，无法进行契约校验"
    return got[0]


def test_all_required_fields_present(received_event):
    """所有必填字段必须存在，缺字段会导致消费端静默降级。"""
    body = received_event["body"]
    missing = [f for f in REQUIRED_FIELDS if f not in body]
    assert not missing, f"事件缺少必填字段：{missing}，实际字段：{sorted(body)}"


def test_event_type_is_within_enum(received_event):
    """eventType 必须是消费端 switch/if 分支能识别的枚举值。"""
    event_type = received_event["body"].get("eventType")
    assert event_type in VALID_EVENT_TYPES, (
        f"eventType={event_type} 不在合法枚举 {sorted(VALID_EVENT_TYPES)} 内，"
        f"消费端会走空分支"
    )


def test_id_fields_are_positive_integers(received_event):
    """标识类字段必须是正整数，避免 "" / null 导致下游按 0 查询。"""
    body = received_event["body"]
    for field in ("recordId", "userId", "bookId", "copyId"):
        value = body.get(field)
        assert isinstance(value, int) and not isinstance(value, bool), (
            f"{field} 应为整数，实际 {type(value).__name__}={value!r}"
        )
        assert value > 0, f"{field} 应为正数，实际 {value}"


def test_date_fields_are_iso8601_or_null(received_event):
    """日期字段必须是 ISO-8601（yyyy-MM-dd），消费端用 DateTimeFormatter 直接解析。"""
    body = received_event["body"]
    for field in ("borrowDate", "dueDate", "returnDate"):
        value = body.get(field)
        if value is None:
            continue
        assert isinstance(value, str), f"{field} 应为字符串或 null，实际 {value!r}"
        parts = value.split("-")
        assert len(parts) == 3 and all(p.isdigit() for p in parts) and len(parts[0]) == 4, (
            f"{field}={value} 不是 yyyy-MM-dd 格式，消费端 LocalDate.parse 会抛异常"
        )


def test_timestamp_is_epoch_millis(received_event):
    """timestamp 必须是毫秒级时间戳且与当前时间接近，便于排查乱序与积压。"""
    ts = received_event["body"].get("timestamp")
    assert isinstance(ts, int), f"timestamp 应为整数，实际 {type(ts).__name__}"
    now_ms = int(time.time() * 1000)
    assert abs(now_ms - ts) < 300_000, (
        f"timestamp={ts} 与当前时间相差过大，疑似用了秒级时间戳或没带时区"
    )


def test_json_message_converter_is_applied(received_event):
    """消息必须带 application/json 内容类型，否则 Jackson2JsonMessageConverter 反序列化失败。"""
    assert received_event["content_type"] == "application/json", (
        f"content_type 期望 application/json，实际 {received_event['content_type']}"
    )


def test_event_carries_idempotency_key(received_event):
    """事件应携带稳定幂等键，供消费端去重。

    已修复（KNWN-MQ-03）：原先报文无任何幂等键，消费端也不去重。
    现在生产端发出 `messageId`（追踪用）+ `idempotentKey`（去重用，由业务事实确定性派生）。
    """
    body = received_event["body"]
    keys = {"messageId", "eventId", "idempotentKey", "dedupKey"}
    assert keys & set(body), (
        f"事件缺少幂等键（期望包含 {sorted(keys)} 之一），当前字段：{sorted(body)}"
    )
    assert "idempotentKey" in body, (
        "缺少确定性幂等键 idempotentKey：只有随机 messageId 无法真正拦住重复投递"
    )
