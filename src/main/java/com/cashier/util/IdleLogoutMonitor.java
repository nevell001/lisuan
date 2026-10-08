package com.cashier.util;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.IntSupplier;

/**
 * 空闲自动登出（对应设置页的 {@code autoLogout} / {@code autoLogoutMinutes}）。
 *
 * <p>此前这两个设置项<b>只写不读</b>，"空闲自动登出"功能实际不存在（2026-10 审计 F6）：
 * 无人值守的收银终端可以无限期停在已登录界面。这里在窗口的 {@link Scene} 上装一个
 * 事件处理器记录用户活动（鼠标/键盘/触摸都算），再用守护定时器周期检查：
 * 超过设定分钟数没有任何活动就回调登出。</p>
 *
 * <p>为什么不直接绑当前场景：应用会在登录页 / 主界面 / 触屏收银台之间换场景，
 * 因此监听 {@code Stage.sceneProperty()}，换一次场景就把活动监听装到新场景上。</p>
 *
 * <p>定时器线程只做"读数 + 判空"，回调一律 {@link Platform#runLater} 回 FX 线程。</p>
 */
public final class IdleLogoutMonitor {

    private static final Logger logger = LoggerFactoryUtil.getLogger(IdleLogoutMonitor.class);

    /** 检查间隔：比最短可配置空闲时间（5 分钟）小得多，登出延迟最多 30 秒 */
    private static final long CHECK_INTERVAL_MILLIS = 30_000L;

    private final Timer timer = new Timer("idle-logout-monitor", true);
    private final IntSupplier idleMinutesSupplier;
    private final Runnable onIdle;

    private volatile long lastActivityMillis = System.currentTimeMillis();
    private volatile boolean stopped;

    private IdleLogoutMonitor(IntSupplier idleMinutesSupplier, Runnable onIdle) {
        this.idleMinutesSupplier = idleMinutesSupplier;
        this.onIdle = onIdle;
    }

    /**
     * 启动监控。
     *
     * @param stage              主窗口（跟随场景切换自动重绑监听）
     * @param idleMinutesSupplier 每次检查时读取的空闲分钟数；返回 ≤0 表示当前不需要自动登出
     *                            （例如未登录、或设置页关闭了自动登出）
     * @param onIdle             判定为空闲超时后的回调（会在 FX 线程执行）
     */
    public static IdleLogoutMonitor start(Stage stage, IntSupplier idleMinutesSupplier, Runnable onIdle) {
        IdleLogoutMonitor monitor = new IdleLogoutMonitor(idleMinutesSupplier, onIdle);
        monitor.bindActivityListener(stage.getScene());
        stage.sceneProperty().addListener((obs, oldScene, newScene) -> monitor.bindActivityListener(newScene));
        monitor.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    monitor.checkIdle();
                } catch (Exception e) {
                    // 定时任务体一旦抛异常会永久取消后续执行，必须自己兜住
                    logger.warn("空闲自动登出检查失败", e);
                }
            }
        }, CHECK_INTERVAL_MILLIS, CHECK_INTERVAL_MILLIS);
        logger.info("空闲自动登出监控已启动");
        return monitor;
    }

    private void bindActivityListener(Scene scene) {
        if (scene == null) {
            return;
        }
        // 用 handler（不是 filter）不消费事件，交给控件继续处理
        scene.addEventHandler(Event.ANY, event -> lastActivityMillis = System.currentTimeMillis());
    }

    private void checkIdle() {
        if (stopped) {
            return;
        }
        int minutes = idleMinutesSupplier.getAsInt();
        if (minutes <= 0) {
            // 未登录 / 已关闭自动登出：把活动时间顶住，避免刚打开设置时被"旧时间戳"立刻踢出去
            lastActivityMillis = System.currentTimeMillis();
            return;
        }
        long idleMillis = System.currentTimeMillis() - lastActivityMillis;
        if (!isIdleExpired(true, minutes, idleMillis)) {
            return;
        }
        // 先顶住时间戳，防止回调执行期间重复触发
        lastActivityMillis = System.currentTimeMillis();
        logger.info("空闲 {} 分钟无操作，自动登出", minutes);
        Platform.runLater(onIdle);
    }

    /**
     * 纯函数判定，便于无头测试：启用、分钟数合法且空闲时间已达到阈值时才需要登出。
     */
    static boolean isIdleExpired(boolean enabled, int minutes, long idleMillis) {
        return enabled && minutes > 0 && idleMillis >= minutes * 60_000L;
    }

    /** 停止监控（退出登录/关闭应用时调用）；可重复调用。 */
    public void stop() {
        stopped = true;
        timer.cancel();
        logger.info("空闲自动登出监控已停止");
    }
}
