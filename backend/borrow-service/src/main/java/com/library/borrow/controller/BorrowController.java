package com.library.borrow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.borrow.dto.BorrowDTO.*;
import com.library.borrow.entity.BorrowRecord;
import com.library.borrow.service.BorrowService;
import com.library.common.result.PageResult;
import com.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "借阅管理接口")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @Operation(summary = "借阅图书")
    @PostMapping("/borrow")
    public Result<BorrowResponse> borrowBook(
            @Valid @RequestBody BorrowRequest request,
            HttpServletRequest servletRequest) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        
        BorrowRecord record = borrowService.borrowBook(userId, request);
        return Result.success("借阅成功", borrowService.toBorrowResponse(record));
    }

    @Operation(summary = "归还图书")
    @PostMapping("/return")
    public Result<ReturnResponse> returnBook(
            @Valid @RequestBody ReturnRequest request,
            HttpServletRequest servletRequest) {
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        
        ReturnResponse response = borrowService.returnBook(userId, request);
        return Result.success("归还成功", response);
    }

    @Operation(summary = "获取借阅记录列表")
    @GetMapping("/borrow-records")
    public Result<PageResult<BorrowResponse>> listRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status) {
        
        Page<BorrowRecord> pageResult = borrowService.pageRecords(page, size, userId, status);
        
        List<BorrowResponse> records = pageResult.getRecords().stream()
                .map(borrowService::toBorrowResponse)
                .collect(Collectors.toList());
        
        PageResult<BorrowResponse> result = PageResult.of(
                records,
                pageResult.getTotal(),
                pageResult.getSize(),
                pageResult.getCurrent()
        );
        
        return Result.success(result);
    }

    @Operation(summary = "获取我的借阅记录")
    @GetMapping("/my-borrows")
    public Result<List<MyBorrowItem>> getMyBorrows(
            @RequestParam(required = false, defaultValue = "all") String status,
            HttpServletRequest servletRequest) {
        
        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        
        List<BorrowRecord> records;
        if ("active".equals(status)) {
            records = borrowService.getActiveByUserId(userId);
        } else {
            Page<BorrowRecord> pageResult = borrowService.pageRecords(1, 1000, userId, status);
            records = pageResult.getRecords();
        }
        
        List<MyBorrowItem> items = records.stream()
                .map(this::toMyBorrowItem)
                .collect(Collectors.toList());
        
        return Result.success(items);
    }

    @Operation(summary = "根据ID获取借阅记录")
    @GetMapping("/borrow-records/{id}")
    public Result<BorrowResponse> getRecordById(@PathVariable Long id) {
        BorrowRecord record = borrowService.getById(id);
        return Result.success(borrowService.toBorrowResponse(record));
    }

    private MyBorrowItem toMyBorrowItem(BorrowRecord record) {
        MyBorrowItem item = new MyBorrowItem();
        item.setId(record.getRecordId());
        item.setRecordId(record.getRecordId());
        item.setBorrowDate(record.getBorrowDate());
        item.setDueDate(record.getDueDate());
        item.setReturnDate(record.getReturnDate());
        item.setStatus(record.getStatus());
        return item;
    }
}
