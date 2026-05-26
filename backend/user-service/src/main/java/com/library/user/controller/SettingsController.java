package com.library.user.controller;

import com.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "系统设置接口")
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    @Operation(summary = "获取系统设置")
    @GetMapping
    public Result<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("borrowDays", 30);
        settings.put("maxBorrowCount", 5);
        settings.put("overdueFinePerDay", 0.5);
        settings.put("maxFineAmount", 50);
        settings.put("systemName", "图书自动借书机系统");
        settings.put("systemVersion", "1.0.0");
        return Result.success(settings);
    }

    @Operation(summary = "更新系统设置")
    @PutMapping
    public Result<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> settings) {
        return Result.success(settings);
    }
}