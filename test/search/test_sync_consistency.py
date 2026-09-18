"""ES 专项 · MySQL → Elasticsearch 数据同步一致性。

这是本项目检索链路上最严重的一类风险：
用户新建一本书之后，**能不能马上搜到**？

代码事实（已核实）：
* ``book-service`` 全模块**不存在任何 ES 相关代码**（无 EsClient、无 search-service 调用）；
* ``SearchService.indexBook()`` 定义了写入方法，但全仓库 grep 不到调用方；
* ES 索引只能靠手工执行 ``seed_es_books.py`` 一次性灌库。

因此推断：**ES 是数据库的一份"过期快照"**，运行期的新增/修改/删除都不会反映到索引里。
本文件用"先证明接口确实写成功、再断言检索不到"的方式把这条结论坐实。
"""
from __future__ import annotations

import time

import pytest

import search_helper as sh


@pytest.fixture
def api_books(gateway, es_client):
    """经网关创建图书，用例结束后同时清理 MySQL 与 ES 两侧的残留。"""
    created: list[int] = []

    def _create(title: str, **extra) -> int:
        payload = {
            "isbn": f"978-7-{int(time.time() * 1000) % 10_000_000:07d}",
            "title": title,
            "author": "同步专项测试",
            "categoryId": 1,
            "price": 59.90,
            "summary": "ES 同步一致性专项测试种下的数据",
        }
        payload.update(extra)
        resp = gateway.post("/api/books", json=payload)
        assert resp.status_code == 200, (
            f"创建图书失败 status={resp.status_code} body={resp.text[:300]}"
        )
        body = resp.json()
        assert body.get("code") == 200, f"创建图书业务码异常：{body}"
        book_id = body["data"]["id"]
        created.append(book_id)
        return book_id

    yield _create

    for book_id in created:
        gateway.request("DELETE", f"/api/books/{book_id}")
        try:
            es_client.delete_doc(book_id)
        except Exception:
            pass
    es_client.refresh()


def _wait_in_index(es_client, book_id: int, timeout: float | None = None) -> dict | None:
    """轮询等待文档出现在索引中（ES 近实时 + 异步同步延迟）。"""
    deadline = time.time() + (timeout if timeout is not None else sh.SYNC_WAIT_SECONDS)
    while time.time() < deadline:
        es_client.refresh()
        resp = es_client.search({"query": {"ids": {"values": [str(book_id)]}}, "size": 1})
        hits = resp["hits"]["hits"]
        if hits:
            return hits[0]
        time.sleep(0.3)
    return None


def _wait_gone_from_index(es_client, book_id: int, timeout: float | None = None) -> bool:
    """轮询等待文档从索引消失。

    同步是异步的（图书写入 → MQ → 消费端写 ES），不能"删完立刻查一次"就断言：
    那一刻索引里当然还在，用例会偶发假失败。
    """
    deadline = time.time() + (timeout if timeout is not None else sh.SYNC_WAIT_SECONDS)
    while time.time() < deadline:
        es_client.refresh()
        resp = es_client.search({"query": {"ids": {"values": [str(book_id)]}}, "size": 1})
        if not resp["hits"]["hits"]:
            return True
        time.sleep(0.3)
    return False


def _wait_title_in_index(es_client, book_id: int, expected_title: str,
                         timeout: float | None = None) -> str | None:
    """轮询等待索引里的标题变成期望值，返回实际标题。

    同样不能用"文档存在"当作收敛条件：更新事件还没被消费时，文档存在但内容还是旧的。
    """
    deadline = time.time() + (timeout if timeout is not None else sh.SYNC_WAIT_SECONDS)
    actual = None
    while time.time() < deadline:
        es_client.refresh()
        resp = es_client.search({"query": {"ids": {"values": [str(book_id)]}}, "size": 1})
        hits = resp["hits"]["hits"]
        if hits:
            actual = hits[0]["_source"].get("title")
            if actual == expected_title:
                return actual
        time.sleep(0.3)
    return actual


def test_book_creation_api_actually_persists(api_books):
    """前提事实：创建接口确实写库成功并返回了主键。

    "写库成功"与"是否进了索引"是两件事，必须分开验证 —— 否则一旦检索不到，
    无法区分是"接口没写成功"还是"ES 没同步"，根因就定不下来。
    """
    title = f"{sh.SEED_PREFIX}创建接口自证{int(time.time())}"
    book_id = api_books(title=title)

    assert isinstance(book_id, int) and book_id > 0, f"创建接口未返回有效主键：{book_id}"


def test_new_book_becomes_searchable_after_creation(gateway, es_client, api_books):
    """新建图书后应能在检索结果中找到（核心同步契约）。

    已修复（KNWN-ES-01）：book-service 现在在图书写入后发出 book.exchange 事件，
    search-service 消费并写 ES 索引。
    """
    title = f"{sh.SEED_PREFIX}同步验证{int(time.time())}"
    book_id = api_books(title=title)

    hit = _wait_in_index(es_client, book_id)
    assert hit is not None, (
        f"新建图书 id={book_id} title={title} 在 {sh.SYNC_WAIT_SECONDS}s 内未进入 "
        f"{sh.INDEX_BOOKS} 索引，用户搜索不到刚上架的新书"
    )
    assert hit["_source"]["title"] == title, "索引中的标题与库中不一致"


def test_updated_title_is_reflected_in_index(gateway, es_client, api_books):
    """修改图书后，索引中的字段应随之更新（已修复 KNWN-ES-01）。"""
    title = f"{sh.SEED_PREFIX}更新前{int(time.time())}"
    book_id = api_books(title=title)

    new_title = f"{sh.SEED_PREFIX}更新后{int(time.time())}"
    resp = gateway.request("PUT", f"/api/books/{book_id}", json={"title": new_title})
    assert resp.status_code == 200, f"更新图书失败：{resp.text[:200]}"

    actual = _wait_title_in_index(es_client, book_id, new_title)
    assert actual == new_title, (
        f"索引中标题仍为『{actual}』，期望『{new_title}』，"
        f"检索结果与详情页不一致"
    )


def test_deleted_book_is_removed_from_index(gateway, es_client, api_books):
    """删除图书后，索引中的对应文档应被移除。

    注意不要写成"只断言删除后不在索引"—— 在 DB→ES 同步完全缺失（KNWN-ES-01）时，
    新建图书也从未进过索引，"删除后不在索引"会被空断言满足，用例因**错误的原因**通过
    （曾表现为 strict xfail 被 XPASS 判 FAILED）。因此先钉住前置条件：新建后必须真的进过索引。
    """
    title = f"{sh.SEED_PREFIX}删除验证{int(time.time())}"
    book_id = api_books(title=title)

    # 前置条件：必须先证明它进过索引，否则"删除同步"这条断言没有任何鉴别力
    indexed_before = _wait_in_index(es_client, book_id)
    assert indexed_before is not None, (
        f"图书 id={book_id} 新建后从未进入索引，无法验证删除同步（前置条件不成立）"
    )

    resp = gateway.request("DELETE", f"/api/books/{book_id}")
    assert resp.status_code == 200, f"删除图书失败：{resp.text[:200]}"

    gone = _wait_gone_from_index(es_client, book_id)
    assert gone, (
        f"图书 id={book_id} 已从库中删除，但 {sh.SYNC_WAIT_SECONDS}s 后索引中仍存在，"
        f"检索会返回一个点进去就 404 的幽灵记录"
    )
