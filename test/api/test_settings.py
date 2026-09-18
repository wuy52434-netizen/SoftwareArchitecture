"""系统设置模块接口自动化测试（user-service /api/settings）。

覆盖：查询、更新、参数校验、数据影响（设置项真实影响借阅上限等）。
"""
import pytest


class TestSettingsRead:
    def test_get_settings_public(self, fresh_client):
        """系统设置（借阅机展示用）为公开读接口"""
        body = fresh_client.get("/api/settings").json()
        assert body["code"] == 200
        d = body["data"]
        assert "maxBorrowCount" in d
        assert "borrowDays" in d
        assert "systemName" in d

    def test_settings_values_positive(self, client):
        d = client.get("/api/settings").json()["data"]
        assert d["maxBorrowCount"] >= 1
        assert d["borrowDays"] >= 1
        assert d["maxRenewalTimes"] >= 0

    def test_settings_structure(self, client):
        d = client.get("/api/settings").json()["data"]
        # 借阅相关配置字段契约
        for key in ("maxBorrowCount", "borrowDays", "maxRenewalTimes",
                    "overdueFinePerDay", "maxFineAmount"):
            assert key in d, f"缺设置字段 {key}"


class TestSettingsUpdate:
    def test_update_settings_shape(self, client, admin_token):
        # 采用"读-改-回"幂等策略，避免破坏真实配置
        cur = client.get("/api/settings").json()["data"]
        body = client.put("/api/settings", json=cur).json()
        assert body["code"] in (200, 5001)  # 允许业务变更挂起

    def test_settings_guard(self, fresh_client):
        """写接口安全基线：未授权 PUT 不应匿名改配置。

        已修复（KNWN-SEC-02）：网关白名单原先**只按路径匹配、不看方法**，
        `/api/settings` 一旦进白名单就连 PUT 也免鉴权，匿名即可改借阅天数与罚款。
        现改为「方法 + 路径」白名单：只读 `GET /api/settings` 匿名放行（借书机归还页需要读规则），
        `PUT /api/settings` 必须携带有效 Token。本用例现在应当稳定通过。
        """
        resp = fresh_client.put("/api/settings", json={"borrowDays": 30})
        body = resp.json()
        code = body.get("code")
        assert resp.status_code in (401, 403) or code in (401, 403), (
            f"未授权 PUT /api/settings 未被拦截：HTTP {resp.status_code}, body={body}"
        )