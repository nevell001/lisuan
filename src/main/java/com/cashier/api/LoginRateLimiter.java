package com.cashier.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 登录失败限流器（按客户端 IP 内存跟踪）
 *
 * <p>背景：登录锁定此前写共享的 login_attempts 表（桌面端与 API 共用），
 * 未认证攻击者只需对已知用户名连发错误密码即可反复锁死真实账号（DoS），
 * 且不存在的用户名也会在表中留下记录（表膨胀）。</p>
 *
 * <p>本限流器改为纯内存、按 IP 计数：5 次失败锁定该 IP 1 分钟，
 * 锁定期间继续尝试按指数退避延长（上限 1 小时）；空闲一段时间后计数自然衰减，
 * 不影响正常用户。API 登录失败不再写 login_attempts 表，无法被用于锁死账号或撑大表。</p>
 *
 * <p>实例方法便于测试；生产使用 {@link #getInstance()}。</p>
 */
public final class LoginRateLimiter {

    private static final int DEFAULT_MAX_FAILURES = 5;
    private static final long DEFAULT_BASE_LOCK_MILLIS = 60_000;      // 1 分钟
    private static final long DEFAULT_MAX_LOCK_MILLIS = 3_600_000;    // 1 小时
    private static final long DEFAULT_IDLE_RESET_MILLIS = 10 * 60_000; // 10 分钟
    private static final int MAX_TRACKED_IPS = 10_000;

    private final int maxFailures;
    private final long baseLockMillis;
    private final long maxLockMillis;
    private final long idleResetMillis;
    private final ConcurrentHashMap<String, Entry> attempts = new ConcurrentHashMap<>();

    private static final class Holder {
        private static final LoginRateLimiter INSTANCE = new LoginRateLimiter(
            DEFAULT_MAX_FAILURES, DEFAULT_BASE_LOCK_MILLIS, DEFAULT_MAX_LOCK_MILLIS,
            DEFAULT_IDLE_RESET_MILLIS);
    }

    public static LoginRateLimiter getInstance() {
        return Holder.INSTANCE;
    }

    /** 供测试注入小参数 */
    LoginRateLimiter(int maxFailures, long baseLockMillis, long maxLockMillis, long idleResetMillis) {
        this.maxFailures = maxFailures;
        this.baseLockMillis = baseLockMillis;
        this.maxLockMillis = maxLockMillis;
        this.idleResetMillis = idleResetMillis;
    }

    private static final class Entry {
        int failures;
        long lockExpireMillis; // 0 = 未锁定
        long lastFailureMillis;
    }

    /**
     * 记录一次失败登录（在密码校验失败后调用）
     * @return 更新后的失败次数
     */
    public int recordFailure(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = "unknown";
        }
        pruneIfNeeded();

        long now = System.currentTimeMillis();
        Entry entry = attempts.computeIfAbsent(clientIp, k -> new Entry());
        synchronized (entry) {
            if (now - entry.lastFailureMillis > idleResetMillis) {
                entry.failures = 0;
                entry.lockExpireMillis = 0;
            }
            entry.failures++;
            entry.lastFailureMillis = now;
            if (entry.failures >= maxFailures) {
                long exponent = Math.min(entry.failures - maxFailures, 20);
                long lockMillis = Math.min(maxLockMillis, baseLockMillis * (1L << exponent));
                entry.lockExpireMillis = now + lockMillis;
            }
            return entry.failures;
        }
    }

    /**
     * 判断该 IP 是否处于锁定状态（在登录入口最先检查）
     */
    public boolean isBlocked(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return false;
        }
        Entry entry = attempts.get(clientIp);
        if (entry == null) {
            return false;
        }
        synchronized (entry) {
            return entry.lockExpireMillis > System.currentTimeMillis();
        }
    }

    /**
     * 剩余锁定毫秒数（未锁定时返回 0）
     */
    public long remainingLockMillis(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return 0;
        }
        Entry entry = attempts.get(clientIp);
        if (entry == null) {
            return 0;
        }
        synchronized (entry) {
            long remaining = entry.lockExpireMillis - System.currentTimeMillis();
            return Math.max(0, remaining);
        }
    }

    /**
     * 登录成功后清空该 IP 的失败记录
     */
    public void reset(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return;
        }
        attempts.remove(clientIp);
    }

    /** 简单防膨胀：超过上限时清掉已过期的条目 */
    private void pruneIfNeeded() {
        if (attempts.size() < MAX_TRACKED_IPS) {
            return;
        }
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(e -> {
            Entry entry = e.getValue();
            synchronized (entry) {
                return entry.lockExpireMillis <= now
                    && now - entry.lastFailureMillis > idleResetMillis;
            }
        });
    }
}
