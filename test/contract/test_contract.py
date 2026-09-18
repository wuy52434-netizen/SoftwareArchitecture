"""契约测试：消费者驱动的 API Schema 校验。

对真实运行的服务抽一根"契约绳"：只要响应结构与 JSON Schema 契约冲突即失败，
从而在上下游(网关/前端/借书机)解耦时锁定接口形状不回归。

技术栈：pytest + jsonschema
"""
import os
import requests
import jsonschema
from jsonschema import validate as _jsonschema_validate

from schemas import (RESULT_SCHEMA, BOOK_SCHEMA, BOOKS_PAGE_SCHEMA,
                     LOGIN_SCHEMA, CATEGORY_SCHEMA)

BASE = os.getenv("CONTRACT_BASE_URL", "http://127.0.0.1:8080")


def get(path, **kw):
    return requests.get(BASE + path, timeout=10, **kw).json()


def _check_valid(obj, schema, label):
    try:
        _jsonschema_validate(obj, schema)
    except jsonschema.ValidationError as e:
        raise AssertionError(f"契约校验失败 [{label}]: {e.message}") from e


def test_result_envelope():
    body = get("/api/books?per_page=1")
    _check_valid(body, RESULT_SCHEMA, "Result 信封")


def test_login_contract():
    body = requests.post(BASE + "/api/auth/login", json={
        "username": "admin", "password": "admin123",
    }, timeout=10).json()
    _check_valid(body["data"], LOGIN_SCHEMA, "登录响应")


def test_book_object_contract():
    body = get("/api/books/1")
    assert body["code"] == 200
    _check_valid(body["data"], BOOK_SCHEMA, "图书对象")


def test_book_page_contract():
    body = get("/api/books", params={"per_page": 5})
    assert body["code"] == 200
    _check_valid(body["data"], BOOKS_PAGE_SCHEMA, "图书分页")


def test_categories_contract():
    body = get("/api/books/categories")
    assert body["code"] == 200
    _check_valid(body["data"], CATEGORY_SCHEMA, "分类列表")


def test_search_result_contract():
    """ES 检索结果命中书对象契约。"""
    body = get("/api/search?keyword=三体")
    assert body["code"] == 200
    data = body["data"]
    # search 返回 books 字段
    if isinstance(data, dict) and "books" in data:
        for b in data["books"]:
            _check_valid(b, BOOK_SCHEMA, "搜索结果图书")