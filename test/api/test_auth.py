"""认证接口测试：登录 / 注册 / 刷新 / 当前用户。

覆盖正反用例：正确登录、密码错误、用户不存在、刷新 token 无效。
"""
import pytest
import requests


class TestAuthLogin:

    def test_login_success_returns_tokens(self, client, admin_token):
        """正确账号密码 → 200 + accessToken + refreshToken。"""
        body = client.post("/api/auth/login", json={
            "username": "admin", "password": "admin123",
        }).json()
        assert body["code"] == 200
        assert body["data"]["accessToken"]
        assert body["data"]["refreshToken"]
        assert body["data"]["tokenType"] == "Bearer"

    def test_login_wrong_password(self, client):
        body = client.post("/api/auth/login", json={
            "username": "admin", "password": "wrong-pass",
        }).json()
        assert body["code"] != 200
        assert body["code"] == 1002  # USER_PASSWORD_ERROR

    def test_login_user_not_found(self, client):
        body = client.post("/api/auth/login", json={
            "username": "no_such_user_xyz", "password": "123456",
        }).json()
        assert body["code"] == 1001  # USER_NOT_FOUND

    def test_login_blank_username(self, client):
        body = client.post("/api/auth/login", json={
            "username": "", "password": "123456",
        }).json()
        assert body["code"] != 200


class TestAuthRefresh:

    def test_refresh_with_invalid_token_rejected(self, client):
        body = client.post("/api/auth/refresh", json={
            "refreshToken": "invalid-token-here",
        }).json()
        assert body["code"] != 200

    def test_refresh_roundtrip(self, client):
        """用 login 拿到的 refreshToken 换新 token。"""
        login = client.post("/api/auth/login", json={
            "username": "admin", "password": "admin123",
        }).json()
        refresh = login["data"]["refreshToken"]

        body = client.post("/api/auth/refresh", json={
            "refreshToken": refresh,
        }).json()
        assert body["code"] == 200
        assert body["data"]["accessToken"]


class TestAuthCurrentUser:

    def test_me_returns_user_with_token(self, auth_client):
        body = auth_client.get("/api/auth/me").json()
        assert body["code"] == 200
        assert body["data"]["userId"] is not None

    def test_me_without_token_rejected(self, fresh_client):
        """无 token 访问受保护接口应返回 401。"""
        body = fresh_client.get("/api/auth/me").json()
        assert body["code"] == 401