"""RabbitMQ 专项测试辅助模块。

设计原则（很重要，直接决定测试有没有价值）：

1. **拓扑断言只读获取**：exchange / queue / binding 的存在性、类型、DLX 参数一律通过
   RabbitMQ Management HTTP API 读取，测试自身**不声明任何拓扑**。
   如果测试自己去 declare，就会"帮被测系统把缺的东西补上"，从而掩盖真实缺陷。
2. **只在投递/消费类用例里用 pika 直连**，且临时资源必须清理。
3. 未连接 broker 时一律跳过（见 ``broker_available``），保证 CI 上不会假失败。

拓扑来源：``backend/notify-service/.../config/RabbitMQConfig.java``
          ``backend/common/.../constant/Constants.java``
"""
from __future__ import annotations

import contextlib
import json
import os
import socket
import time
import uuid
from typing import Any

import pika
import requests

# ---------------------------------------------------------------- 连接参数
RABBIT_HOST = os.getenv("RABBITMQ_HOST", "127.0.0.1")
RABBIT_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBIT_VHOST = os.getenv("RABBITMQ_VHOST", "/")
# 默认凭据必须与 docker/docker-compose.yml 里 rabbitmq 服务的
# RABBITMQ_DEFAULT_USER/PASS（admin / admin123）保持一致。
# 之前默认写成 guest/guest，broker 以 PLAIN 认证拒绝，导致 7 failed + 6 errors + 11 skipped
# ——看起来像"拓扑缺失"，实际是测试自己的凭据错了。
RABBIT_USER = os.getenv("RABBITMQ_USER", "admin")
RABBIT_PASSWORD = os.getenv("RABBITMQ_PASSWORD", "admin123")
MGMT_URL = os.getenv("RABBITMQ_MGMT_URL", "http://127.0.0.1:15672").rstrip("/")
MGMT_USER = os.getenv("RABBITMQ_MGMT_USER", RABBIT_USER)
MGMT_PASSWORD = os.getenv("RABBITMQ_MGMT_PASSWORD", RABBIT_PASSWORD)

# ---------------------------------------------------------------- 被测拓扑常量
BORROW_EXCHANGE = "borrow.exchange"
NOTIFY_EXCHANGE = "notify.exchange"
STATS_EXCHANGE = "stats.exchange"

QUEUE_BORROW_SUCCESS = "queue.borrow.success"
QUEUE_BORROW_FAIL = "queue.borrow.fail"
QUEUE_RETURN_SUCCESS = "queue.return.success"
QUEUE_NOTIFY_SMS = "queue.notify.sms"
QUEUE_NOTIFY_EMAIL = "queue.notify.email"
QUEUE_NOTIFY_INNER = "queue.notify.inner"
QUEUE_STATS_DAILY = "queue.stats.daily"
# queue.borrow.success 的 x-dead-letter-routing-key 指向该队列
QUEUE_DEAD_LETTER = "dead.letter.queue"

ROUTING_KEY_BORROW_SUCCESS = "borrow.success"
ROUTING_KEY_BORROW_RETURN = "borrow.return"
ROUTING_KEY_BORROW_FAIL = "borrow.fail"

ALL_QUEUES = (
    QUEUE_BORROW_SUCCESS,
    QUEUE_BORROW_FAIL,
    QUEUE_RETURN_SUCCESS,
    QUEUE_NOTIFY_SMS,
    QUEUE_NOTIFY_EMAIL,
    QUEUE_NOTIFY_INNER,
    QUEUE_STATS_DAILY,
)

# 借书成功事件会被路由到这些队列（borrow.exchange 上一对多绑定）
BORROW_SUCCESS_FANOUT_QUEUES = (QUEUE_BORROW_SUCCESS, QUEUE_NOTIFY_SMS, QUEUE_NOTIFY_EMAIL, QUEUE_STATS_DAILY)


# ---------------------------------------------------------------- 连通性探测
def broker_alive(timeout: float = 2.0) -> bool:
    """TCP 探测 AMQP 端口。"""
    try:
        with socket.create_connection((RABBIT_HOST, RABBIT_PORT), timeout=timeout):
            return True
    except OSError:
        return False


def management_alive(timeout: float = 3.0) -> bool:
    """探测 Management HTTP API，拓扑断言依赖它。"""
    try:
        resp = requests.get(f"{MGMT_URL}/api/overview", auth=(MGMT_USER, MGMT_PASSWORD), timeout=timeout)
        return resp.status_code == 200
    except requests.RequestException:
        return False


