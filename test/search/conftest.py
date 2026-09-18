"""Elasticsearch 专项测试公共夹具。

ES 是近实时引擎（默认 1s refresh），涉及写入的断言统一走 ``wait_for_indexing`` 轮询，
避免用 sleep 硬等造成的偶发失败。

环境不可达时按层跳过：只跑 mapping/分词用例不需要网关，只跑接口用例不需要直连 ES，
所以 ``es_client`` 与 ``gateway`` 是两个独立哨兵。
"""
from __future__ import annotations

import pytest

import search_helper as sh


@pytest.fixture(scope="module")
def es_client() -> sh.EsClient:
    """ES 直连客户端；9200 不可达则跳过。"""
    if not sh.es_alive():
        pytest.skip(f"Elasticsearch 不可达 ({sh.ES_URL})，请先执行 "
                    f"docker compose up -d elasticsearch")
    client = sh.EsClient()
    if not client.index_exists():
        pytest.skip(f"索引 {sh.INDEX_BOOKS} 不存在，请先启动 search-service 或执行 seed_es_books.py")
    return client


@pytest.fixture(scope="module")
def gateway() -> sh.GatewayClient:
    """网关客户端（已登录）；8080 不可达或登录失败则跳过。"""
    if not sh.gateway_alive():
        pytest.skip(f"API 网关不可达 ({sh.GATEWAY_URL})，请先启动后端服务")
    client = sh.GatewayClient()
    if not client.login():
        pytest.skip("管理员登录失败，请确认种子账号 admin/admin123 存在")
    return client


@pytest.fixture
def seeded_books(es_client):
    """注入带 kwn_ 前缀的种子图书，用例结束后清理，不污染真实书库。"""
    created: list[int] = []

    def _seed(**kwargs) -> dict:
        doc_id = kwargs.pop("doc_id", 900000 + len(created) + 1)
        doc = es_client.seed_book(doc_id, **kwargs)
        created.append(doc_id)
        return doc

    yield _seed

    for doc_id in created:
        es_client.delete_doc(doc_id)
    es_client.refresh()
    es_client.cleanup_seed_docs()
