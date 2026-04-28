package com.library.stats.service;

import com.library.stats.dto.ChartData;
import com.library.stats.dto.DashboardStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final RedisTemplate<String, Object> redisTemplate;

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

        stats.setTotalBooks(getCounter("total:books", 120L));
        stats.setAvailableBooks(getCounter("available:books", 95L));
        stats.setTotalUsers(getCounter("total:users", 500L));
        stats.setTotalBorrows(getCounter("total:borrows", 1250L));
        stats.setActiveBorrows(getCounter("active:borrows", 35L));
        stats.setOverdueCount(getCounter("overdue:count", 3L));

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
        redisTemplate.opsForHash().increment(KEY_BORROW_TREND, dateKey, 1);
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

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateKey = date.format(dateFormatter);
            String label = date.format(monthFormatter);

            Object value = redisTemplate.opsForHash().get(KEY_BORROW_TREND, dateKey);
            long count = value != null ? ((Number) value).longValue() : getRandomCount(10, 50);

            trend.add(ChartData.of(label, count));
        }

        return trend;
    }

    private List<ChartData> getCategoryDistribution() {
        Map<Object, Object> categoryMap = redisTemplate.opsForHash().entries(KEY_CATEGORY_DISTRIBUTION);

        if (categoryMap.isEmpty()) {
            return Arrays.asList(
                    ChartData.of("文学", 35),
                    ChartData.of("技术", 25),
                    ChartData.of("历史", 15),
                    ChartData.of("教育", 10),
                    ChartData.of("艺术", 10),
                    ChartData.of("其他", 5)
            );
        }

        List<ChartData> result = new ArrayList<>();
        categoryMap.forEach((k, v) -> result.add(ChartData.of(String.valueOf(k), v)));
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
        Set<Object> popularBooks = redisTemplate.opsForZSet().reverseRange(KEY_POPULAR_BOOKS, 0, 9);

        if (popularBooks == null || popularBooks.isEmpty()) {
            return Arrays.asList(
                    ChartData.of("三体", 156),
                    ChartData.of("活着", 142),
                    ChartData.of("百年孤独", 128),
                    ChartData.of("人类简史", 115),
                    ChartData.of("算法导论", 98),
                    ChartData.of("明朝那些事儿", 89),
                    ChartData.of("深度学习", 76),
                    ChartData.of("解忧杂货店", 65)
            );
        }

        List<ChartData> result = new ArrayList<>();
        for (Object book : popularBooks) {
            Double score = redisTemplate.opsForZSet().score(KEY_POPULAR_BOOKS, book);
            result.add(ChartData.of(String.valueOf(book), score != null ? score.longValue() : 0L));
        }
        return result;
    }

    private List<ChartData> getHourlyDistribution() {
        List<ChartData> distribution = new ArrayList<>();

        for (int hour = 8; hour <= 20; hour++) {
            String hourKey = String.format("%02d", hour);
            Object value = redisTemplate.opsForHash().get(KEY_HOURLY_DISTRIBUTION, hourKey);
            long count = value != null ? ((Number) value).longValue() : getRandomCount(5, 30);
            distribution.add(ChartData.of(hourKey + ":00", count));
        }

        return distribution;
    }

    private Map<String, Long> getRecentActivity() {
        Map<String, Long> activity = new LinkedHashMap<>();
        activity.put("借书", getTodayCounter("borrow"));
        activity.put("还书", getTodayCounter("return"));
        activity.put("注册用户", getRandomCount(0, 5));
        activity.put("新图书", getRandomCount(0, 3));
        return activity;
    }

    private long getCounter(String key, long defaultValue) {
        Object value = redisTemplate.opsForValue().get(KEY_STATS_PREFIX + key);
        return value != null ? ((Number) value).longValue() : defaultValue;
    }

    private void incrementCounter(String key) {
        redisTemplate.opsForValue().increment(KEY_STATS_PREFIX + key, 1);
    }

    private void decrementCounter(String key) {
        redisTemplate.opsForValue().decrement(KEY_STATS_PREFIX + key, 1);
    }

    private long getTodayCounter(String type) {
        String today = LocalDate.now().format(dateFormatter);
        Object value = redisTemplate.opsForHash().get(KEY_TODAY_COUNTER + ":" + today, type);
        return value != null ? ((Number) value).longValue() : getRandomCount(5, 20);
    }

    private void incrementTodayCounter(String type) {
        String today = LocalDate.now().format(dateFormatter);
        redisTemplate.opsForHash().increment(KEY_TODAY_COUNTER + ":" + today, type, 1);
    }

    private void recordHourlyActivity(String type) {
        int hour = LocalDateTime.now().getHour();
        String hourKey = String.format("%02d", hour);
        redisTemplate.opsForHash().increment(KEY_HOURLY_DISTRIBUTION, hourKey, 1);
    }

    private void incrementBookBorrowCount(Long bookId, String bookTitle) {
        redisTemplate.opsForZSet().incrementScore(KEY_POPULAR_BOOKS, bookTitle, 1);
    }

    private long getRandomCount(int min, int max) {
        return min + new Random().nextInt(max - min + 1);
    }
}
