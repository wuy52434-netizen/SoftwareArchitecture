package com.library.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BookDTO {

    @Data
    public static class BookCreateRequest {
        @NotBlank(message = "ISBN不能为空")
        private String isbn;

        @NotBlank(message = "书名不能为空")
        private String title;

        @NotBlank(message = "作者不能为空")
        private String author;

        @NotNull(message = "分类ID不能为空")
        private Long categoryId;

        @NotNull(message = "价格不能为空")
        @Positive(message = "价格必须为正数")
        private BigDecimal price;

        private Long publisherId;
        private String summary;
        private LocalDate publicationDate;
        private String coverImage;
        private Integer totalCopies;
        private String language;
        private String description;
        private String location;
    }

    @Data
    public static class BookUpdateRequest {
        private String title;
        private String author;
        private Long categoryId;
        private BigDecimal price;
        private String summary;
        private LocalDate publicationDate;
        private String coverImage;
        private String status;
        private String language;
        private String description;
        private String location;
        private Integer totalCopies;
    }

    @Data
    public static class BookResponse {
        private Long id;
        private String isbn;
        private String title;
        private String author;
        private String publisher;
        private String category;
        private BigDecimal price;
        private String summary;
        private String description;
        private LocalDate publicationDate;
        private Integer publishDate;
        private String coverUrl;
        private String coverImage;
        private Integer totalCopies;
        private Integer availableCopies;
        private Integer stock;
        private String status;
        private String language;
        private String location;
        private String createdAt;
    }

    @Data
    public static class CategoryCreateRequest {
        @NotBlank(message = "分类名称不能为空")
        private String categoryName;
        private String description;
    }

    @Data
    public static class CategoryResponse {
        private Long categoryId;
        private String categoryName;
        private String description;
    }

    @Data
    public static class CopyCreateRequest {
        @NotBlank(message = "ISBN不能为空")
        private String isbn;
        @NotBlank(message = "条形码不能为空")
        private String barcode;
        private Long locationId;
    }

    @Data
    public static class CopyResponse {
        private Long copyId;
        private Long bookId;
        private String isbn;
        private String barcode;
        private String status;
        private Long locationId;
        private String condition;
        private String storageDate;
        private BookResponse bookInfo;
    }

    @Data
    public static class ScanBookResponse {
        private String inputType;
        private Boolean copyAvailable;
        private BookResponse book;
        private CopyResponse copy;
    }
}
