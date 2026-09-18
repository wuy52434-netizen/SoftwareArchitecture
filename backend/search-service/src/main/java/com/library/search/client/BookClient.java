package com.library.search.client;

import com.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

/**
 * 图书详情客户端（KNWN-ES-01）。
 *
 * <p>同步事件只带 bookId，消费端拿到 id 后回这里取详情再写索引。
 * 这样做而不是让生产端把整份图书快照塞进消息：字段到索引的映射只存在一处，
 * 不会出现"生产端改了字段、索引映射没跟上"的双份维护问题。
 */
@FeignClient(name = "book-service", fallback = BookClientFallback.class)
public interface BookClient {

    @GetMapping("/api/books/{id}")
    Result<BookDetail> getBookById(@PathVariable("id") Long id);

    /** 只声明索引需要的字段。 */
    class BookDetail {
        private Long id;
        private String isbn;
        private String title;
        private String author;
        private String publisher;
        private String category;
        private BigDecimal price;
        private String summary;
        private Integer publishDate;
        private String coverUrl;
        private String status;
        private Integer availableCopies;
        private Integer borrowCount;
        private String createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getIsbn() { return isbn; }
        public void setIsbn(String isbn) { this.isbn = isbn; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getPublisher() { return publisher; }
        public void setPublisher(String publisher) { this.publisher = publisher; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public Integer getPublishDate() { return publishDate; }
        public void setPublishDate(Integer publishDate) { this.publishDate = publishDate; }
        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getAvailableCopies() { return availableCopies; }
        public void setAvailableCopies(Integer availableCopies) { this.availableCopies = availableCopies; }
        public Integer getBorrowCount() { return borrowCount; }
        public void setBorrowCount(Integer borrowCount) { this.borrowCount = borrowCount; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}
