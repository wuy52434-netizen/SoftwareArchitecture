package com.library.stats.controller;

import com.library.common.result.Result;
import com.library.stats.dto.DashboardStats;
import com.library.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/dashboard")
    public Result<DashboardStats> getDashboardStats() {
        DashboardStats stats = statsService.getDashboardStats();
        return Result.success(stats);
    }

    @PostMapping("/borrow")
    public Result<Void> recordBorrow(
            @RequestParam Long bookId,
            @RequestParam String bookTitle) {
        statsService.incrementBorrow(bookId, bookTitle);
        return Result.success();
    }

    @PostMapping("/return")
    public Result<Void> recordReturn() {
        statsService.incrementReturn();
        return Result.success();
    }
}
