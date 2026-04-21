import os
import json
import uuid
from contextlib import contextmanager
from redis import Redis
from redis.exceptions import RedisError


_redis_client = None


def get_redis_client():
    """获取 Redis 客户端（单例）。连接失败时返回 None，不阻塞主流程。"""
    global _redis_client
    if _redis_client is not None:
        return _redis_client

    redis_url = os.environ.get('REDIS_URL', 'redis://127.0.0.1:6379/0')
    try:
        client = Redis.from_url(redis_url, decode_responses=True)
        client.ping()
        _redis_client = client
        return _redis_client
    except Exception:
        return None


def redis_get_json(key):
    client = get_redis_client()
    if not client:
        return None
    try:
        value = client.get(key)
        return json.loads(value) if value else None
    except (RedisError, json.JSONDecodeError, TypeError):
        return None


def redis_set_json(key, value, ex=60):
    client = get_redis_client()
    if not client:
        return False
    try:
        client.set(key, json.dumps(value, ensure_ascii=False), ex=ex)
        return True
    except (RedisError, TypeError):
        return False


def redis_delete_pattern(pattern):
    client = get_redis_client()
    if not client:
        return
    try:
        keys = client.keys(pattern)
        if keys:
            client.delete(*keys)
    except RedisError:
        pass


@contextmanager
def redis_lock(lock_key, timeout=5, blocking_timeout=2):
    """Redis 分布式锁。Redis 不可用时自动降级为无锁。"""
    client = get_redis_client()
    if not client:
        yield True
        return

    token = str(uuid.uuid4())
    acquired = False
    try:
        acquired = client.set(lock_key, token, nx=True, ex=timeout)
        if not acquired:
            yield False
            return
        yield True
    finally:
        if acquired:
            try:
                if client.get(lock_key) == token:
                    client.delete(lock_key)
            except RedisError:
                pass
