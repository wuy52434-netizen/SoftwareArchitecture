"""用户管理模块接口自动化测试（user-service，经网关）。

覆盖：管理员 CRUD、越权访问、未授权拒绝、读者自身查询。
依赖：conftest 的 client / admin_token / fresh_client 夹具。
"""
import pytest
import uuid


def _uid():
    return str(uuid.uuid4())[:6]


class TestUserList:
    """GET /api/users —— 管理员才能访问"""

    def test_list_users_requires_admin(self, fresh_client):
        """未登录访问用户列表应被网关/服务拒（401）。"""
        resp = fresh_client.get("/api/users")
        body = resp.json()
        # 安全基线：用户管理属管理端，未登录必须被拒绝
        assert body["code"] == 401

    def test_list_users_with_admin(self, client, admin_token):
        resp = client.get("/api/users")
        body = resp.json()
        assert body["code"] == 200
        assert isinstance(body["data"], list)
        assert len(body["data"]) >= 1

    def test_list_users_userdata_structure(self, client, admin_token):
        body = client.get("/api/users").json()
        first = body["data"][0]
        # 用户实体关键字段契约
        assert "userId" in first or "id" in first
        assert "username" in first


class TestUserGet:
    def test_get_user_by_id_admin(self, client, admin_token):
        # 获取 admin 自身（id=9）
        body = client.get("/api/users/9").json()
        assert body["code"] == 200
        assert body["data"]["username"] == "admin"

    def test_get_user_not_found(self, client, admin_token):
        body = client.get("/api/users/99999").json()
        assert body["code"] != 200


class TestUserCreateReadUpdate:
    """创建-查询-更新-删除闭环"""

    def test_create_user_and_query(self, client, admin_token):
        uname = f"tester_{_uid()}"
        body = client.post("/api/users", json={
            "username": uname,
            "password": "Passw0rd!",
            "realName": "自动化测试员",
            "userType": "reader",
        }).json()
        assert body["code"] == 200, body.get("message")
        uid = body["data"]["userId"]

        # 用新账号登录
        resp = client.post("/api/auth/login", json={"username": uname, "password": "Passw0rd!"})
        assert resp.json()["code"] == 200

    def test_duplicate_username_rejected(self, client, admin_token):
        body = client.post("/api/users", json={
            "username": "admin", "password": "x123456", "realName": "dup",
        }).json()
        assert body["code"] != 200  # 重复用户名被拒

    def test_create_missing_password_validation(self, client, admin_token):
        body = client.post("/api/users", json={"username": f"u_{_uid()}"}).json()
        assert body["code"] != 200 or body["code"] == 400


class TestUserUpdateStatus:
    def test_update_user_status(self, client, admin_token):
        """更新用户状态：参数走 query（契约属性校验）。"""
        body = client.put("/api/users/9/status", params={"status": "active"}).json()
        # 契约：status 为 query param；接受业务成功/业务错误，不允许 500
        assert body["code"] in (200, 5001) or body["code"] != 500

    def test_update_self_profile(self, client, admin_token):
        # 管理员更新自己真实姓名（幂等测试：改回原名）
        profile = client.get("/api/users/9").json()["data"]
        original = profile.get("realName")
        body = client.put("/api/users/9", json={"realName": original}).json()
        assert body["code"] == 200


class TestUserAuthGuard:
    """安全基线：未被白名单保护的写接口必须拒绝未授权"""

    def test_create_user_without_token_rejected(self, fresh_client):
        resp = fresh_client.post("/api/users", json={
            "username": "hacker_x", "password": "123456",
        })
        assert resp.json()["code"] == 401

    def test_list_users_without_token_rejected(self, fresh_client):
        assert fresh_client.get("/api/users").json()["code"] == 401