def connect() -> pika.BlockingConnection:
    """建立 AMQP 连接（调用方负责关闭）。"""
    params = pika.ConnectionParameters(
        host=RABBIT_HOST,
        port=RABBIT_PORT,
        virtual_host=RABBIT_VHOST,
        credentials=pika.PlainCredentials(RABBIT_USER, RABBIT_PASSWORD),
        heartbeat=30,
        blocked_connection_timeout=10,
    )
    return pika.BlockingConnection(params)


# ---------------------------------------------------------------- 只读内省（Management API）
def _api(path: str) -> Any:
    vhost = "%2F" if RABBIT_VHOST == "/" else RABBIT_VHOST
    url = f"{MGMT_URL}/api/{path.format(vhost=vhost)}"
    resp = requests.get(url, auth=(MGMT_USER, MGMT_PASSWORD), timeout=5)
    resp.raise_for_status()
    return resp.json()


def get_exchange(name: str) -> dict | None:
    try:
        return _api(f"exchanges/{{vhost}}/{name}")
    except requests.HTTPError:
        return None


def get_queue(name: str) -> dict | None:
    try:
        return _api(f"queues/{{vhost}}/{name}")
    except requests.HTTPError:
        return None


def list_queues() -> list[dict]:
    return _api("queues/{vhost}")


def list_bindings() -> list[dict]:
    return _api("bindings/{vhost}")


def queue_depth(name: str) -> int:
    """队列当前消息数；队列不存在返回 -1。"""
    info = get_queue(name)
    return -1 if info is None else int(info.get("messages", 0))


def bindings_for_queue(queue: str) -> list[dict]:
    return [b for b in list_bindings() if b.get("destination") == queue and b.get("routing_key")]


def dead_letter_args(queue: str) -> dict:
    """读取队列的 arguments（含 x-dead-letter-exchange / x-dead-letter-routing-key）。"""
    info = get_queue(queue)
    return {} if info is None else dict(info.get("arguments") or {})


# ---------------------------------------------------------------- 投递与消费
def publish(exchange: str, routing_key: str, payload: dict, headers: dict | None = None) -> None:
    """以 JSON 内容类型向指定 exchange 投递一条消息。"""
    conn = connect()
    try:
        ch = conn.channel()
        ch.basic_publish(
            exchange=exchange,
            routing_key=routing_key,
            body=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
            properties=pika.BasicProperties(
                content_type="application/json",
                delivery_mode=2,
                message_id=str(uuid.uuid4()),
                headers=headers or {},
            ),
        )
    finally:
        conn.close()


def drain(queue: str, timeout: float = 2.0, max_messages: int = 100) -> list[dict]:
    """把队列里的消息全部取走（auto_ack=False + nack requeue=True 会死循环，
    因此这里用 auto_ack=True 消费，仅用于断言"消息到没到"这类场景）。"""
    conn = connect()
    got: list[dict] = []
    try:
        ch = conn.channel()
        deadline = time.time() + timeout
        while len(got) < max_messages and time.time() < deadline:
            method, props, body = ch.basic_get(queue=queue, auto_ack=True)
            if method is None:
                break
            try:
                decoded = json.loads(body.decode("utf-8"))
            except (UnicodeDecodeError, json.JSONDecodeError):
                decoded = {"_raw": body.decode("utf-8", errors="replace")}
            got.append({
                "routing_key": method.routing_key,
                "exchange": method.exchange,
                "redelivered": method.redelivered,
                "content_type": props.content_type,
                "message_id": props.message_id,
                "headers": dict(props.headers or {}),
                "body": decoded,
            })
    finally:
        conn.close()
    return got


def nack_to_dead_letter(queue: str, timeout: float = 2.0) -> bool:
    """取一条消息并 ``basic_nack(requeue=False)``，触发 broker 按队列 DLX 参数死信转发。

    返回是否成功拒绝了一条消息（队列里没有消息时返回 False）。
    """
    conn = connect()
    try:
        ch = conn.channel()
        method, _props, _body = ch.basic_get(queue=queue, auto_ack=False)
        if method is None:
            return False
        ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
        return True
    finally:
        conn.close()


def purge(queue: str) -> None:
    """清空队列，用于用例前置清理。"""
    try:
        conn = connect()
        try:
            conn.channel().queue_purge(queue=queue)
        finally:
            conn.close()
    except pika.exceptions.ChannelClosedByBroker:
        pass


def only_purge_if_exists(queue: str) -> None:
    if get_queue(queue) is not None:
        purge(queue)


