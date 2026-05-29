package com.library.borrow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BorrowDTO {

    @Data
    public static class BorrowRequest {
        @NotNull(message = "图书ID不能为空")
        private Long bookId;

        private Long userId;
        private Long copyId;
        private LocalDate borrowDate;
        private LocalDate dueDate;
        private String readerId;
        private String readerName;
        private String note;
    }

    @Data
    public static class ReturnRequest {
        private Long bookId;
        private Long borrowId;
    }

    @Data
    public static class BorrowResponse {
        private Long recordId;
        private Long userId;
        private Long bookId;
        private Long copyId;
        private String copyBarcode;
        private String bookTitle;
        private String bookAuthor;
        private String bookCoverUrl;
        private String readerName;
        private String readerId;
        private LocalDate borrowDate;
        private LocalDate dueDate;
        private LocalDate returnDate;
        private String status;
        private BigDecimal fineAmount;
        private Integer finePaid;
        private String remark;
    }

    @Data
    public static class ReturnResponse {
        private Long recordId;
        private Long bookId;
        private LocalDate returnDate;
        private Integer overdueDays;
        private BigDecimal fineAmount;
        private String status;
    }

    @Data
    public static class MyBorrowItem {
        private Long id;
        private Long recordId;
        private String bookTitle;
        private String bookAuthor;
        private String bookCoverUrl;
        private Long copyId;
        private String copyBarcode;
        private LocalDate borrowDate;
        private LocalDate dueDate;
        private LocalDate returnDate;
        private String status;
    }
}
