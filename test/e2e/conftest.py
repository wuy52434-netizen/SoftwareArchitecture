"""Playwright UI 自动化公共夹具。

前置条件（二选一）：
* **容器形态（默认，推荐）**：``docker-compose up -d nginx api-gateway ...``，
  前端由 nginx 在 **8090** 端口提供，且 nginx 已把 ``/api/`` 反代到网关（同源，无需 CORS）。
  ``frontend/dist`` 必须存在且是最新构建（``cd frontend && npm run build``）。
* **开发形态**：``cd frontend && npm run dev``（Vite 默认 3000），
  此时用 ``WEB_BASE_URL=http://127.0.0.1:3000`` 覆盖。

前端不可达时整个套件自动跳过。

选择器策略详见 README：只使用 ``data-testid``。
"""
from __future__ import annotations

import os
import socket
from urllib.parse import urlparse

import pytest

from pages.books_page import BooksPage
from pages.login_page import LoginPage

# 默认对准 compose 里的 nginx（8090）。此前默认写 3000，与容器形态对不上，
# 表现为 13 个 setup error（"前端不可达"），而实际前端是活的、只是端口不对。
APP_URL = os.getenv("WEB_BASE_URL", "http://127.0.0.1:8090")
ADMIN_USER = os.getenv("ADMIN_USER", "admin")
ADMIN_PASS = os.getenv("ADMIN_PASS", "admin123")


def _reachable(url: str, timeout: float = 2.0) -> bool:
    parsed = urlparse(url)
    try:
        with socket.create_connection((parsed.hostname or "127.0.0.1", parsed.port or 80), timeout=timeout):
            return True
    except OSError:
        return False


@pytest.fixture(scope="session", autouse=True)
def _web_env_guard():
    """前端不可达则整体跳过，避免在无环境时产生一堆假失败。"""
    if not _reachable(APP_URL):
        pytest.skip(
            f"前端不可达（{APP_URL}）。容器形态请先执行 "
            f"`docker-compose up -d nginx`（需 frontend/dist 已构建）；"
            f"开发形态请 `cd frontend && npm run dev` 并设 WEB_BASE_URL=http://127.0.0.1:3000"
        )


@pytest.fixture(scope="session")
def browser_context_args(browser_context_args):
    """统一注入 base_url 与视口，让页面对象可以直接用相对路径 goto。"""
    return {
        **browser_context_args,
        "base_url": APP_URL,
        "viewport": {"width": 1440, "height": 900},
        "ignore_https_errors": True,
    }


@pytest.fixture
def login_page(page) -> LoginPage:
    return LoginPage(page).open()


@pytest.fixture
def admin_page(page, login_page) -> "object":
    """已完成管理员登录的页面，可直接访问需鉴权的路由。"""
    login_page.login_expect_success(ADMIN_USER, ADMIN_PASS)
    page.wait_for_url(lambda url: "/login" not in url, timeout=15_000)
    return page


@pytest.fixture
def books_page(admin_page) -> BooksPage:
    """已登录状态下的图书检索页（open() 内部已等到 /api/books 响应返回）。"""
    return BooksPage(admin_page).open()
