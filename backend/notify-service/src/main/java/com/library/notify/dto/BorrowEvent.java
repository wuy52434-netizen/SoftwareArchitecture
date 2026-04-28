package com.library.notify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowEvent implements Serializable {

    private Long recordId;
    private Long userId;
    private String username;
    private String userPhone;
    private String userEmail;
    private Long bookId;
    private String bookTitle;
    private String bookIsbn;
    private String copyBarcode;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String eventType;
    private Boolean isSuccess;
    private String failReason;
    private LocalDateTime eventTime;

    public static BorrowEvent borrowSuccess(Long recordId, Long userId, String username,
                                             Long bookId, String bookTitle,
                                             LocalDate borrowDate, LocalDate dueDate) {
        return BorrowEvent.builder()
                .recordId(recordId)
                .userId(userId)
                .username(username)
                .bookId(bookId)
                .bookTitle(bookTitle)
                .borrowDate(borrowDate)
                .dueDate(dueDate)
                .eventType("BORROW")
                .isSuccess(true)
                .eventTime(LocalDateTime.now())
                .build();
    }

    public static BorrowEvent returnSuccess(Long recordId, Long userId, String username,
                                             Long bookId, String bookTitle,
                                             LocalDate returnDate) {
        return BorrowEvent.builder()
                .recordId(recordId)
                .userId(userId)
                .username(username)
                .bookId(bookId)
                .bookTitle(bookTitle)
                .returnDate(returnDate)
                .eventType("RETURN")
                .isSuccess(true)
                .eventTime(LocalDateTime.now())
                .build();
    }

    public static BorrowEvent borrowFail(Long userId, String username,
                                          Long bookId, String bookTitle,
                                          String failReason) {
        return BorrowEvent.builder()
                .userId(userId)
                .username(username)
                .bookId(bookId)
                .bookTitle(bookTitle)
                .eventType("BORROW")
                .isSuccess(false)
                .failReason(failReason)
                .eventTime(LocalDateTime.now())
                .build();
    }
}
