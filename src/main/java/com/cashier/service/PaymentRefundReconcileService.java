package com.cashier.service;

import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 退款对账调度（F9）：定期把停在 {@code PROCESSING} 的退款回查渠道并收敛到终态。
 *
 * <p>为什么需要：微信退款是异步的（非 SUCCESS 只能先记处理中），而退款请求没带
 * {@code notify_url}，微信不会推送退款结果。没有回查时这条记录永远停在处理中——
 * 预占额度永久占用（同一笔支付再也退不了）、订单状态还可能被错标成"部分退款"。
 * 本类只负责节拍与生命周期，业务逻辑在 {@link PaymentService#reconcileRefunds(int)}。</p>
 *
 * <p>线程纪律（TD-033 / TD-035 教训）：① {@code isRunning} 守卫，重复 {@code start()} 不得叠出第二个
 * 调度器（{@code BackupService.start()} 曾缺这个守卫）；② 线程必须是 daemon，否则退出应用时被吊住；
 * ③ 任务体必须兜住所有异常——按 {@code ScheduledExecutorService} 语义，任务抛一次异常就**永久取消**
 * 后续调度。</p>
 */
public final class PaymentRefundReconcileService {

    private static final Logger logger = LoggerFactoryUtil.getLogger(PaymentRefundReconcileService.class);

    /** 每批处理的退款笔数上限，避免一次对账把渠道打满 */
    private static final int BATCH_LIMIT = 50;

    private static PaymentRefundReconcileService instance;

    private volatile boolean isRunning = false;
    private ScheduledExecutorService scheduler;

    private PaymentRefundReconcileService() {
    }

    public static synchronized PaymentRefundReconcileService getInstance() {
        if (instance == null) {
            instance = new PaymentRefundReconcileService();
        }
        return instance;
    }

    /**
     * 启动退款对账（登录后调用）。未启用或已在运行时直接返回，不重复起调度器。
     */
    public void start() {
        PaymentService.PaymentConfig config = PaymentService.getConfig();
        if (!config.refundReconcileEnabled) {
            logger.info("退款对账未启用（refund.reconcile.enabled=false），停在处理中的退款只能人工核对");
            return;
        }
        if (isRunning) {
            logger.warn("退款对账服务已在运行中");
            return;
        }
        if (scheduler == null || scheduler.isShutdown() || scheduler.isTerminated()) {
            scheduler = createScheduler();
        }

        isRunning = true;
        long intervalSeconds = Math.max(10, config.refundReconcileSeconds);
        scheduler.scheduleWithFixedDelay(() -> {
            if (!isRunning) {
                return;
            }
            try {
                int reconciled = PaymentService.reconcileRefunds(BATCH_LIMIT);
                if (reconciled > 0) {
                    logger.info("退款对账完成，本次收敛 {} 笔", reconciled);
                }
            } catch (Throwable t) {
                // 兜住一切：任务抛异常会让后续调度被永久取消（TD-035）
                logger.error("退款对账任务异常", t);
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);

        logger.info("退款对账服务已启动，间隔 {} 秒", intervalSeconds);
    }

    /**
     * 停止退款对账（登出/退出时调用）。
     */
    public void stop() {
        if (!isRunning) {
            logger.debug("退款对账服务未在运行");
            return;
        }
        isRunning = false;
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        logger.info("退款对账服务已停止");
    }

    public boolean isRunning() {
        return isRunning;
    }

    /** 立即对账一次（不依赖调度器；给运维入口/测试用）。 */
    public int reconcileNow() {
        return PaymentService.reconcileRefunds(BATCH_LIMIT);
    }

    private ScheduledExecutorService createScheduler() {
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger sequence = new AtomicInteger();

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "refund-reconcile-" + sequence.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };
        return Executors.newSingleThreadScheduledExecutor(factory);
    }
}
