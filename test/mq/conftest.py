"""RabbitMQ 专项测试公共夹具。

运行前置：``docker compose -f docker/docker-compose.yml up -d rabbitmq`` 并确保
notify-service / stats-service 已启动（拓扑由 Spring AMQP 在应用启动时声明）。

broker 不可达时整个目录自动跳过，不会在无环境时产生假失败。
"""
from __future__ import annotations

import pytest

import mq_helper as mq


@pytest.fixture(scope="session")
def broker() -> str:
    """AMQP 连通性哨兵；不可达直接跳过整个测试模块。"""
    if not mq.broker_alive():
        pytest.skip(f"RabbitMQ 不可达 ({mq.RABBIT_HOST}:{mq.RABBIT_PORT})，"
                    f"请先执行 docker compose up -d rabbitmq")
    return f"{mq.RABBIT_HOST}:{mq.RABBIT_PORT}"


@pytest.fixture(scope="session")
def mgmt(broker) -> dict:
    """拓扑内省能力哨兵；Management API 不可用则跳过依赖拓扑的用例。"""
    if not mq.management_alive():
        pytest.skip(f"RabbitMQ Management API 不可达 ({mq.MGMT_URL})，"
                    f"拓扑断言需要 15672 端口开放")
    return {"url": mq.MGMT_URL}


@pytest.fixture
def clean_queues(broker):
    """用例前置：清空测试会碰到的队列，避免上一轮残留消息造成误判。"""
    for queue in mq.ALL_QUEUES + (mq.QUEUE_DEAD_LETTER,):
        mq.only_purge_if_exists(queue)
    yield
    for queue in mq.ALL_QUEUES + (mq.QUEUE_DEAD_LETTER,):
        mq.only_purge_if_exists(queue)
