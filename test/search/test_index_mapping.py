"""ES 专项 · 索引与 mapping 契约。

代码侧 ``BookDocument`` 声明了什么，运行时索引里就必须是什么。
索引 mapping 与代码实体不一致是 ES 项目最隐蔽的一类缺陷：
服务照常返回 200，只是检索结果"不太准"，很难被发现。

本文件对比三份"事实来源"：
  1. 代码实体 backend/search-service/.../document/BookDocument.java
  2. 运行时索引 GET /books/_mapping
  3. 权威 mapping 文件 backend/search-service/src/main/resources/es/books-mapping.json

**期望值一律从源码/权威文件解析出来，不在测试里硬编码。**
上一版把 ``ik_max_word`` 与 9 个字段名写死在测试里，结果测试本身就是错的：
实体其实声明了 15 个字段，分词器也和线上一致地都是 standard。
硬编码期望值等于又造一份会漂移的定义（KNWN-ES-02/03 的教训）。
"""
from __future__ import annotations

import json

import search_helper as sh

# BookDocument 中声明为 Keyword 的字段
KEYWORD_FIELDS = ("isbn", "publisher", "category", "status")


def _document_model_fields() -> set[str]:
    return set(sh.book_document_fields())


def test_index_exists(es_client):
    """books 索引必须存在，否则 /api/search 直接 500。"""
    assert es_client.index_exists(), f"索引 {sh.INDEX_BOOKS} 不存在"


def test_keyword_fields_are_not_analyzed(es_client):
    """精确匹配字段必须是 keyword，否则 term 查询会因分词而失效。"""
    props = es_client.mapping().get("properties", {})
    wrong = {}
    for field in KEYWORD_FIELDS:
        actual = props.get(field, {}).get("type")
        if actual != "keyword":
            wrong[field] = actual
    assert not wrong, (
        f"以下字段应为 keyword 实际为 {wrong}，"
        f"SearchService 里的 term 查询（category / status 过滤）会查不到数据"
    )


def _effective_search_analyzer(conf: dict) -> str | None:
    """ES 的语义：未显式声明 search_analyzer 时，它与 analyzer 相同。

    GET _mapping 在两者相同时也可能把 search_analyzer 省略掉，
    所以比较时不能把"缺省"当作"不一致"。
    """
    if "search_analyzer" in conf:
        return conf["search_analyzer"]
    return conf.get("analyzer")


def test_index_analyzer_matches_book_document_declaration(es_client):
    """运行时索引的分词器必须与 BookDocument 的声明逐字一致。

    已修复（KNWN-ES-02）：原先实体声明 ik_max_word/ik_smart，而运行时索引用 standard
    （ES 镜像从未装 IK 插件），代码在说谎。现在两处统一为 standard，
    并且本用例不写死具体分词器名 —— 它校验的是"代码与索引一致"这件事本身，
    所以无论是 standard 还是将来换成 IK，这条都成立。
    """
    declared = sh.book_document_analyzers()
    assert declared, "未能从 BookDocument.java 解析出任何分词器声明，解析逻辑可能已失效"

    props = es_client.mapping().get("properties", {})
    mismatched = {}
    for field, conf in declared.items():
        actual = props.get(field, {})
        if actual.get("analyzer") != conf.get("analyzer"):
            mismatched[field] = {
                "analyzer_declared": conf.get("analyzer"),
                "analyzer_index": actual.get("analyzer", "<未设置=standard>"),
            }
        declared_search = _effective_search_analyzer(conf)
        actual_search = _effective_search_analyzer(actual)
        if declared_search != actual_search:
            mismatched.setdefault(field, {})["search_analyzer_declared"] = declared_search
            mismatched[field]["search_analyzer_index"] = actual_search

    assert not mismatched, (
        f"以下字段的分词器与 BookDocument 声明不符：{mismatched}。"
        f"声明与线上不一致属于「文档骗人」，必须两边改齐"
    )


def test_chinese_tokenization_follows_declared_analyzer(es_client):
    """分词行为必须与声明的分词器自洽，并把中文分词质量的真实情况记录下来。

    已修复（KNWN-ES-02）：不再断言"必须切出词"。standard 对中文本来就是逐字切分，
    硬要求多字 token 等于要求一个没装的插件。这里按声明分支断言：
    * 声明 standard → 断言确实逐字切分（同时说明这正是中文检索精度受限的原因）；
    * 声明 ik_* → 断言必须切出多字词，否则说明插件没生效。
    """
    declared = sh.book_document_analyzers()
    title_analyzer = declared.get("title", {}).get("analyzer", "standard")
    tokens = es_client.analyze("软件测试技术", title_analyzer)
    assert tokens, f"analyzer={title_analyzer} 对中文没有产出任何 token"

    multi_char = [t for t in tokens if len(t) >= 2]
    if title_analyzer.startswith("ik_"):
        assert multi_char, f"声明使用 {title_analyzer} 却切不出多字词，IK 插件未生效：{tokens}"
    else:
        assert not multi_char, (
            f"声明 analyzer={title_analyzer}（逐字切分）却出现了多字 token {multi_char}，"
            f"声明与实际不一致"
        )


def test_index_field_set_matches_book_document_model(es_client):
    """索引字段集合必须与 BookDocument 声明的字段集合一致。

    已修复（KNWN-ES-03）：原先实体缺 updatedAt（索引有、实体无），
    按更新时间过滤/排序的查询取不到数据。期望值现在从 Java 源码解析，不写死。
    """
    model_fields = _document_model_fields()
    assert model_fields, "未能从 BookDocument.java 解析出字段，解析逻辑可能已失效"

    index_fields = set(es_client.mapping().get("properties", {}).keys())
    extra = index_fields - model_fields
    missing = model_fields - index_fields
    assert not extra and not missing, (
        f"索引多出字段 {sorted(extra)}；索引缺少字段 {sorted(missing)}"
        f"（实体解析出 {len(model_fields)} 个字段，索引有 {len(index_fields)} 个）"
    )


def test_index_mapping_file_matches_runtime_mapping(es_client):
    """权威 mapping 文件与运行时索引必须一致 —— 防止有人绕过文件手工改索引。

    这条是上面几条的"根因防线"：映射文件是唯一事实来源，
    一旦运行时索引与文件不一致，说明存在未受控的手工变更。
    """
    mapping_file = (sh.BOOK_DOCUMENT_JAVA.parents[5]
                    / "resources" / "es" / "books-mapping.json")
    expected = json.loads(mapping_file.read_text(encoding="utf-8"))["mappings"]["properties"]

    runtime = es_client.mapping().get("properties", {})
    expected_fields = set(expected.keys())
    runtime_fields = set(runtime.keys())

    assert expected_fields == runtime_fields, (
        f"权威文件 {mapping_file.name} 与运行时索引字段不一致："
        f"文件多 {sorted(expected_fields - runtime_fields)}，索引多 {sorted(runtime_fields - expected_fields)}"
    )
    for field, conf in expected.items():
        for key in ("type", "analyzer"):
            if key in conf:
                assert runtime[field].get(key) == conf[key], (
                    f"字段 {field} 的 {key} 不一致：文件={conf[key]} 索引={runtime[field].get(key)}"
                )
        if "search_analyzer" in conf:
            # ES 在 search_analyzer == analyzer 时会省略该键，按语义比较
            assert _effective_search_analyzer(runtime[field]) == conf["search_analyzer"], (
                f"字段 {field} 的 search_analyzer 不一致："
                f"文件={conf['search_analyzer']} 索引={_effective_search_analyzer(runtime[field])}"
            )