# ---------------------------------------------------------------- 不可路由探测
def publish_mandatory(exchange: str, routing_key: str, payload: dict, timeout: float = 2.0) -> list[dict]:
    """以 ``mandatory=True`` 投递并捕获 basic.return。

    返回值非空表示消息**无可路由目标**，被 broker 退回（即生产端如果不带
    mandatory 且不处理 return，这条消息就彻底消失了）。
    """
    conn = connect()
    returned: list[dict] = []
    try:
        ch = conn.channel()

        def _on_return(_ch, method, properties, body):
            returned.append({
                "reply_code": method.reply_code,
                "reply_text": method.reply_text,
                "exchange": method.exchange,
                "routing_key": method.routing_key,
                "body": body.decode("utf-8", errors="replace"),
            })

        ch.add_on_return_callback(_on_return)
        ch.basic_publish(
            exchange=exchange,
            routing_key=routing_key,
            body=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
            properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
            mandatory=True,
        )
        deadline = time.time() + timeout
        while time.time() < deadline and not returned:
            conn.process_data_events(time_limit=0.2)
    finally:
        conn.close()
    return returned


# ---------------------------------------------------------------- 临时绑定队列（路由验证专用）
class TempQueue:
    """测试私有的临时队列句柄。

    为什么需要它：业务队列上挂着 notify-service / stats-service 的消费者，
    直接往 queue.borrow.success 投递再 basic_get 会与正在运行的消费者抢消息，
    导致断言随机失败。临时队列只订阅、不消费业务队列，可以稳定验证"路由有没有到位"。
    """

    def __init__(self, channel, name: str):
        self._channel = channel
        self.name = name

    def collect(self, expect: int = 1, timeout: float = 3.0) -> list[dict]:
        """轮询收集消息，凑够 expect 条或超时后返回。"""
        got: list[dict] = []
        deadline = time.time() + timeout
        while len(got) < expect and time.time() < deadline:
            method, props, body = self._channel.basic_get(queue=self.name, auto_ack=True)
            if method is None:
                time.sleep(0.05)
                continue
            try:
                decoded = json.loads(body.decode("utf-8"))
            except (UnicodeDecodeError, json.JSONDecodeError):
                decoded = {"_raw": body.decode("utf-8", errors="replace")}
            got.append({
                "routing_key": method.routing_key,
                "exchange": method.exchange,
                "content_type": props.content_type,
                "message_id": props.message_id,
                "headers": dict(props.headers or {}),
                "body": decoded,
            })
        return got


@contextlib.contextmanager
def temp_bound_queue(exchange: str, routing_key: str, extra_bindings=None):
    """创建独占临时队列并绑定，退出时自动删除（exclusive + auto_delete）。"""
    conn = connect()
    try:
        ch = conn.channel()
        name = ch.queue_declare(queue="", exclusive=True, auto_delete=True).method.queue
        ch.queue_bind(exchange=exchange, queue=name, routing_key=routing_key)
        for extra_exchange, extra_key in (extra_bindings or []):
            ch.queue_bind(exchange=extra_exchange, queue=name, routing_key=extra_key)
        yield TempQueue(ch, name)
    finally:
        conn.close()


# ---------------------------------------------------------------- 事件样例
def borrow_success_event(record_id: int | None = None, **overrides: Any) -> dict:
    """构造一条与 BorrowService.sendBorrowEvent 结构一致的借书成功事件。

    注意：本函数是**生产者报文结构的镜像**。改这里不代表生产端就改了 ——
    真正验证生产端确实发出幂等键的是
    ``test_idempotency.py::test_real_borrow_event_from_producer_carries_idempotency_key``
    （真实借书 → 从 broker 捕获事件）。两者分工：这里守住"契约长什么样"，
    那条守住"生产端真的照做了"。
    """
    rid = record_id or 900000 + int(time.time()) % 100000
    payload = {
        "eventType": "BORROW_SUCCESS",
        "messageId": str(uuid.uuid4()),
        # 确定性幂等键：同一业务事实重复投递时保持不变，消费端据此去重
        "idempotentKey": f"BORROW_SUCCESS:{rid}",
        "recordId": rid,
        "userId": 1,
        "bookId": 1,
        "copyId": 1,
        "bookTitle": "软件测试的艺术",
        "borrowDate": "2026-09-18",
        "dueDate": "2026-10-18",
        "returnDate": None,
        "timestamp": int(time.time() * 1000),
    }
    payload.update(overrides)
    return payload


# ---------------------------------------------------------------- 网关侧辅助
#: 走 API 网关做"真实业务动作"（例如借一本书），以便捕获生产端真实发出的事件。
#: 事件契约不能只靠 mq_helper 自己构造的 payload 来验证 —— 那是自证。
GATEWAY_URL = os.getenv("GATEWAY_URL", "http://127.0.0.1:8080").rstrip("/")
ADMIN_USER = os.getenv("ADMIN_USER", "admin")
ADMIN_PASS = os.getenv("ADMIN_PASS", "admin123")


