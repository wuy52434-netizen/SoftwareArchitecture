"""登录页页面对象。

对应前端 `frontend/src/views/Login.vue`，路由 `/login`。
"""
from __future__ import annotations

from playwright.sync_api import expect

from .base_page import BasePage


class LoginPage(BasePage):
    path = "/login"

    # ------------------------------------------------------------ 元素
    @property
    def form(self):
        return self.testid("login-form")

    @property
    def username_input(self):
        # Element Plus 的 el-input 会把非 prop 属性**透传到内部原生 <input>**，
        # 而不是外层 .el-input 容器。已用 Playwright 实地 dump 确认：
        #   <input class="el-input__inner" data-testid="login-username" ...>
        # 所以这里直接用 testid 本身，不能再 .locator("input")（会找不到任何元素）。
        return self.testid("login-username")

    @property
    def password_input(self):
        return self.testid("login-password")

    @property
    def remember_checkbox(self):
        """el-checkbox 的 testid 落在 <label> 上，原生 checkbox 在其内部。"""
        return self.testid("login-remember").locator("input")

    @property
    def submit_button(self):
        return self.testid("login-submit")

    @property
    def quick_login_admin(self):
        return self.testid("quick-login-admin")

    @property
    def error_toast(self):
        """ElMessage 的错误提示。"""
        return self.page.locator(".el-message--error")

    @property
    def success_toast(self):
        return self.page.locator(".el-message--success")

    @property
    def form_item_error(self):
        """el-form 校验失败时的字段级红字提示。"""
        return self.page.locator(".el-form-item__error")

    # ------------------------------------------------------------ 行为
    def open(self, expect_ready: bool = True) -> "LoginPage":
        self.goto()
        if expect_ready:
            self.expect_loaded()
        return self

    def expect_loaded(self) -> None:
        expect(self.form).to_be_visible()
        expect(self.username_input).to_be_visible()
        expect(self.password_input).to_be_visible()
        expect(self.submit_button).to_be_visible()

    def login(self, username: str, password: str) -> "LoginPage":
        """填表并提交；不做结果断言，由调用方决定期望（成功 / 失败）。"""
        self.username_input.fill(username)
        self.password_input.fill(password)
        self.submit_button.click()
        return self

    def login_expect_success(self, username: str, password: str) -> "LoginPage":
        self.login(username, password)
        expect(self.error_toast).to_have_count(0)
        return self
