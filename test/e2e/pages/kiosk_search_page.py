"""借书机检索页页面对象。

对应前端 `frontend/src/views/kiosk/Search.vue`，路由 `/kiosk/search`。

注意：这个页面走的是 **Elasticsearch 检索链路**（``searchApi.searchBooks`` → `GET /api/search`），
与读者门户 ``portal/Books.vue`` 走的 MySQL ``/api/books`` 是两条完全不同的实现，
因此两条链路必须分别覆盖。
"""
from __future__ import annotations

from playwright.sync_api import expect

from .base_page import BasePage

#: 借书机检索接口（Elasticsearch 链路）。
SEARCH_API = "/api/search"


class KioskSearchPage(BasePage):
    path = "/kiosk/search"

    @property
    def search_input(self):
        return self.testid("kiosk-search-input")

    @property
    def submit_button(self):
        return self.testid("kiosk-search-submit")

    @property
    def results_section(self):
        return self.testid("kiosk-search-results")

    @property
    def total_count(self):
        return self.testid("kiosk-search-total")

    def open(self) -> "KioskSearchPage":
        self.goto()
        expect(self.search_input).to_be_visible()
        return self

    def search(self, keyword: str) -> "KioskSearchPage":
        """输入关键词并检索，**以 /api/search 响应返回为收敛信号**。

        不能只等 ``kiosk-search-results`` 出现：该区块在 ``searched=true`` 时立刻渲染，
        此时 ``pagination.total`` 还是初始值 0，读到 0 会误判"搜不到"。
        另外 ``.el-loading-mask`` 在过渡期会同时存在两个节点，必须用 ``.first``，
        否则触发 Playwright strict mode 报错。
        """
        self.search_input.fill(keyword)
        with self.page.expect_response(lambda r: SEARCH_API in r.url, timeout=20_000):
            self.submit_button.click()
        expect(self.results_section).to_be_visible(timeout=10_000)
        self.page.locator(".el-loading-mask").first.wait_for(state="hidden", timeout=10_000)
        return self

    def total_count_value(self) -> int:
        return int(self.total_count.inner_text().strip())
