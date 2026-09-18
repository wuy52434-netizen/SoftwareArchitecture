"""搜索模块接口自动化测试（search-service 基于 Elasticsearch）。

覆盖：关键词精确/模糊检索、高亮标记、空关键词、热词、推荐、中文检索。
"""
import pytest


class TestSearch:
    def test_search_exact_keyword(self, client):
        body = client.get("/api/search", params={"keyword": "三体"}).json()
        assert body["code"] == 200
        assert len(body["data"]["books"]) >= 1
        # 命中的书标题应包含"三体"
        assert any("三体" in b.get("title", "") for b in body["data"]["books"])

    def test_search_highlight_structure(self, client):
        """ES 高亮字段应存在且结构完整"""
        body = client.get("/api/search", params={"keyword": "三体"}).json()
        first = body["data"]["books"][0]
        assert "highlightTitle" in first or "highlightSummary" in first

    def test_search_no_result(self, client):
        body = client.get("/api/search", params={"keyword": "zzzz不存在词"}).json()
        assert body["code"] == 200
        assert body["data"]["books"] == []  # 无结果返回空而非报错

    def test_search_empty_keyword(self, client):
        body = client.get("/api/search", params={"keyword": ""}).json()
        assert body["code"] == 200  # 空关键词不崩溃

    def test_search_pagination_shape(self, client):
        body = client.get("/api/search", params={"keyword": "a"}).json()
        d = body["data"]
        for key in ("books", "total", "page"):
            assert key in d, f"搜索结果缺字段 {key}"

    def test_search_by_author(self, client):
        body = client.get("/api/search", params={"keyword": "刘慈欣"}).json()
        assert body["code"] == 200
        assert len(body["data"]["books"]) >= 1


class TestHotAndRecommend:
    def test_hot(self, client):
        body = client.get("/api/search/hot").json()
        assert body["code"] == 200
        assert isinstance(body["data"], list)

    def test_recommend(self, client):
        body = client.get("/api/search/recommend").json()
        assert body["code"] == 200
        assert isinstance(body["data"], list)