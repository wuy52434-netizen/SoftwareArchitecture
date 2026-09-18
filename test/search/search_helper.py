"""Elasticsearch 专项测试辅助模块。

两层被测对象：

* **ES 直连层**（``http://127.0.0.1:9200``）：索引 / mapping / 分词器 / 检索 DSL 行为。
  直连才能验证"分词器有没有生效""权重有没有配"这类底层契约，
  经服务层只能看到最终结果，定位不到根因。
* **服务层**（``http://127.0.0.1:8080`` 网关 → search-service）：``/api/search`` 的
  对外行为与"MySQL 改了 ES 跟没跟"的数据一致性。

设计原则：种子文档一律用 ``kwn_`` 前缀，用例结束即清理，不污染真实书库。
"""
from __future__ import annotations

import os
import re
import socket
import time
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

import requests

# ---------------------------------------------------------------- 连接参数
ES_URL = os.getenv("ES_URL", "http://127.0.0.1:9200").rstrip("/")
GATEWAY_URL = os.getenv("BASE_URL", "http://127.0.0.1:8080").rstrip("/")
ADMIN_USER = os.getenv("ADMIN_USER", "admin")
ADMIN_PASS = os.getenv("ADMIN_PASS", "admin123")

INDEX_BOOKS = "books"
SEED_PREFIX = "kwn_"          # 本套件注入的种子文档统一前缀，便于清理
#: 索引同步的等待上限。链路是"图书写入 → MQ 事件 → 消费端回查 → 写 ES → ES 刷新"，
#: 端到端要跨 4 个环节，原来 3s 对异步链路偏紧、容易偶发假失败，放宽到 10s。
SYNC_WAIT_SECONDS = float(os.getenv("ES_SYNC_WAIT", "10"))


# ---------------------------------------------------------------- 连通性
def _tcp_alive(host: str, port: int, timeout: float = 2.0) -> bool:
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return True
    except OSError:
        return False


def es_alive() -> bool:
    parsed = urlparse(ES_URL)
    return _tcp_alive(parsed.hostname or "127.0.0.1", parsed.port or 9200)


def gateway_alive() -> bool:
    parsed = urlparse(GATEWAY_URL)
    return _tcp_alive(parsed.hostname or "127.0.0.1", parsed.port or 8080)


# ---------------------------------------------------------------- ES 直连客户端
class EsClient:
    def __init__(self, base_url: str = ES_URL):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
        # 直连本机 ES，禁止继承宿主机 HTTP_PROXY。
        # 走代理时请求行会变成 absolute-form，ES 直接回
        # `no handler found for uri [http://127.0.0.1:9200/books/_search] and method [POST]`，
        # 而且时好时坏（同一请求前一次 200、后一次 400）。
        self.session.trust_env = False

    def get(self, path: str, **kw) -> requests.Response:
        return self.session.get(self.base_url + path, timeout=10, **kw)

    def post(self, path: str, json: Any = None, **kw) -> requests.Response:
        return self.session.post(self.base_url + path, json=json, timeout=15, **kw)

    def put(self, path: str, json: Any = None, **kw) -> requests.Response:
        return self.session.put(self.base_url + path, json=json, timeout=15, **kw)

    def delete(self, path: str, **kw) -> requests.Response:
        return self.session.delete(self.base_url + path, timeout=15, **kw)

    # --- 便捷方法 ---------------------------------------------------
    def index_exists(self, name: str = INDEX_BOOKS) -> bool:
        return self.get(f"/{name}").status_code == 200

    def mapping(self, name: str = INDEX_BOOKS) -> dict:
        resp = self.get(f"/{name}/_mapping")
        resp.raise_for_status()
        return resp.json().get(name, {}).get("mappings", {})

    def count(self, query: dict | None = None, name: str = INDEX_BOOKS) -> int:
        body = {"query": query or {"match_all": {}}}
        resp = self.post(f"/{name}/_count", json=body)
        resp.raise_for_status()
        return int(resp.json().get("count", 0))

    def search(self, body: dict, name: str = INDEX_BOOKS) -> dict:
        resp = self.post(f"/{name}/_search", json=body)
        resp.raise_for_status()
        return resp.json()

    def analyze(self, text: str, analyzer: str, name: str = INDEX_BOOKS) -> list[str]:
        resp = self.post(f"/{name}/_analyze", json={"analyzer": analyzer, "text": text})
        resp.raise_for_status()
        return [t["token"] for t in resp.json().get("tokens", [])]

    def refresh(self, name: str = INDEX_BOOKS) -> None:
        self.post(f"/{name}/_refresh")

    # --- 种子文档 ---------------------------------------------------
    def seed_book(self, doc_id: int, *, title: str, author: str = "测试作者",
                  summary: str = "测试摘要", category: str = "测试分类",
                  status: str = "AVAILABLE", **extra) -> dict:
        payload = {
            "id": doc_id,
            "title": title,
            "author": author,
            "summary": summary,
            "category": category,
            "status": status,
            "isbn": f"978-7-{doc_id:07d}",
            "publisher": "测试出版社",
            "publishDate": 2024,
            "createdAt": "2026-01-01T00:00:00",
        }
        payload.update(extra)
        resp = self.put(f"/{INDEX_BOOKS}/_doc/{doc_id}", json=payload)
        resp.raise_for_status()
        return payload

    def delete_doc(self, doc_id: int) -> None:
        self.delete(f"/{INDEX_BOOKS}/_doc/{doc_id}")

    def cleanup_seed_docs(self) -> int:
        """删除所有 kwn_ 前缀的种子文档，返回删除条数。"""
        body = {"query": {"prefix": {"title": SEED_PREFIX}}, "size": 200}
        try:
            resp = self.search(body)
        except requests.HTTPError:
            return 0
        hits = resp.get("hits", {}).get("hits", [])
        for hit in hits:
            self.delete_doc(hit["_id"])
        if hits:
            self.refresh()
        return len(hits)


