"""图书接口测试：列表 / 详情 / 搜索 / 分类 / CRUD。

覆盖缓存旁路、分页参数、搜索(ES)、热门/新书、以及写操作的鉴权要求。
"""
import pytest
import time


class TestBookList:

    def test_list_books_pagination(self, client):
        body = client.get("/api/books", params={"page": 1, "per_page": 5}).json()
        assert body["code"] == 200
        data = body["data"]
        assert len(data["records"]) <= 5
        assert data["total"] >= 0

    def test_list_search_by_title(self, client):
        body = client.get("/api/books", params={"search": "三体"}).json()
        assert body["code"] == 200
        records = body["data"]["records"]
        if records:
            assert any("三体" in r["title"] for r in records)

    def test_list_filter_by_category(self, client):
        body = client.get("/api/books", params={"category": "1"}).json()
        assert body["code"] == 200
        for r in body["data"]["records"]:
            assert r["category"] == "文学"

    def test_list_empty_page_not_error(self, client):
        body = client.get("/api/books", params={"page": 9999, "per_page": 24}).json()
        assert body["code"] == 200


class TestBookDetail:

    def test_detail_returns_book(self, client):
        """详情走 Redis 缓存旁路 —— 返回结构完整。"""
        body = client.get("/api/books/1").json()
        assert body["code"] == 200
        book = body["data"]
        assert book["id"] == 1
        assert book["title"]
        assert book["isbn"]

    def test_detail_not_found_bookid_999999(self, client):
        resp = client.get("/api/books/999999")
        body = resp.json()
        # 实体查不到时返回业务失败
        assert body["code"] != 200


class TestBookCategoryAndPopular:

    def test_categories(self, client):
        body = client.get("/api/books/categories").json()
        assert body["code"] == 200
        assert len(body["data"]) >= 6

    def test_popular_books(self, client):
        body = client.get("/api/books/popular").json()
        assert body["code"] == 200

    def test_newest_books(self, client):
        body = client.get("/api/books/newest").json()
        assert body["code"] == 200


class TestBookWrite:

    def test_book_namespace_is_public_by_design(self, fresh_client):
        """安全基线：网关白名单含 /api/books/**（借书机自助场景需匿名访问）。
        → 无 token 读取图书列表应放行返回 200。"""
        body = fresh_client.get("/api/books", params={"per_page": 1}).json()
        assert body["code"] == 200
        assert "records" in body["data"]

    def test_protected_user_management_requires_auth(self, fresh_client):
        """安全基线：/api/users/** 不在白名单，无 token 应被网关拦截(401)。"""
        resp = fresh_client.post("/api/users", json={
            "username": "hacker", "password": "123456", "realName": "x",
        })
        body = resp.json()
        assert resp.status_code in (401, 403) or body.get("code") in (401, 403)

    def test_create_and_update_and_delete_roundtrip(self, auth_client):
        """完整写链路：创建 → 查询 → 更新 → 删除。"""
        uniq = int(time.time())
        create = auth_client.post("/api/books", json={
            "isbn": f"9789{uniq}0000",
            "title": f"自动化测试图书{uniq}",
            "author": "测试作者",
            "price": 39.9,
            "categoryId": 1,
            "totalCopies": 2,
            "language": "中文",
        })
        cbody = create.json()
        assert cbody["code"] == 200, f"创建失败: {cbody}"
        book_id = cbody["data"]["id"]

        try:
            # 查询详情
            detail = auth_client.get(f"/api/books/{book_id}").json()
            assert detail["code"] == 200
            assert detail["data"]["title"].startswith("自动化测试图书")

            # 更新
            update = auth_client.put(f"/api/books/{book_id}", json={
                "title": f"更新后的图书{uniq}",
                "status": "available",
            }).json()
            assert update["code"] == 200
            assert "更新后的图书" in update["data"]["title"]
        finally:
            # 清理
            delete = auth_client.delete(f"/api/books/{book_id}").json()
            assert delete["code"] == 200


class TestBookScan:

    def test_scan_book_1_by_isbn(self, client):
        """扫码接口（借书机）：用 book1 的 ISBN 扫描应返回 book。"""
        body = client.get("/api/books/scan", params={"code": "9787229100605"}).json()
        assert body["code"] == 200
        assert body["data"]["book"]["title"] == "三体"