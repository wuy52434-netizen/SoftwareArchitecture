package com.library.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import com.library.common.result.Result;
import com.library.search.document.BookDocument;
import com.library.search.dto.SearchDTO.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchTemplate elasticsearchTemplate;

    public SearchResult search(SearchRequest request) {
        try {
            List<Query> queries = new ArrayList<>();
            
            if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
                Query multiMatch = Query.of(q -> q.multiMatch(m -> m
                        .query(request.getKeyword())
                        .fields("title^3", "author^2", "summary")
                ));
                queries.add(multiMatch);
            }
            
            if (request.getCategory() != null && !request.getCategory().isEmpty()) {
                Query termQuery = Query.of(q -> q.term(t -> t
                        .field("category")
                        .value(request.getCategory())
                ));
                queries.add(termQuery);
            }
            
            if (request.getStatus() != null && !request.getStatus().isEmpty()) {
                Query termQuery = Query.of(q -> q.term(t -> t
                        .field("status")
                        .value(request.getStatus())
                ));
                queries.add(termQuery);
            }
            
            Query finalQuery = queries.isEmpty() 
                    ? Query.of(q -> q.matchAll(m -> m))
                    : Query.of(q -> q.bool(b -> b.must(queries)));
            
            Highlight highlight = Highlight.of(h -> h
                    .fields("title", HighlightField.of(f -> f.preTags("<em>").postTags("</em>")))
                    .fields("author", HighlightField.of(f -> f.preTags("<em>").postTags("</em>")))
                    .fields("summary", HighlightField.of(f -> f.preTags("<em>").postTags("</em>")))
            );
            
            int from = (request.getPage() - 1) * request.getSize();
            
            SearchResponse<BookDocument> response = elasticsearchClient.search(s -> s
                    .index("books")
                    .query(finalQuery)
                    .highlight(highlight)
                    .from(from)
                    .size(request.getSize())
                    .sort(sort -> sort.field(f -> f.field("createdAt").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)))
            , BookDocument.class);
            
            List<BookItem> items = new ArrayList<>();
            for (Hit<BookDocument> hit : response.hits().hits()) {
                BookDocument doc = hit.source();
                if (doc != null) {
                    BookItem item = toBookItem(doc);
                    
                    if (hit.highlight() != null) {
                        List<String> titleHighlights = hit.highlight().get("title");
                        if (titleHighlights != null && !titleHighlights.isEmpty()) {
                            item.setHighlightTitle(titleHighlights.get(0));
                        }
                        List<String> authorHighlights = hit.highlight().get("author");
                        if (authorHighlights != null && !authorHighlights.isEmpty()) {
                            item.setHighlightAuthor(authorHighlights.get(0));
                        }
                        List<String> summaryHighlights = hit.highlight().get("summary");
                        if (summaryHighlights != null && !summaryHighlights.isEmpty()) {
                            item.setHighlightSummary(summaryHighlights.get(0));
                        }
                    }
                    items.add(item);
                }
            }
            
            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            int pages = (int) Math.ceil((double) total / request.getSize());
            
            SearchResult result = new SearchResult();
            result.setBooks(items);
            result.setTotal(total);
            result.setPage(request.getPage());
            result.setSize(request.getSize());
            result.setPages(pages);
            
            return result;
            
        } catch (Exception e) {
            log.error("搜索失败: {}", e.getMessage());
            throw new RuntimeException("搜索失败: " + e.getMessage());
        }
    }

    public void indexBook(BookDocument document) {
        try {
            elasticsearchClient.index(i -> i
                    .index("books")
                    .id(String.valueOf(document.getId()))
                    .document(document)
            );
            log.info("图书索引创建成功: id={}", document.getId());
        } catch (Exception e) {
            log.error("图书索引创建失败: {}", e.getMessage());
        }
    }

    public void deleteIndex(Long bookId) {
        try {
            elasticsearchClient.delete(d -> d
                    .index("books")
                    .id(String.valueOf(bookId))
            );
            log.info("图书索引删除成功: id={}", bookId);
        } catch (Exception e) {
            log.error("图书索引删除失败: {}", e.getMessage());
        }
    }

    public List<BookItem> getHotBooks(String category, int size) {
        try {
            List<Query> queries = new ArrayList<>();
            
            if (category != null && !category.isEmpty()) {
                Query termQuery = Query.of(q -> q.term(t -> t
                        .field("category")
                        .value(category)
                ));
                queries.add(termQuery);
            }
            
            Query finalQuery = queries.isEmpty() 
                    ? Query.of(q -> q.matchAll(m -> m))
                    : Query.of(q -> q.bool(b -> b.must(queries)));
            
            SearchResponse<BookDocument> response = elasticsearchClient.search(s -> s
                    .index("books")
                    .query(finalQuery)
                    .size(size)
                    .sort(sort -> sort.field(f -> f.field("borrowCount").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)))
            , BookDocument.class);
            
            List<BookItem> items = new ArrayList<>();
            for (Hit<BookDocument> hit : response.hits().hits()) {
                BookDocument doc = hit.source();
                if (doc != null) {
                    items.add(toBookItem(doc));
                }
            }
            return items;
            
        } catch (Exception e) {
            log.error("获取热门图书失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private BookItem toBookItem(BookDocument doc) {
        BookItem item = new BookItem();
        item.setId(doc.getId());
        item.setIsbn(doc.getIsbn());
        item.setTitle(doc.getTitle());
        item.setAuthor(doc.getAuthor());
        item.setPublisher(doc.getPublisher());
        item.setCategory(doc.getCategory());
        item.setSummary(doc.getSummary());
        item.setPrice(doc.getPrice());
        item.setCoverUrl(doc.getCoverUrl());
        item.setStatus(doc.getStatus());
        item.setAvailableCopies(doc.getAvailableCopies());
        item.setPublishDate(doc.getPublishDate());
        return item;
    }
}
