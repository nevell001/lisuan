package com.cashier.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 空闲自动登出的判定逻辑（2026-10 审计 F6：设置页的自动登出此前只写不读）。
 *
 * <p>只测纯函数：真正的监控依赖 JavaFX {@code Stage/Scene}，无头环境构造不出来，
 * 那部分用源码接线 + 人工验证（见 {@code TECH_DEBT} 的 F6 记录）。</p>
 */
@DisplayName("空闲自动登出判定")
class IdleLogoutMonitorTest {

    @Test
    @DisplayName("达到阈值即登出；关闭设置或分钟数非法时不登出")
    void idleExpiryRules() {
        // 启用 + 30 分钟：29:59 不登出，30:00 登出
        assertFalse(IdleLogoutMonitor.isIdleExpired(true, 30, 30 * 60_000L - 1));
        assertTrue(IdleLogoutMonitor.isIdleExpired(true, 30, 30 * 60_000L));
        assertTrue(IdleLogoutMonitor.isIdleExpired(true, 30, 31 * 60_000L));

        // 设置页关掉自动登出（enabled=false）或分钟数 ≤0：永不登出
        assertFalse(IdleLogoutMonitor.isIdleExpired(false, 30, 24 * 60 * 60_000L));
        assertFalse(IdleLogoutMonitor.isIdleExpired(true, 0, 24 * 60 * 60_000L));
        assertFalse(IdleLogoutMonitor.isIdleExpired(true, -5, 24 * 60 * 60_000L));

        // 刚有过活动（未登录时 supplier 返回 0，这里等价于没有空闲）
        assertFalse(IdleLogoutMonitor.isIdleExpired(true, 5, 0));
    }
}
