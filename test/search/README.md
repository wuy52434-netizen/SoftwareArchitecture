# Elasticsearch 检索与数据一致性专项测试

覆盖**索引 mapping 契约**、**检索准确性**、**MySQL→ES 同步一致性**三个维度。

## 为什么要分两层测

只从 `/api/search` 打接口，只能看到"搜出来的结果不对"，定位不到根因。本套件同时直连 ES：

| 层 | 地址 | 能验证什么 |
|---|---|---|
| ES 直连层 | `http://127.0.0.1:9200` | 索引是否存在、mapping 分析器、字段类型、分词结果、DSL 权重与高亮 |
| 服务层 | `http://127.0.0.1:8080`（网关 → search-service） | 对外检索行为、读写一致性 |

## 被测事实来源

- 代码实体：`backend/search-service/src/main/java/com/library/search/document/BookDocument.java`
- 检索 DSL：`backend/search-service/src/main/java/com/library/search/service/SearchService.java`
- 接口：`GET /api/search?keyword=&category=&year=&status=&page=&size=`、`/api/search/hot`、`/api/search/recommend`
- 索引：`books`，字段 `title^3 / author^2 / summary` multi_match，高亮 `<em>`，按 `createdAt` 倒序
- 建索引脚本：`seed_es_books.py`（手工 MySQL→ES 灌库）

## 前置条件

```bash
docker compose -f docker/docker-compose.yml up -d elasticsearch
# 启动后端（search-service 提供检索，book-service 提供写接口）
python seed_es_books.py          # 首次灌库
```

ES 或网关不可达时用例自动跳过。两个哨兵相互独立：只跑 mapping 用例不需要网关，只跑接口用例不需要直连 ES。

## 运行

```bash
pip install -r requirements.txt
pytest -v
pytest -v --html=report.html --self-contained-html
pytest -v -k sync                # 只跑同步一致性
ES_URL=http://10.0.0.5:9200 BASE_URL=http://10.0.0.5:8080 pytest -v
```

## 目录结构

```
test/search/
├── search_helper.py             # ES 直连客户端 + 网关客户端 + 种子文档管理
├── conftest.py                  # es_client / gateway / seeded_books 夹具
├── test_index_mapping.py        # 索引与 mapping 契约（5 条）
├── test_search_accuracy.py      # 检索准确性（7 条）
└── test_sync_consistency.py     # DB→ES 同步一致性（4 条）
```

## 设计要点

1. **种子数据隔离**：所有注入文档统一 `kwn_` 前缀 + 专属分类 `kwn_专项测试`，
   用 `bool.must[term(category)]` 把断言范围限定在种子集内，不受真实书库干扰，用例结束即清理。
2. **不用 sleep 硬等**：ES 是近实时引擎，写入断言统一走 `wait_for_indexing` / `_wait_in_index` 轮询。
3. **xfail 前先立证据**：`test_book_creation_api_actually_persists` 先证明创建接口确实写库成功，
   排除"创建失败"这个解释，才把"检索不到"的根因锁定在同步缺失上。
4. **已知缺陷用 `xfail(strict=True)`**：套件保持绿色，缺陷被固化成可执行文档；
   一旦修好会变成 XPASS 并失败，提醒更新用例。

## 已定位缺陷

| 编号 | 严重度 | 问题 | 证据 |
|---|---|---|---|
| **KNWN-ES-01** | P1 | **ES 与 MySQL 完全没有同步机制**。`book-service` 全模块无任何 ES 相关代码；`SearchService.indexBook()` 定义了写入方法但全仓库无调用方；索引只能靠手工跑 `seed_es_books.py`。后果：新书上架搜不到、改书名索引仍是旧名、已删图书仍能被搜到（点进去 404）。 | `test_sync_consistency.py` 三条用例 |
| **KNWN-ES-02** | P1 | **代码声明的分词器与线上索引不一致**。`BookDocument` 声明 `analyzer=ik_max_word / searchAnalyzer=ik_smart`，但运行时索引由 `seed_es_books.py` 以 standard 分词创建（脚本注释原文：「standard 分词, 不依赖外部 IK 插件」），`docker-compose` 的 `elasticsearch:8.11.3` 镜像也未装 IK 插件。后果：中文退化为单字切分，「软件测试」→ 软/件/测/试，短语检索精度与相关度排序明显退化；且若索引被删除，Spring Data ES 按实体重建索引会因找不到 IK 分析器而失败。 | `test_index_mapping.py` 两条用例 |
| **KNWN-ES-03** | P2 | 运行时 mapping 比 `BookDocument` 多出 `price` / `coverUrl` / `availableCopies` / `borrowCount`，代码实体无法映射这些字段，前端拿到的价格与可借数量恒为空。 | `test_index_mapping.py::test_index_field_set_matches_document_model` |
| **KNWN-ES-04** | P3 | `SearchService.search()` 在关键词与过滤条件全空时退化为 `match_all` 拉全库。当前 `size` 仍生效，但接口缺少「空条件拒绝」或上限保护，属于潜在的数据过度暴露与性能风险。 | `test_search_accuracy.py::test_empty_keyword_respects_page_size` |

## 修复建议

```java
// 1. 补上 DB→ES 同步（解决 KNWN-ES-01）
//    方案 A（推荐，解耦）：book-service 在增删改后发领域事件，search-service 消费后 indexBook/deleteBook
//    方案 B（简单）：book-service 直接调用 search-service 的内部接口
//    不要用「定时全量重建」兜底——数据窗口期的问题依然存在

// 2. 分词器与运行时环境对齐（解决 KNWN-ES-02）
//    要么在 ES 镜像里装 IK：docker-compose 增加
//      RUN elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.11.3/elasticsearch-analysis-ik-8.11.3.zip
//    要么下调代码声明，与 mapping 保持一致，不要两套说法
//    索引重建统一由 Spring Data ES 或版本化 mapping 脚本负责，不要再用一次性 seed 脚本建索引

// 3. 索引字段与实体对齐（解决 KNWN-ES-03）
//    BookDocument 补 price / coverUrl / availableCopies / borrowCount

// 4. 空查询保护（解决 KNWN-ES-04）
//    keyword 与所有过滤条件均为空时直接返回 400，或强制 from+size 上限
```
