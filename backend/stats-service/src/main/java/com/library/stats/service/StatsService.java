package com.library.stats.service;

import com.library.common.result.Result;
import com.library.stats.client.BookClient;
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
    private final BookClient bookClient;

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

        // 图书数取自图书服务（唯一权威源），与 `GET /api/books` 的 total 同源同口径。
        // 修复前这里是 getCounter("total:books", 24L) —— 读本地 Redis 计数器，
        // 初值还是硬编码 24，所以看板永远与图书接口对不上（实测 24 vs 25）。
        stats.setTotalBooks(resolveBookCount(bookClient.listBooks(1, 1, null), "total:books"));
        stats.setAvailableBooks(resolveBookCount(bookClient.listBooks(1, 1, "available"), "available:books"));
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

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateKey = date.format(dateFormatter);
            String label = date.format(monthFormatter);

            Object value = stringRedisTemplate.opsForHash().get(KEY_BORROW_TREND, dateKey);
            long count = parseLong(value, 0L);

            trend.add(ChartData.of(label, count));
        }

        // 修复 KNWN-DEF-02：原先在"没有历史数据"时用一段写死的波形 {5,8,3,6,9,4} 填充，
        // 前端把它当真实趋势展示 —— 这是编造统计。没有数据就应当显示 0，
        // 由前端呈现"暂无数据"，而不是让系统伪造一个好看但虚假的走势。
        return trend;
    }

    private List<ChartData> getCategoryDistribution() {
        Map<Object, Object> categoryMap = stringRedisTemplate.opsForHash().entries(KEY_CATEGORY_DISTRIBUTION);

        // 同上：原先在 map 为空时返回一组写死的分类分布（文学 6 / 科技 9 / ...），已移除。
        if (categoryMap.isEmpty()) {
            log.debug("分类分布暂无统计数据（未收到借阅事件），返回空列表而非编造值");
            return Collections.emptyList();
        }

        List<ChartData> result = new ArrayList<>();
        categoryMap.forEach((k, v) -> result.add(ChartData.of(String.valueOf(k), parseLong(v, 0L))));
        return result;
    }

    /**
     * 用户类型分布。
     *
     * <p>修复 KNWN-DEF-02：原实现直接返回写死的 65/20/15，**从不查库**，
     * 与真实用户表毫无关系，却作为"统计"展示在管理后台。
     *
     * <p>为什么现在返回空列表而不是"顺手算一下"：真实数据在 user-service，
     * 其 {@code GET /api/users} 受 {@code @PreAuthorize("hasRole('ADMIN')")} 保护
     * （实测内部无鉴权调用返回 500）。要取这份数据必须先建立**服务间鉴权**
     * （例如 service token 或内部专用只读接口），那是安全设计任务；
     * 在它完成之前，正确的做法是明确"暂无数据"，而不是继续编造。
     */
    private List<ChartData> getUserTypeDistribution() {
        log.debug("用户类型分布需要 user-service 的服务间鉴权接口，暂以空列表表示「暂无数据」");
        return Collections.emptyList();
    }

    private List<ChartData> getPopularBooks() {
        Set<String> popularBooks = stringRedisTemplate.opsForZSet().reverseRange(KEY_POPULAR_BOOKS, 0, 9);

        // 原先在无数据时返回写死的热门榜（三体 12 / 活着 9 / ...），同样属于编造，已移除。
        if (popularBooks == null || popularBooks.isEmpty()) {
            return Collections.emptyList();
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

        for (int hour = 8; hour <= 20; hour++) {
            String hourKey = String.format("%02d", hour);
            Object value = stringRedisTemplate.opsForHash().get(KEY_HOURLY_DISTRIBUTION, hourKey);
            long count = parseLong(value, 0L);
            distribution.add(ChartData.of(hourKey + ":00", count));
        }

        // 原先在整点为 0 时用写死的分布覆盖，已移除（KNWN-DEF-02）。
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

    /**
     * 取权威图书数；图书服务不可用时退回本地计数器并告警。
     *
     * <p>降级时刻意"退回旧值 + 告警"而不是返回 0：看板上出现 0 会被当成"库空了"，
     * 而略旧的真实值不会误导人，配合告警日志即可定位。
     */
    private long resolveBookCount(Result<BookClient.BookPage> result, String counterKey) {
        if (result != null && result.isSuccess()
                && result.getData() != null && result.getData().getTotal() != null) {
            return result.getData().getTotal();
        }
        long fallback = getCounter(counterKey, 0L);
        log.warn("图书服务计数不可用，看板退回本地计数器 {}={}（口径可能与 /api/books 不一致）",
                counterKey, fallback);
        return fallback;
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
