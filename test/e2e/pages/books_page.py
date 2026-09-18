"""读者门户图书检索页页面对象。

对应前端 `frontend/src/views/portal/Books.vue`，路由 `/portal/books`（需登录）。
"""
from __future__ import annotations

from playwright.sync_api import expect, Page

from .base_page import BasePage

#: 图书列表接口。页面挂载/检索都打这个地址。
BOOKS_API = "/api/books"


class BooksPage(BasePage):
    path = "/portal/books"

    # ------------------------------------------------------------ 元素
    @property
    def search_input(self):
        return self.testid("books-search-input")

    @property
    def total_count(self):
        return self.testid("books-total-count")

    @property
    def grid(self):
        return self.testid("books-grid")

    @property
    def book_cards(self):
        return self.testid("book-card")

    @property
    def book_titles(self):
        return self.testid("book-card-title")

    @property
    def empty_state(self):
        return self.testid("books-empty-state")

    @property
    def pagination(self):
        return self.testid("books-pagination")

    # ------------------------------------------------------------ 行为
    def open(self) -> "BooksPage":
        # 首次挂载就会请求图书列表；只等 DOM 会出现竞态：
        # pagination.total 初值为 0，响应回来后才更新，等到 DOM 就读会误判"书库为空"。
        with self.page.expect_response(lambda r: BOOKS_API in r.url, timeout=20_000):
            self.goto()
        self.expect_loaded()
        self.wait_settled()
        return self

    def expect_loaded(self) -> None:
        expect(self.search_input).to_be_visible()
        expect(self.search_input).to_be_editable()

    def search(self, keyword: str) -> "BooksPage":
        """输入关键词并回车，以 /api/books 响应返回为准。"""
        self.search_input.fill(keyword)
        with self.page.expect_response(lambda r: BOOKS_API in r.url, timeout=20_000):
            self.search_input.press("Enter")
        self.wait_settled()
        return self

    def clear_search(self) -> "BooksPage":
        self.search_input.fill("")
        with self.page.expect_response(lambda r: BOOKS_API in r.url, timeout=20_000):
            self.search_input.press("Enter")
        self.wait_settled()
        return self

    def wait_settled(self, timeout: int = 10_000) -> None:
        """等待 el-loading 遮罩消失。

        注意：**只有**在已经用 ``expect_response`` 确认过数据返回之后调用它才有意义。
        ``wait_for(state="hidden")`` 在遮罩尚未出现时会立刻返回（hidden 包含"不存在"），
        单独用它等数据是不可靠的 —— 这正是之前 5 条用例被误判为"书库为空"的原因。
        另外过渡期会同时存在两个 mask 节点，必须用 ``.first`` 避免 strict mode 报错。
        """
        self.page.locator(".el-loading-mask").first.wait_for(state="hidden", timeout=timeout)

    # ------------------------------------------------------------ 断言辅助
    def total_count_value(self) -> int:
        return int(self.total_count.inner_text().strip())

    def require_catalog(self) -> int:
        """返回图书总数；为 0 时直接**失败**。

        这里不再 ``pytest.skip``：书库为空是环境未准备好，把它 skip 掉会让
        "检索链路"整批用例静默变绿，失去意义。空库就该红着提醒去补数据。
        """
        total = self.total_count_value()
        assert total > 0, (
            "书库为空，无法验证检索链路。请先准备数据："
            "首次启动由 docker-compose 初始化脚本导入 MySQL 种子数据；"
            "若为二次启动，请确认 books 表非空。"
        )
        return total

    def rendered_titles(self) -> list[str]:
        return [t.strip() for t in self.book_titles.all_inner_texts()]
