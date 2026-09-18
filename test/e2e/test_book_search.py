"""UI 自动化 · 读者门户图书检索链路。

对应前端 `frontend/src/views/portal/Books.vue` → `GET /api/books`（**MySQL LIKE 检索**）。
借书机端的 ES 检索链路另见 `test_kiosk_search.py`。
"""
from __future__ import annotations

import pytest
from playwright.sync_api import expect

from pages.books_page import BooksPage

UNMATCHED_KEYWORD = "zzz不存在的书名zzz"


def test_books_page_loads_and_lists_books(books_page: BooksPage):
    """页面加载后应渲染出图书卡片，而不是空白或一直 loading。"""
    expect(books_page.search_input).to_be_visible()

    total = books_page.require_catalog()

    assert books_page.book_cards.count() > 0, "总数大于 0 但一张图书卡片都没渲染出来"


def test_keyword_search_narrows_result_set(books_page: BooksPage):
    """用书名中的连续片段检索，应能命中且能缩小结果集。"""
    baseline = books_page.require_catalog()

    first_title = books_page.rendered_titles()[0]
    keyword = first_title[:4]
    books_page.search(keyword)

    narrowed = books_page.total_count_value()
    assert narrowed >= 1, f"用书名片段『{keyword}』检索应有命中的图书，实际 0 条"
    assert narrowed <= baseline, f"检索后结果数 {narrowed} 反而多于检索前 {baseline}"

    titles = books_page.rendered_titles()
    assert any(keyword in t for t in titles), (
        f"检索『{keyword}』返回的结果里没有任何标题包含该关键词：{titles[:5]}"
    )


def test_search_with_unmatched_keyword_shows_empty_state(books_page: BooksPage):
    """无命中时应给出空状态提示，而不是停留在上一次的结果上。"""
    books_page.search(UNMATCHED_KEYWORD)

    assert books_page.total_count_value() == 0, (
        f"用不可能命中的关键词检索却返回了结果，总数="
        f"{books_page.total_count_value()}"
    )
    expect(books_page.empty_state).to_be_visible()
    assert books_page.book_cards.count() == 0, "空状态下仍渲染了图书卡片"


def test_rendered_card_count_never_exceeds_page_size(books_page: BooksPage):
    """分页必须真的生效：单页渲染的卡片数不能超过每页上限（当前最大 48）。"""
    rendered = books_page.book_cards.count()
    assert rendered <= 48, f"单页渲染了 {rendered} 张卡片，超过每页上限 48，分页未生效"


def test_clearing_search_restores_full_result_set(books_page: BooksPage):
    """清空关键词后应回到全量结果，不能把过滤条件粘住。"""
    baseline = books_page.require_catalog()

    books_page.search(UNMATCHED_KEYWORD)
    assert books_page.total_count_value() == 0

    books_page.clear_search()
    assert books_page.total_count_value() == baseline, (
        f"清空检索条件后未恢复全量结果，期望 {baseline}，实际 "
        f"{books_page.total_count_value()}"
    )
