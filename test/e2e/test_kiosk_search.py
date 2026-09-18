"""UI 自动化 · 借书机 Elasticsearch 检索链路。

对应前端 `frontend/src/views/kiosk/Search.vue` → `searchApi.searchBooks` → `GET /api/search`
（**Elasticsearch 检索**）。

这是全项目唯一一条真正走 ES 的前端链路，因此也是唯一能在 UI 层暴露
「索引与数据库不一致」的用户可见入口：读者在 MySQL 里明明有这本书，
在借书机的检索框里却搜不到。

设计要点（上一版的两个毛病一起修掉）：
1. 旧版取"门户列表第一本"再去借书机搜，而列表第一本恰好是历史测试造出来的、
   **从未进入 ES 索引**的书 —— 于是用例时红时绿，取决于数据顺序。现在：
   * 正向用例固定搜一本**确定已被索引**的种子书（`三体`，id=1，seed 时写入 ES）；
   * 缺陷探针改成**主动新建一本**唯一标题的书，再断言借书机搜得到 —— 结果确定。
2. 新建的书用完即删，避免继续往环境里堆垃圾数据。
"""
from __future__ import annotations

import time

import pytest
import requests
from playwright.sync_api import expect

from pages.books_page import BooksPage
from pages.kiosk_search_page import KioskSearchPage

GATEWAY = "http://127.0.0.1:8080"
ADMIN_USER = "admin"
ADMIN_PASS = "admin123"

#: 确定同时存在于 MySQL 与 ES 索引的种子书（compose 初始化 + seed_es_books.py 都写入了它）
SEEDED_TITLE = "三体"


def _admin_api() -> requests.Session:
    """直连网关的管理员 API 会话（直连：禁止继承宿主机 HTTP_PROXY）。"""
    s = requests.Session()
    s.trust_env = False
    s.headers.update({"Content-Type": "application/json"})
    resp = s.post(f"{GATEWAY}/api/auth/login",
                  json={"username": ADMIN_USER, "password": ADMIN_PASS}, timeout=15)
    token = (resp.json().get("data") or {}).get("accessToken")
    assert token, f"管理员登录失败：{resp.text[:200]}"
    s.headers["Authorization"] = f"Bearer {token}"
    return s


@pytest.fixture
def new_book():
    """新建一本唯一标题的图书，测完删除（避免污染环境）。"""
    api = _admin_api()
    created: list[int] = []

    def _create(title: str) -> int:
        resp = api.post(f"{GATEWAY}/api/books", json={
            "isbn": f"978-7-{int(time.time() * 1000) % 10_000_000:07d}",
            "title": title,
            "author": "借书机检索专项",
            "categoryId": 1,
            "price": 42.00,
            "summary": "借书机 ES 召回专项测试种下的数据",
        }, timeout=15)
        assert resp.status_code == 200, f"创建图书失败 status={resp.status_code} {resp.text[:200]}"
        body = resp.json()
        assert body.get("code") == 200, f"创建图书业务码异常：{body}"
        book_id = body["data"]["id"]
        created.append(book_id)
        return book_id

    yield _create

    for book_id in created:
        api.delete(f"{GATEWAY}/api/books/{book_id}", timeout=15)


def test_kiosk_search_page_renders_controls(admin_page):
    """借书机检索页应渲染出搜索框与搜索按钮。"""
    page = KioskSearchPage(admin_page).open()

    expect(page.search_input).to_be_visible()
    expect(page.submit_button).to_be_visible()


def test_kiosk_search_finds_seeded_book(admin_page):
    """正向基线：已在索引里的种子书，借书机必须搜得到。

    这条**必须稳定通过** —— 它是"借书机检索链路本身是通的"的证明；
    没有它，下面那条 xfail 就无法排除"接口全坏"这种解释。
    """
    kiosk = KioskSearchPage(admin_page).open()
    kiosk.search(SEEDED_TITLE)

    assert kiosk.total_count_value() >= 1, (
        f"种子书『{SEEDED_TITLE}』在借书机（ES）里搜不到，检索链路本身有问题"
    )


def test_newly_added_book_is_searchable_in_kiosk(admin_page, new_book):
    """用户可见影响：刚上架的新书，借书机也必须搜得到。

    已修复（KNWN-ES-01）：book-service 现在在图书写入后发 book.exchange 事件，
    search-service 消费并同步 ES 索引，因此本用例应当稳定通过。
    它同时也是"同步链路端到端可用"的用户视角验证 —— 比直接查 ES 更有说服力。

    步骤严格对齐真实用户动作：
    1. 通过接口上架一本唯一标题的书（标题含时间戳，避免与历史数据撞名）；
    2. 门户（MySQL 链路）确认这本书搜得到 —— 排除"压根没上架成功"；
    3. 借书机（ES 链路）搜同一个标题 —— 期望搜得到。
    """
    title = f"EB同步探针{int(time.time())}"
    new_book(title)

    books = BooksPage(admin_page).open()
    books.search(title)
    mysql_total = books.total_count_value()
    assert mysql_total >= 1, (
        f"新建的书『{title}』在门户（MySQL）里都搜不到，创建接口没写库，"
        f"本用例的前提不成立"
    )

    # 同步是异步的：轮询等待索引追上，而不是立刻断言（否则会偶发假失败）
    kiosk = KioskSearchPage(admin_page).open()
    deadline = time.time() + 20
    es_total = 0
    while time.time() < deadline:
        kiosk.search(title)
        es_total = kiosk.total_count_value()
        if es_total >= mysql_total:
            break
        time.sleep(1.5)

    assert es_total == mysql_total, (
        f"『{title}』在门户（MySQL）命中 {mysql_total} 条，"
        f"借书机（ES）命中 {es_total} 条 —— 新书没有同步进索引，"
        f"读者在借书机上搜不到刚上架的书"
    )