def wait_for_indexing(client: EsClient, expected: int, timeout: float = SYNC_WAIT_SECONDS) -> int:
    """ES 是近实时（默认 1s refresh），轮询等待文档数达标。"""
    deadline = time.time() + timeout
    count = 0
    while time.time() < deadline:
        client.refresh()
        count = client.count()
        if count >= expected:
            return count
        time.sleep(0.2)
    return count


# ---------------------------------------------------------------- 网关客户端
class GatewayClient:
    """经 API 网关调用业务接口的轻量客户端。"""

    def __init__(self, base_url: str = GATEWAY_URL):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
        # 直连本机网关，禁止继承宿主机 HTTP_PROXY（理由同 EsClient）
        self.session.trust_env = False
        self.token: str | None = None
        self.timeout = 15

    def _headers(self) -> dict:
        return {"Authorization": f"Bearer {self.token}"} if self.token else {}

    def request(self, method: str, path: str, **kw) -> requests.Response:
        kw.setdefault("timeout", self.timeout)
        headers = dict(kw.pop("headers", {}))
        headers.update(self._headers())
        return self.session.request(method, self.base_url + path, headers=headers, **kw)

    def get(self, path: str, **kw) -> requests.Response:
        return self.request("GET", path, **kw)

    def post(self, path: str, json=None, **kw) -> requests.Response:
        return self.request("POST", path, json=json, **kw)

    def login(self, username: str = ADMIN_USER, password: str = ADMIN_PASS) -> bool:
        resp = self.post("/api/auth/login", json={"username": username, "password": password})
        if resp.status_code != 200:
            return False
        body = resp.json()
        if body.get("code") == 200:
            self.token = body["data"]["accessToken"]
            return True
        return False

    def search_books(self, **params) -> requests.Response:
        clean = {k: v for k, v in params.items() if v is not None}
        return self.get("/api/search", params=clean)


# ---------------------------------------------------------------- 实体源码解析
#: 代码实体（唯一事实来源之一）。索引 mapping 必须与它一致。
BOOK_DOCUMENT_JAVA = (Path(__file__).resolve().parents[2]
                      / "backend" / "search-service" / "src" / "main" / "java"
                      / "com" / "library" / "search" / "document" / "BookDocument.java")


def book_document_fields() -> list[str]:
    """从 BookDocument.java 解析出实体声明的字段名。

    刻意解析源码而不是在测试里再抄一份字段清单：抄一份就等于又多了一份会漂移的定义，
    而本次要修的 KNWN-ES-03 正是"测试里写死的期望字段"与"实体真实字段"不一致导致的误判。
    """
    src = BOOK_DOCUMENT_JAVA.read_text(encoding="utf-8")
    return re.findall(r"private\s+[\w.<>\[\]]+\s+(\w+)\s*;", src)


def book_document_analyzers() -> dict[str, dict[str, str]]:
    """解析出每个 Text 字段声明的 analyzer / searchAnalyzer。"""
    src = BOOK_DOCUMENT_JAVA.read_text(encoding="utf-8")
    result: dict[str, dict[str, str]] = {}
    # 匹配形如： @Field(type = FieldType.Text, analyzer = "x", searchAnalyzer = "y")\n private String name;
    pattern = re.compile(
        r'@Field\(([^)]*)\)\s*private\s+[\w.<>\[\]]+\s+(\w+)\s*;',
        re.S,
    )
    for annotation, field in pattern.findall(src):
        conf: dict[str, str] = {}
        m = re.search(r'analyzer\s*=\s*"([^"]+)"', annotation)
        if m:
            conf["analyzer"] = m.group(1)
        m = re.search(r'searchAnalyzer\s*=\s*"([^"]+)"', annotation)
        if m:
            conf["search_analyzer"] = m.group(1)
        if conf:
            result[field] = conf
    return result
