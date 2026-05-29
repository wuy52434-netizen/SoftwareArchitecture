package com.library.user.controller;

import com.library.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "系统设置接口")
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "获取系统设置")
    @GetMapping
    public Result<Map<String, Object>> getSettings() {
        Map<String, Object> settings = loadSettings();
        settings.put("systemName", "图书自动借书机系统");
        settings.put("systemVersion", "1.0.0");
        return Result.success(settings);
    }

    @Operation(summary = "更新系统设置")
    @PutMapping
    public Result<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> settings) {
        saveSetting("default_borrow_days", settings.get("borrowDays"));
        saveSetting("max_borrow_count", settings.get("maxBorrowCount"));
        saveSetting("max_renewal_times", settings.get("maxRenewalTimes"));
        saveSetting("overdue_fine_per_day", settings.get("overdueFinePerDay"));
        saveSetting("max_fine_amount", settings.get("maxFineAmount"));
        saveSetting("reminder_days_before_due", settings.get("reminderDaysBeforeDue"));
        return Result.success(loadSettings());
    }

    private Map<String, Object> loadSettings() {
        Map<String, Object> values = new HashMap<>();
        jdbcTemplate.query("SELECT setting_key, setting_value FROM system_settings", rs -> {
            values.put(rs.getString("setting_key"), rs.getString("setting_value"));
        });

        Map<String, Object> settings = new HashMap<>();
        settings.put("borrowDays", toInt(values.get("default_borrow_days"), 30));
        settings.put("maxBorrowCount", toInt(values.get("max_borrow_count"), 5));
        settings.put("maxRenewalTimes", toInt(values.get("max_renewal_times"), 2));
        settings.put("overdueFinePerDay", toDouble(values.get("overdue_fine_per_day"), 0.5));
        settings.put("maxFineAmount", toDouble(values.get("max_fine_amount"), 50.0));
        settings.put("reminderDaysBeforeDue", toInt(values.get("reminder_days_before_due"), 3));
        return settings;
    }

    private void saveSetting(String key, Object value) {
        if (value == null) {
            return;
        }

        int updated = jdbcTemplate.update(
                "UPDATE system_settings SET setting_value = ?, updated_at = NOW() WHERE setting_key = ?",
                String.valueOf(value),
                key
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO system_settings(setting_key, setting_value, description) VALUES (?, ?, ?)",
                    key,
                    String.valueOf(value),
                    key
            );
        }
    }

    private int toInt(Object value, int defaultValue) {
        try {
            return value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double toDouble(Object value, double defaultValue) {
        try {
            return value == null ? defaultValue : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
