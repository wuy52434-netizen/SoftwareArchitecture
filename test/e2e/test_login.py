"""UI 自动化 · 登录链路。

对应前端 `frontend/src/views/Login.vue`。
"""
from __future__ import annotations

import pytest
from playwright.sync_api import expect

from pages.login_page import LoginPage

VALID_ADMIN = "admin"
VALID_PASS = "admin123"
# 必须是**合法长度**（前端规则 6-20）的错误密码。
# 原值 "definitely-wrong-password"（25 位）会被前端长度校验直接拦下，
# 根本走不到后端鉴权——用例名说测"密码错误"，实际测的是"长度校验"（已实测确认）。
WRONG_PASS = "wrongpass123"


def test_login_page_renders_all_required_controls(login_page: LoginPage):
    """登录页必须渲染出用户名、密码、提交按钮三个核心控件。"""
    expect(login_page.username_input).to_be_visible()
    expect(login_page.password_input).to_be_visible()
    expect(login_page.submit_button).to_be_visible()
    expect(login_page.submit_button).to_be_enabled()


def test_valid_admin_login_leaves_login_page(page, login_page: LoginPage):
    """正确凭据应登录成功并跳转离开登录页。"""
    login_page.login_expect_success(VALID_ADMIN, VALID_PASS)

    page.wait_for_url(lambda url: "/login" not in url, timeout=15_000)
    assert "/login" not in page.url, f"登录成功后仍停留在登录页：{page.url}"


def test_wrong_password_shows_error_and_stays_on_login(page, login_page: LoginPage):
    """密码错误必须给出可见的错误提示，且不能放行。"""
    login_page.login(VALID_ADMIN, WRONG_PASS)

    # 用 .first：后端/表单校验可能弹出多条提示（重复提示本身见下方 xfail 用例），
    # 本用例只关心"确实给出了错误反馈且没放行"。
    expect(login_page.error_toast.first).to_be_visible(timeout=8_000)
    assert "/login" in page.url, f"凭据错误却离开了登录页：{page.url}"


def test_validation_error_is_shown_only_once(login_page: LoginPage):
    """同一次提交失败只应给出一条错误提示，重复弹窗会干扰用户判断。

    已修复（KNWN-UI-01）：根因不是"请求发了两次"（实测只有 1 次 POST /api/auth/login），
    而是 api 响应拦截器对业务错误弹了一次提示，调用方 catch 里又弹了一次。
    现在业务错误统一由调用方提示，拦截器不再重复弹。
    """
    login_page.login(VALID_ADMIN, WRONG_PASS)

    expect(login_page.error_toast.first).to_be_visible(timeout=8_000)
    login_page.page.wait_for_timeout(1500)      # 留出 repeated message 的入队时间
    count = login_page.error_toast.count()
    assert count == 1, f"同一次失败弹出了 {count} 条错误提示，应只有 1 条"


def test_empty_submit_triggers_field_level_validation(page, login_page: LoginPage):
    """空表单提交应触发前端字段校验，而不是直接发请求。"""
    login_page.submit_button.click()

    expect(login_page.form_item_error.first).to_be_visible(timeout=5_000)
    assert login_page.form_item_error.count() >= 1, "未出现任何字段级校验提示"


@pytest.mark.parametrize(
    ("button_testid", "expected_user", "expected_pass"),
    [
        ("quick-login-admin", "admin", "admin123"),
        ("quick-login-user", "user1", "123456"),
    ],
)
def test_quick_login_fills_expected_credentials(login_page: LoginPage, button_testid, expected_user, expected_pass):
    """"快速登录"按钮应自动回填对应角色的凭据，方便演示与回归。"""
    login_page.testid(button_testid).click()

    expect(login_page.username_input).to_have_value(expected_user)
    expect(login_page.password_input).to_have_value(expected_pass)
