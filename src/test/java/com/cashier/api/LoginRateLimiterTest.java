package com.cashier.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * API 登录限流器测试
 * 回归 P1：公开登录口失败次数达到阈值后按 IP 锁定（内存、指数退避、可恢复），
 * 不允许未认证者借 API 锁死账号或写库膨胀。
 */
@DisplayName("API 登录限流器测试")
class LoginRateLimiterTest {

    private final LoginRateLimiter limiter = new LoginRateLimiter(3, 60_000, 300_000, 600_000);

    @Test
    @DisplayName("未达阈值不锁定，达阈值后按 IP 锁定")
    void blocksAfterThreshold() {
        String ip = "203.0.113.9";
        assertFalse(limiter.isBlocked(ip), "初始不应锁定");

        assertEquals(1, limiter.recordFailure(ip));
        assertFalse(limiter.isBlocked(ip), "1 次失败不应锁定");

        limiter.recordFailure(ip);
        assertFalse(limiter.isBlocked(ip), "2 次失败不应锁定");

        limiter.recordFailure(ip);
        assertTrue(limiter.isBlocked(ip), "达到阈值后应锁定该 IP");
        assertTrue(limiter.remainingLockMillis(ip) > 0, "锁定后应有剩余时间");
    }

    @Test
    @DisplayName("锁定只影响来源 IP，不影响其他 IP")
    void lockIsPerIp() {
        limiter.recordFailure("198.51.100.1");
        limiter.recordFailure("198.51.100.1");
        limiter.recordFailure("198.51.100.1");

        assertTrue(limiter.isBlocked("198.51.100.1"), "来源 IP 应被锁定");
        assertFalse(limiter.isBlocked("198.51.100.2"), "其他 IP 不受影响");
    }

    @Test
    @DisplayName("登录成功后重置，不再锁定")
    void resetClearsBlock() {
        String ip = "203.0.113.20";
        limiter.recordFailure(ip);
        limiter.recordFailure(ip);
        limiter.recordFailure(ip);
        assertTrue(limiter.isBlocked(ip));

        limiter.reset(ip);

        assertFalse(limiter.isBlocked(ip), "重置后应解锁");
        assertEquals(1, limiter.recordFailure(ip), "重置后从 1 次开始计数");
    }

    @Test
    @DisplayName("锁定期过后自动解锁；空闲一段时间后计数衰减")
    void lockExpiresAndIdleDecays() throws Exception {
        // 50ms 后过期、150ms 空闲即衰减，便于快速验证
        LoginRateLimiter fast = new LoginRateLimiter(3, 60, 300, 150);

        String ip = "203.0.113.30";
        fast.recordFailure(ip);
        fast.recordFailure(ip);
        fast.recordFailure(ip);
        assertTrue(fast.isBlocked(ip));

        Thread.sleep(120);
        assertFalse(fast.isBlocked(ip), "锁定期过后应自动解锁");

        // 空闲超过 idleReset 后，下一次失败从 1 开始重新计数
        Thread.sleep(200);
        assertEquals(1, fast.recordFailure(ip), "空闲衰减后应重新计数");
        assertFalse(fast.isBlocked(ip));
    }
}
