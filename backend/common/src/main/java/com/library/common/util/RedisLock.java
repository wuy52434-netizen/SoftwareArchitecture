package com.library.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLock {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final long DEFAULT_WAIT_TIME = 3L;
    private static final long DEFAULT_LEASE_TIME = 5L;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    private static final String RELEASE_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    public LockResult tryLock(String key) {
        return tryLock(key, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, DEFAULT_TIME_UNIT);
    }

    public LockResult tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();
        long waitMillis = unit.toMillis(waitTime);
        long startTime = System.currentTimeMillis();

        try {
            while (System.currentTimeMillis() - startTime < waitMillis) {
                Boolean locked = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, lockValue, leaseTime, unit);
                
                if (Boolean.TRUE.equals(locked)) {
                    log.debug("获取锁成功: key={}, value={}", lockKey, lockValue);
                    return new LockResult(true, lockKey, lockValue);
                }

                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取锁被中断: key={}", lockKey);
        }

        log.debug("获取锁失败: key={}", lockKey);
        return new LockResult(false, lockKey, lockValue);
    }

    public boolean unlock(LockResult lockResult) {
        if (lockResult == null || !lockResult.isLocked()) {
            return false;
        }

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(RELEASE_SCRIPT);
            script.setResultType(Long.class);

            Long result = redisTemplate.execute(
                    script,
                    Collections.singletonList(lockResult.getLockKey()),
                    lockResult.getLockValue()
            );

            boolean success = result != null && result > 0;
            if (success) {
                log.debug("释放锁成功: key={}", lockResult.getLockKey());
            } else {
                log.debug("释放锁失败，锁可能已过期或被其他线程持有: key={}", lockResult.getLockKey());
            }
            return success;
        } catch (Exception e) {
            log.error("释放锁异常: key={}", lockResult.getLockKey(), e);
            return false;
        }
    }

    @RequiredArgsConstructor
    public static class LockResult implements AutoCloseable {
        private final boolean locked;
        private final String lockKey;
        private final String lockValue;
        private final RedisLock lock;

        public LockResult(boolean locked, String lockKey, String lockValue) {
            this.locked = locked;
            this.lockKey = lockKey;
            this.lockValue = lockValue;
            this.lock = null;
        }

        public boolean isLocked() {
            return locked;
        }

        public String getLockKey() {
            return lockKey;
        }

        public String getLockValue() {
            return lockValue;
        }

        @Override
        public void close() {
            if (lock != null) {
                lock.unlock(this);
            }
        }
    }

    public LockExecutor lockExecutor(String key) {
        return new LockExecutor(this, key, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, DEFAULT_TIME_UNIT);
    }

    public LockExecutor lockExecutor(String key, long waitTime, long leaseTime, TimeUnit unit) {
        return new LockExecutor(this, key, waitTime, leaseTime, unit);
    }

    @RequiredArgsConstructor
    public static class LockExecutor {
        private final RedisLock redisLock;
        private final String key;
        private final long waitTime;
        private final long leaseTime;
        private final TimeUnit unit;

        public <T> T execute(LockCallback<T> callback) {
            LockResult lockResult = redisLock.tryLock(key, waitTime, leaseTime, unit);
            if (!lockResult.isLocked()) {
                throw new RuntimeException("获取锁失败: " + key);
            }
            try {
                return callback.execute();
            } finally {
                redisLock.unlock(lockResult);
            }
        }

        public void execute(Runnable runnable) {
            LockResult lockResult = redisLock.tryLock(key, waitTime, leaseTime, unit);
            if (!lockResult.isLocked()) {
                throw new RuntimeException("获取锁失败: " + key);
            }
            try {
                runnable.run();
            } finally {
                redisLock.unlock(lockResult);
            }
        }
    }

    @FunctionalInterface
    public interface LockCallback<T> {
        T execute();
    }
}