def gateway_session() -> requests.Session:
    """直连网关的管理员会话（trust_env=False：禁止继承宿主机 HTTP_PROXY）。"""
    session = requests.Session()
    session.trust_env = False
    session.headers.update({"Content-Type": "application/json"})
    resp = session.post(f"{GATEWAY_URL}/api/auth/login",
                        json={"username": ADMIN_USER, "password": ADMIN_PASS}, timeout=15)
    token = (resp.json().get("data") or {}).get("accessToken")
    if not token:
        raise AssertionError(f"管理员登录失败，无法做真实业务动作：{resp.text[:200]}")
    session.headers["Authorization"] = f"Bearer {token}"
    return session


def create_dedicated_reader(session: requests.Session) -> int:
    """建一个专用读者账号并返回 userId。

    不复用演示账号：它可能已被历史测试借满额度，会以业务失败的形式掩盖真正要验证的东西。
    """
    username = f"mq_reader_{uuid.uuid4().hex[:8]}"
    resp = session.post(f"{GATEWAY_URL}/api/users", json={
        "username": username,
        "password": "Passw0rd!",
        "realName": "MQ 事件捕获专用读者",
        "userType": "reader",
    }, timeout=15)
    body = resp.json()
    if body.get("code") != 200:
        raise AssertionError(f"创建专用读者失败：{body}")
    return body["data"]["userId"]


def disable_user(session: requests.Session, user_id: int) -> None:
    """停用专用账号，避免用户表随每次跑测试持续膨胀。"""
    try:
        session.put(f"{GATEWAY_URL}/api/users/{user_id}/status",
                    params={"status": "disabled"}, timeout=15)
    except Exception:
        pass


def first_borrowable_book(session: requests.Session) -> int:
    """挑一本 status=available 且库存 >0 的图书用于真实借阅。"""
    resp = session.get(f"{GATEWAY_URL}/api/books",
                       params={"page": 1, "per_page": 100}, timeout=15)
    records = resp.json()["data"]["records"]
    for book in records:
        if book.get("status") == "available" and (book.get("stock") or 0) > 0:
            return book["id"]
    raise AssertionError("环境里没有可借图书，无法触发真实借阅事件")


def login_as(username: str, password: str) -> requests.Session:
    """以指定用户登录并返回带其 token 的会话。

    为什么必须用用户自己的 token：借阅接口的用户身份**取自 token**，请求体里的 userId 会被忽略。
    用管理员 token 借书会记在管理员名下（演示账号往往已借满 5 本）→ 返回 3003。
    """
    session = requests.Session()
    session.trust_env = False
    session.headers.update({"Content-Type": "application/json"})
    resp = session.post(f"{GATEWAY_URL}/api/auth/login",
                        json={"username": username, "password": password}, timeout=15)
    token = (resp.json().get("data") or {}).get("accessToken")
    if not token:
        raise AssertionError(f"用户 {username} 登录失败：{resp.text[:200]}")
    session.headers["Authorization"] = f"Bearer {token}"
    return session


def create_reader_and_login(session: requests.Session) -> tuple[int, requests.Session]:
    """建专用读者并返回 (userId, 该读者的会话)。专用账号保证不在额度上限上。"""
    username = f"mq_reader_{uuid.uuid4().hex[:8]}"
    password = "Passw0rd!"
    resp = session.post(f"{GATEWAY_URL}/api/users", json={
        "username": username,
        "password": password,
        "realName": "MQ 事件捕获专用读者",
        "userType": "reader",
    }, timeout=15)
    body = resp.json()
    if body.get("code") != 200:
        raise AssertionError(f"创建专用读者失败：{body}")
    return body["data"]["userId"], login_as(username, password)


def declare_probe_queue_with_dlx(channel, source_queue: str) -> str:
    """按 source_queue 的真实 DLX 参数声明一个测试私有队列，返回队列名。

    为什么不直接往 queue.borrow.success 投递再 basic_get：那个队列上挂着 notify-service 的
    在线消费者，消息会被它先抢走，测试永远取不到（"竞争消费者"问题）。
    这里读取生产队列**实际生效**的 DLX 参数，原样应用到一个测试私有队列上，
    既忠实又不会被抢。
    """
    info = get_queue(source_queue) or {}
    args = {k: v for k, v in (info.get("arguments") or {}).items()
            if k.startswith("x-dead-letter")}
    name = channel.queue_declare(queue="", exclusive=True, auto_delete=True,
                                 arguments=args).method.queue
    return name
