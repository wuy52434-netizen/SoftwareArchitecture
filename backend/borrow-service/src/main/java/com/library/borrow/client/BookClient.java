package com.library.borrow.client;

import com.library.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "book-service", fallback = BookClientFallback.class)
public interface BookClient {

    @GetMapping("/api/books/{id}")
    Result<BookInfo> getBookById(@PathVariable Long id);

    @PutMapping("/api/books/{id}/decrease-stock")
    Result<BookCopy> decreaseStock(@PathVariable Long id, @RequestParam Long copyId);

    @PutMapping("/api/books/{id}/increase-stock")
    Result<BookCopy> increaseStock(@PathVariable Long id, @RequestParam Long copyId);

    @GetMapping("/api/books/copy/{copyId}")
    Result<BookCopy> getCopyById(@PathVariable Long copyId);

    @PutMapping("/api/books/copy/{copyId}/status")
    Result<Void> updateCopyStatus(@PathVariable Long copyId, @RequestParam String status);

    class BookInfo {
        private Long id;
        private String title;
        private String author;
        private String isbn;
        private String coverImage;
        private String categoryName;
        private Integer totalCopies;
        private Integer availableCopies;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getIsbn() { return isbn; }
        public void setIsbn(String isbn) { this.isbn = isbn; }
        public String getCoverImage() { return coverImage; }
        public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public Integer getTotalCopies() { return totalCopies; }
        public void setTotalCopies(Integer totalCopies) { this.totalCopies = totalCopies; }
        public Integer getAvailableCopies() { return availableCopies; }
        public void setAvailableCopies(Integer availableCopies) { this.availableCopies = availableCopies; }
    }

    class BookCopy {
        private Long id;
        private Long bookId;
        private String copyNumber;
        private String location;
        private String status;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }
        public String getCopyNumber() { return copyNumber; }
        public void setCopyNumber(String copyNumber) { this.copyNumber = copyNumber; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
