package com.library.common.constant;

public class Constants {

    private Constants() {
    }

    public static final String DEFAULT_CHARSET = "UTF-8";

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_NAME_HEADER = "X-User-Name";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_READER = "reader";
    public static final String ROLE_SUPER_ADMIN = "super_admin";

    public static final String BOOK_STATUS_AVAILABLE = "available";
    public static final String BOOK_STATUS_BORROWED = "borrowed";
    public static final String BOOK_STATUS_FROZEN = "frozen";

    public static final String COPY_STATUS_AVAILABLE = "available";
    public static final String COPY_STATUS_BORROWED = "borrowed";
    public static final String COPY_STATUS_DAMAGED = "damaged";
    public static final String COPY_STATUS_LOST = "lost";

    public static final String BORROW_STATUS_ACTIVE = "active";
    public static final String BORROW_STATUS_RETURNED = "returned";
    public static final String BORROW_STATUS_OVERDUE = "overdue";

    public static final String USER_STATUS_ACTIVE = "active";
    public static final String USER_STATUS_INACTIVE = "inactive";

    public static final String REDIS_KEY_PREFIX = "library:";
    public static final String REDIS_BOOK_LIST_KEY = REDIS_KEY_PREFIX + "books:list:";
    public static final String REDIS_BOOK_DETAIL_KEY = REDIS_KEY_PREFIX + "books:detail:";
    public static final String REDIS_USER_SESSION_KEY = REDIS_KEY_PREFIX + "user:session:";
    public static final String REDIS_BORROW_LOCK_KEY = REDIS_KEY_PREFIX + "lock:borrow:";
    public static final String REDIS_STATS_KEY = REDIS_KEY_PREFIX + "stats:";
    public static final String REDIS_RANK_KEY = REDIS_KEY_PREFIX + "rank:";

    public static final String EXCHANGE_BORROW = "borrow.exchange";
    public static final String EXCHANGE_NOTIFY = "notify.exchange";
    public static final String EXCHANGE_STATS = "stats.exchange";

    public static final String QUEUE_BORROW_SUCCESS = "queue.borrow.success";
    public static final String QUEUE_BORROW_RETURN = "queue.borrow.return";
    public static final String QUEUE_NOTIFY_SMS = "queue.notify.sms";
    public static final String QUEUE_NOTIFY_EMAIL = "queue.notify.email";
    public static final String QUEUE_STATS_UPDATE = "queue.stats.update";

    public static final String ROUTING_KEY_BORROW_SUCCESS = "borrow.success";
    public static final String ROUTING_KEY_BORROW_RETURN = "borrow.return";
    public static final String ROUTING_KEY_NOTIFY_SMS = "notify.sms";
    public static final String ROUTING_KEY_NOTIFY_EMAIL = "notify.email";
    public static final String ROUTING_KEY_STATS_UPDATE = "stats.update";

    public static final int DEFAULT_BORROW_DAYS = 30;
    public static final int MAX_BORROW_COUNT = 5;
    public static final int MAX_RENEWAL_TIMES = 2;
    public static final double OVERDUE_FINE_PER_DAY = 0.5;
    public static final double MAX_FINE_AMOUNT = 50.0;
    public static final int REMINDER_DAYS_BEFORE_DUE = 3;

    public static final int CACHE_BOOK_LIST_TTL = 60;
    public static final int CACHE_BOOK_DETAIL_TTL = 120;
    public static final int CACHE_USER_SESSION_TTL = 7200;

    public static final int PAGE_DEFAULT = 1;
    public static final int SIZE_DEFAULT = 10;
    public static final int SIZE_MAX = 100;
}
