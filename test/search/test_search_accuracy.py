"""ES 专项 · 检索准确性。

复现 SearchService.search() 实际下发的 DSL，验证检索行为本身是否正确：
命中、相关度权重、高亮、过滤、分页、排序。
所有种子文档带 kwn_ 前缀并挂在专属分类下，与真实书库隔离。
"""
from __future__ import annotations

import pytest

import search_helper as sh

# 专属分类，用于把断言范围限定在本套件注入的种子文档内
TEST_CATEGORY = "kwn_专项测试"
RARE_KEYWORD = "量子编织机"


@pytest.fixture
def scoped_query():
    """构造一个"只在本套件种子文档内检索"的 bool 查询，避免受真实书库干扰。"""
    def _build(keyword: str | None = None, **extra) -> dict:
        must = [{"term": {"category": TEST_CATEGORY}}]
        if keyword is not None:
            must.append({
                "multi_match": {
                    "query": keyword,
                    "fields": ["title^3", "author^2", "summary"],
                }
            })
        return {"bool": {"must": must, **extra}}
    return _build


def test_keyword_search_matches_title(es_client, seeded_books, scoped_query):
    """标题包含关键词的图书必须能被检索到。"""
    seeded_books(title=f"{sh.SEED_PREFIX}{RARE_KEYWORD}入门", category=TEST_CATEGORY)
    es_client.refresh()

    resp = es_client.search({"query": scoped_query(RARE_KEYWORD), "size": 10})
    titles = [h["_source"]["title"] for h in resp["hits"]["hits"]]

    assert any(RARE_KEYWORD in t for t in titles), (
        f"标题含『{RARE_KEYWORD}』的图书未被检索到，命中集：{titles}"
    )


def test_title_boost_outranks_summary_match(es_client, seeded_books, scoped_query):
    """相关度权重契约：title^3 > author^2 > summary。

    构造两条只在命中字段上不同的文档——一条关键词在标题，一条在摘要。
    标题命中的那条必须排在前面，否则说明 boost 没生效或分词把信号冲淡了。
    """
    seeded_books(title=f"{sh.SEED_PREFIX}{RARE_KEYWORD}技术手册", summary="无关内容",
                 category=TEST_CATEGORY)
    seeded_books(title=f"{sh.SEED_PREFIX}无关书名", summary=f"本书讲解{RARE_KEYWORD}的原理",
                 category=TEST_CATEGORY)
    es_client.refresh()

    resp = es_client.search({"query": scoped_query(RARE_KEYWORD), "size": 10})
    hits = resp["hits"]["hits"]
    assert len(hits) >= 2, f"期望命中两条种子文档，实际 {len(hits)} 条"

    title_hit = next((h for h in hits if RARE_KEYWORD in h["_source"]["title"]), None)
    summary_hit = next((h for h in hits if RARE_KEYWORD not in h["_source"]["title"]), None)
    assert title_hit and summary_hit, "种子文档未按预期被区分命中"

    assert title_hit["_score"] > summary_hit["_score"], (
        f"标题命中得分 {title_hit['_score']} 未高于摘要命中得分 {summary_hit['_score']}，"
        f"title^3 的权重提升未生效"
    )


def test_highlight_wraps_matched_terms(es_client, seeded_books, scoped_query):
    """高亮必须用 <em> 包裹命中词，前端依赖该标签渲染。"""
    seeded_books(title=f"{sh.SEED_PREFIX}{RARE_KEYWORD}详解", category=TEST_CATEGORY)
    es_client.refresh()

    resp = es_client.search({
        "query": scoped_query(RARE_KEYWORD),
        "highlight": {
            "fields": {
                "title": {"pre_tags": ["<em>"], "post_tags": ["</em>"]},
                "author": {"pre_tags": ["<em>"], "post_tags": ["</em>"]},
                "summary": {"pre_tags": ["<em>"], "post_tags": ["</em>"]},
            }
        },
        "size": 10,
    })
    hits = resp["hits"]["hits"]
    assert hits, "未命中任何文档，无法校验高亮"

    highlighted = [h for h in hits if h.get("highlight", {}).get("title")]
    assert highlighted, f"没有任何命中返回 title 高亮片段，实际 highlight={hits[0].get('highlight')}"
    assert "<em>" in highlighted[0]["highlight"]["title"][0], (
        f"高亮标签缺失：{highlighted[0]['highlight']['title']}"
    )


