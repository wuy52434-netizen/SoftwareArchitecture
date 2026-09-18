"""页面对象基类：统一导航、testid 定位与通用断言。"""
from __future__ import annotations

from playwright.sync_api import Page, expect


class BasePage:
    """所有页面对象的基类。

    定位策略：**只允许使用 ``data-testid``**，不使用 class / 层级选择器。
    原因见 README「定位策略」——前端重构样式时 class 会变，testid 不会。
    """

    #: 子类覆写，用于 goto() 无参调用
    path: str = "/"

    def __init__(self, page: Page):
        self.page = page

    # ------------------------------------------------------------ 定位
    def testid(self, value: str):
        """按 data-testid 定位（Playwright 默认 test id 属性即 data-testid）。"""
        return self.page.locator(f'[data-testid="{value}"]')

    def testid_all(self, value: str):
        return self.page.locator(f'[data-testid="{value}"]')

    # ------------------------------------------------------------ 导航
    def goto(self, path: str | None = None) -> "BasePage":
        self.page.goto(path or self.path, wait_until="domcontentloaded")
        return self

    def expect_url(self, pattern: str) -> None:
        expect(self.page).to_have_url(pattern)

    def title(self) -> str:
        return self.page.title()
