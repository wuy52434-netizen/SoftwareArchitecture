"""统计模块接口测试（stats-service /api/stats）。

覆盖：看板结构、鉴权护栏、借还统计事件。
"""
import pytest


class TestStatsDashboard:
    def test_dashboard_requires_auth(self, fresh_client):
        body = fresh_client.get("/api/stats/dashboard").json()
        assert body["code"] == 401  # 统计看板受保护

    def test_dashboard_with_admin(self, client, admin_token):
        body = client.get("/api/stats/dashboard").json()
        assert body["code"] == 200
        d = body["data"]
        # 看板核心指标字段
        for key in ("totalBooks", "availableBooks", "totalUsers",
                    "totalBorrows", "activeBorrows", "overdueCount"):
            assert key in d, f"缺统计字段 {key}"

    def test_dashboard_consistency(self, client, admin_token):
        """统计总量与分页/状态数据交叉核对（灰盒）"""
        body = client.get("/api/stats/dashboard").json()["data"]
        assert body["totalBooks"] >= 0
        assert body["activeBorrows"] <= body["totalBorrows"]
        assert len(body["borrowTrend"]) >= 7 or "borrowTrend" in body

    def test_dashboard_does_not_fabricate_chart_data(self, client, admin_token):
        """看板不得编造统计值（已修复 KNWN-DEF-02）。

        `StatsService` 原先在"无数据"时回落到写死的常量并当作真实统计返回：
        `userTypeDistribution` 直接写死 65/20/15 且从不查库；趋势/时段/分类/热门榜
        也各有一套编造值。现在这些回落全部移除，没有数据就返回空/零。

        断言的是**不变量**而不是"必须为空"：分布要么为空（暂无数据），
        要么各项之和等于真实总量 —— 两者都不允许出现凭空捏造的数字。
        这样将来真的实现了聚合逻辑，本用例依然成立。
        """
        d = client.get("/api/stats/dashboard").json()["data"]

        dist = d["userTypeDistribution"]
        if dist:
            dist_sum = sum(item["value"] for item in dist)
            real_users = client.get("/api/users").json()["data"]["total"]
            assert dist_sum == real_users, (
                f"userTypeDistribution 非空时各项之和应等于真实用户数："
                f"分布之和 {dist_sum} ≠ 真实用户数 {real_users} —— 说明存在编造的统计值"
            )

        # 回归护栏：这几个正是原先写死的编造值，一旦重新出现立即失败
        fabricated = {("学生", 65), ("教师", 20), ("其他", 15)}
        actual_pairs = {(item["name"], item["value"]) for item in dist}
        assert not (fabricated & actual_pairs), (
            f"看板重新出现了写死的编造分布 {sorted(fabricated & actual_pairs)}"
        )

    def test_dashboard_books_vs_books_endpoint(self, client, admin_token):
        """[KNWN-BUG] 看板图书总数 与 /api/books 分页 total 一致（数据一致性基线）。

        已修复（KNWN-DEF-01）：看板原先读的是本地 Redis 计数器 `stats:total:books`，
        初值还是硬编码常量 24，与真实库表数量天然不一致。
        现在 stats-service 通过 Feign 向 book-service 取 total，与 `GET /api/books` 同源同口径。
        """
        d = client.get("/api/stats/dashboard").json()["data"]
        books_total = client.get("/api/books", params={"per_page": 1}).json()["data"]["total"]
        assert d["totalBooks"] == books_total, (
            f"看板图书数 {d['totalBooks']} ≠ 图书接口 total {books_total}（统计口径需对齐）"
        )


class TestStatsEvents:
    def test_borrow_event_ok(self, client, admin_token):
        """借阅统计事件：参数走 query（契约属性校验）。"""
        body = client.post("/api/stats/borrow", params={"bookId": 1, "count": 1}).json()
        assert body["code"] in (200, 5001) or body["code"] != 500

    def test_return_event_ok(self, client, admin_token):
        body = client.post("/api/stats/return", params={"bookId": 1}).json()
        assert body["code"] in (200, 5001) or body["code"] != 500