def test_category_term_filter_is_exact(es_client, seeded_books, scoped_query):
    """分类是 keyword 精确过滤，不应出现跨分类串数据。"""
    seeded_books(title=f"{sh.SEED_PREFIX}甲书", category=TEST_CATEGORY)
    seeded_books(title=f"{sh.SEED_PREFIX}乙书", category="kwn_其他分类")
    es_client.refresh()

    resp = es_client.search({"query": scoped_query(), "size": 50})
    categories = {h["_source"]["category"] for h in resp["hits"]["hits"]}

    assert categories == {TEST_CATEGORY}, (
        f"分类 term 过滤串数据，返回了非目标分类：{categories - {TEST_CATEGORY}}"
    )


def test_pagination_slices_are_disjoint(es_client, seeded_books, scoped_query):
    """分页切片不能重叠，也不能漏数据 —— 否则列表页会出现重复或缺失条目。"""
    for i in range(5):
        seeded_books(title=f"{sh.SEED_PREFIX}分页测试{i}", category=TEST_CATEGORY)
    es_client.refresh()

    first = es_client.search({"query": scoped_query(), "from": 0, "size": 2, "sort": [{"id": "asc"}]})
    second = es_client.search({"query": scoped_query(), "from": 2, "size": 2, "sort": [{"id": "asc"}]})

    ids_first = [h["_source"]["id"] for h in first["hits"]["hits"]]
    ids_second = [h["_source"]["id"] for h in second["hits"]["hits"]]

    assert len(ids_first) == 2 and len(ids_second) == 2, (
        f"分页返回条数不符，第一页 {len(ids_first)} 条、第二页 {len(ids_second)} 条"
    )
    assert not set(ids_first) & set(ids_second), (
        f"分页切片重叠：第一页 {ids_first}、第二页 {ids_second}"
    )


def test_results_sorted_by_created_at_desc(es_client, seeded_books, scoped_query):
    """SearchService 按 createdAt 倒序返回，新书应排在前面。"""
    seeded_books(title=f"{sh.SEED_PREFIX}旧书", category=TEST_CATEGORY,
                 createdAt="2025-01-01T00:00:00")
    seeded_books(title=f"{sh.SEED_PREFIX}新书", category=TEST_CATEGORY,
                 createdAt="2026-08-01T00:00:00")
    seeded_books(title=f"{sh.SEED_PREFIX}中书", category=TEST_CATEGORY,
                 createdAt="2026-01-01T00:00:00")
    es_client.refresh()

    resp = es_client.search({
        "query": scoped_query(),
        "sort": [{"createdAt": {"order": "desc"}}],
        "size": 10,
    })
    created = [h["_source"]["createdAt"] for h in resp["hits"]["hits"]]
    assert created == sorted(created, reverse=True), f"createdAt 未按倒序返回：{created}"


def test_empty_keyword_respects_page_size(es_client, scoped_query):
    """空关键词时 SearchService 会退化成 matchAll（拉全库）。

    这是当前实现的既定行为，但必须至少守住一条底线：**分页参数仍然生效**。
    否则一次 GET /api/search 就能把全库拖出来，既拖垮 ES 也构成数据过度暴露。
    """
    resp = es_client.search({"query": {"match_all": {}}, "from": 0, "size": 3})
    hits = resp["hits"]["hits"]

    assert len(hits) == 3, f"空关键词查询未遵守 size=3，实际返回 {len(hits)} 条"
    assert resp["hits"]["total"]["value"] > 3, (
        "全库图书不足 3 本，该用例失去意义，请先灌入种子数据"
    )
