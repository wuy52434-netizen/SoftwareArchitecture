"""接口自动化测试公共配置。

技术栈：pytest + requests
被测对象：图书自动借书机系统（spring-cloud 微服务，经 API 网关 8080 暴露）。
Base URL 可通过环境变量 BASE_URL 覆盖（默认走本机网关）。
"""
import os
import pytest
import requests

BASE_URL = os.getenv("BASE_URL", "http://127.0.0.1:8080")

# 测试账号（在系统内真实存在的种子用户）
ADMIN_USER = os.getenv("ADMIN_USER", "admin")
ADMIN_PASS = os.getenv("ADMIN_PASS", "admin123")


class ApiClient:
    """轻量 HTTP 客户端：自动携带 token，统一超时与 JSON 解析。"""

    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/")
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
        # 本地被测服务必须直连，绝不能继承宿主机的 HTTP_PROXY/HTTPS_PROXY。
        # 一旦走了代理，请求行会变成 absolute-form（http://127.0.0.1:8080/...），
        # 服务端/网关无法路由，表现为忽好忽坏的 400/超时（已实测复现）。
        self.session.trust_env = False
        self.token = None
        self.timeout = 10

    def _headers(self):
        h = {}
        if self.token:
            h["Authorization"] = f"Bearer {self.token}"
        return h

    def request(self, method, path, **kwargs):
        kwargs.setdefault("timeout", self.timeout)
        headers = dict(kwargs.pop("headers", {}))
        headers.update(self._headers())
        url = self.base_url + path
        resp = self.session.request(method, url, headers=headers, **kwargs)
        return resp

    def get(self, path, **kw):
        return self.request("GET", path, **kw)

    def post(self, path, json=None, **kw):
        return self.request("POST", path, json=json, **kw)

    def put(self, path, json=None, **kw):
        return self.request("PUT", path, json=json, **kw)

    def delete(self, path, **kw):
        return self.request("DELETE", path, **kw)

    def login(self, username, password):
        resp = self.post("/api/auth/login", json={
            "username": username,
            "password": password,
        })
        body = resp.json()
        if body.get("code") == 200:
            self.token = body["data"]["accessToken"]
            return body["data"]
        return None


@pytest.fixture(scope="session")
def client():
    return ApiClient(BASE_URL)


@pytest.fixture(scope="session")
def admin_token(client):
    """前置步骤：管理员登录获取 token，失败快速定位。"""
    client.login(ADMIN_USER, ADMIN_PASS)
    assert client.token, f"管理员 {ADMIN_USER} 登录失败，请检查服务是否启动及账号是否正确"
    return client.token


@pytest.fixture(scope="session")
def auth_client(client, admin_token):
    """已携带管理员 token 的客户端，供需要鉴权的用例复用。"""
    client.token = admin_token
    return client


@pytest.fixture
def fresh_client():
    """全新的无 token 客户端。每次独立创建，避免 session 级共享 client 的 token 污染。"""
    return ApiClient(BASE_URL)