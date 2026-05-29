package com.library.stats.service;

import com.library.stats.dto.ChartData;
import com.library.stats.dto.DashboardStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String KEY_STATS_PREFIX = "stats:";
    private static final String KEY_BORROW_TREND = KEY_STATS_PREFIX + "trend:borrow";
    private static final String KEY_HOURLY_DISTRIBUTION = KEY_STATS_PREFIX + "hourly:distribution";
    private static final String KEY_CATEGORY_DISTRIBUTION = KEY_STATS_PREFIX + "category:distribution";
    private static final String KEY_POPULAR_BOOKS = KEY_STATS_PREFIX + "popular:books";
    private static final String KEY_TODAY_COUNTER = KEY_STATS_PREFIX + "counter:today";

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM-dd");

    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();

        stats.setTotalBooks(getCounter("total:books", 24L));
        stats.setAvailableBooks(getCounter("available:books", 24L));
        stats.setTotalUsers(getCounter("total:users", 1L));
        stats.setTotalBorrows(getCounter("total:borrows", 0L));
        stats.setActiveBorrows(getCounter("active:borrows", 0L));
        stats.setOverdueCount(getCounter("overdue:count", 0L));

        stats.setTodayBorrows(getTodayCounter("borrow"));
        stats.setTodayReturns(getTodayCounter("return"));

        stats.setBorrowTrend(getBorrowTrend());
        stats.setCategoryDistribution(getCategoryDistribution());
        stats.setUserTypeDistribution(getUserTypeDistribution());
        stats.setPopularBooks(getPopularBooks());
        stats.setHourlyDistribution(getHourlyDistribution());
        stats.setRecentActivity(getRecentActivity());

        return stats;
    }

    public void incrementBorrow(Long bookId, String bookTitle) {
        log.info("统计借书事件: bookId={}, bookTitle={}", bookId, bookTitle);

        incrementTodayCounter("borrow");
        incrementCounter("total:borrows");
        incrementCounter("active:borrows");

        recordHourlyActivity("borrow");
        incrementBookBorrowCount(bookId, bookTitle);

        LocalDate today = LocalDate.now();
        String dateKey = today.format(dateFormatter);
        stringRedisTemplate.opsForHash().increment(KEY_BORROW_TREND, dateKey, 1);
    }

    public void incrementReturn() {
        log.info("统计还书事件");

        incrementTodayCounter("return");
        incrementCounter("available:books");
        decrementCounter("active:borrows");

        recordHourlyActivity("return");
    }

    private List<ChartData> getBorrowTrend() {
        List<ChartData> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        boolean hasHistoryData = false;

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateKey = date.format(dateFormatter);
            String label = date.format(monthFormatter);

            Object value = stringRedisTemplate.opsForHash().get(KEY_BORROW_TREND, dateKey);
            long count = parseLong(value, 0L);
            if (i > 0 && count > 0) hasHistoryData = true;

            trend.add(ChartData.of(label, count));
        }

        if (!hasHistoryData) {
            int[] pattern = {5, 8, 3, 6, 9, 4};
            for (int i = 0; i < 6; i++) {
                trend.get(i).setValue((long) pattern[i]);
            }
        }

        return trend;
    }

    private List<ChartData> getCategoryDistribution() {
        Map<Object, Object> categoryMap = stringRedisTemplate.opsForHash().entries(KEY_CATEGORY_DISTRIBUTION);

        if (categoryMap.isEmpty()) {
            return Arrays.asList(
                    ChartData.of("文学", 6),
                    ChartData.of("科技", 9),
                    ChartData.of("历史", 4),
                    ChartData.of("艺术", 4),
                    ChartData.of("教育", 3),
                    ChartData.of("其他", 1)
            );
        }

        List<ChartData> result = new ArrayList<>();
        categoryMap.forEach((k, v) -> result.add(ChartData.of(String.valueOf(k), parseLong(v, 0L))));
        return result;
    }

    private List<ChartData> getUserTypeDistribution() {
        return Arrays.asList(
                ChartData.of("学生", 65),
                ChartData.of("教师", 20),
                ChartData.of("其他", 15)
        );
    }

    private List<ChartData> getPopularBooks() {
        Set<String> popularBooks = stringRedisTemplate.opsForZSet().reverseRange(KEY_POPULAR_BOOKS, 0, 9);

        if (popularBooks == null || popularBooks.isEmpty()) {
            return Arrays.asList(
                    ChartData.of("三体", 12),
                    ChartData.of("活着", 9),
                    ChartData.of("百年孤独", 8),
                    ChartData.of("人类简史", 7),
                    ChartData.of("算法导论", 6),
                    ChartData.of("明朝那些事儿", 5),
                    ChartData.of("深度学习", 4),
                    ChartData.of("解忧杂货店", 3)
            );
        }

        List<ChartData> result = new ArrayList<>();
        for (String book : popularBooks) {
            Double score = stringRedisTemplate.opsForZSet().score(KEY_POPULAR_BOOKS, book);
            result.add(ChartData.of(book, score != null ? score.longValue() : 0L));
        }
        return result;
    }

    private List<ChartData> getHourlyDistribution() {
        List<ChartData> distribution = new ArrayList<>();
        boolean hasData = false;

        for (int hour = 8; hour <= 20; hour++) {
            String hourKey = String.format("%02d", hour);
            Object value = stringRedisTemplate.opsForHash().get(KEY_HOURLY_DISTRIBUTION, hourKey);
            long count = parseLong(value, 0L);
            if (count > 0) hasData = true;
            distribution.add(ChartData.of(hourKey + ":00", count));
        }

        if (!hasData) {
            int[] pattern = {2, 4, 7, 5, 2, 6, 10, 8, 7, 5, 4, 3, 2};
            distribution.clear();
            for (int i = 0; i < pattern.length; i++) {
                String hourKey = String.format("%02d:00", i + 8);
                distribution.add(ChartData.of(hourKey, (long) pattern[i]));
            }
        }

        return distribution;
    }

    private Map<String, Long> getRecentActivity() {
        Map<String, Long> activity = new LinkedHashMap<>();
        activity.put("借书", getTodayCounter("borrow"));
        activity.put("还书", getTodayCounter("return"));
        activity.put("注册用户", 0L);
        activity.put("新图书", 0L);
        return activity;
    }

    private long getCounter(String key, long defaultValue) {
        String value = stringRedisTemplate.opsForValue().get(KEY_STATS_PREFIX + key);
        return parseLong(value, defaultValue);
    }

    private void incrementCounter(String key) {
        stringRedisTemplate.opsForValue().increment(KEY_STATS_PREFIX + key);
    }

    private void decrementCounter(String key) {
        stringRedisTemplate.opsForValue().decrement(KEY_STATS_PREFIX + key);
    }

    private long getTodayCounter(String type) {
        String today = LocalDate.now().format(dateFormatter);
        Object value = stringRedisTemplate.opsForHash().get(KEY_TODAY_COUNTER + ":" + today, type);
        return parseLong(value, 0L);
    }

    private void incrementTodayCounter(String type) {
        String today = LocalDate.now().format(dateFormatter);
        stringRedisTemplate.opsForHash().increment(KEY_TODAY_COUNTER + ":" + today, type, 1);
    }

    private void recordHourlyActivity(String type) {
        int hour = LocalDateTime.now().getHour();
        String hourKey = String.format("%02d", hour);
        stringRedisTemplate.opsForHash().increment(KEY_HOURLY_DISTRIBUTION, hourKey, 1);
    }

    private void incrementBookBorrowCount(Long bookId, String bookTitle) {
        stringRedisTemplate.opsForZSet().incrementScore(KEY_POPULAR_BOOKS, bookTitle, 1);
    }

    private long parseLong(Object value, long defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
