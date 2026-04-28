package com.library.search.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class SearchDTO {

    @Data
    public static class SearchRequest {
        private String keyword;
        private String category;
        private Integer year;
        private String status;
        private Integer page = 1;
        private Integer size = 10;
    }

    @Data
    public static class SearchResult {
        private List<BookItem> books;
        private Long total;
        private Integer page;
        private Integer size;
        private Integer pages;
        private Map<String, Long> categoryAggs;
        private Map<String, Long> yearAggs;
    }

    @Data
    public static class BookItem {
        private Long id;
        private String isbn;
        private String title;
        private String author;
        private String publisher;
        private String category;
        private String summary;
        private String highlightTitle;
        private String highlightAuthor;
        private String highlightSummary;
        private BigDecimal price;
        private String coverUrl;
        private String status;
        private Integer availableCopies;
        private Integer publishDate;
    }

    @Data
    public static class RecommendRequest {
        private Long userId;
        private Integer size = 10;
    }

    @Data
    public static class HotBooksRequest {
        private String category;
        private Integer size = 10;
    }
}
