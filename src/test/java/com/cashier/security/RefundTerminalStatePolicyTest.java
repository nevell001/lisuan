package com.cashier.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 退款终态策略门禁（F9）。
 *
 * <p>实测缺陷：微信退款是异步的，{@code refund()} 拿到非 SUCCESS 只落 {@code PROCESSING}，而退款请求
 * 没带 {@code notify_url}（微信不推送结果）。此前既没有回查渠道的能力，也没有对账任务，于是这笔记录
 * 永远停在处理中——预占额度永久占用（同一笔支付再也退不了），且 {@code settleRefund} 会把
 * "已成功退款合计 = 0" 当成部分退款，订单状态被错标。</p>
 *
 * <p>本门禁钉住四条不变量（逐条都对应一次真实的错误输出）：</p>
 * <ol>
 *   <li>渠道接口必须能回查退款终态，且四个实现都不能漏；</li>
 *   <li>对账的状态迁移必须**带 from 状态条件**（裸 UPDATE 会被并发回调整退回退，导致重复结算）；</li>
 *   <li>对账任务必须有 isRunning 守卫、daemon 线程、兜住所有异常（否则一次异常永久取消调度）；</li>
 *   <li>没有任何成功退款时不得把订单标成部分退款；且对账服务必须在登录后真正被启动/停止。</li>
 * </ol>
 */
@DisplayName("退款终态策略门禁（F9）")
class RefundTerminalStatePolicyTest {

    private static final String SERVICE = "src/main/java/com/cashier/service/PaymentService.java";
    private static final String RECONCILE_SERVICE =
        "src/main/java/com/cashier/service/PaymentRefundReconcileService.java";
    private static final String DAO = "src/main/java/com/cashier/dao/PaymentDAORefactored.java";
    private static final String APP = "src/main/java/com/cashier/CashierSystemFXApplication.java";

    @Test
    @DisplayName("每个渠道适配器都要能回查退款终态（漏了就会永远停在处理中）")
    void everyProviderCanQueryRefundStatus() throws Exception {
        String contract = read("src/main/java/com/cashier/service/payment/PaymentChannelProvider.java");
        assertTrue(contract.contains("RefundRecord.RefundStatus queryRefund("),
            "渠道接口必须提供退款回查能力，否则异步退款无法收敛到终态");

        for (String provider : new String[]{
            "WechatNativePaymentProvider", "AlipayPrecreatePaymentProvider",
            "MockPaymentChannelProvider", "UnavailablePaymentChannelProvider"}) {
            String source = read("src/main/java/com/cashier/service/payment/" + provider + ".java");
            assertTrue(source.contains("queryRefund("),
                provider + " 必须实现 queryRefund（可用渠道回查渠道，不可用渠道抛不可用）");
        }
    }

    @Test
    @DisplayName("退款对账的状态迁移必须带 from 状态条件，不能被并发调整回退")
    void reconcileUsesConditionalStatusUpdate() throws Exception {
        String dao = read(DAO);
        int method = dao.indexOf("updateRefundStatusIfNotFinalWithConnection");
        assertTrue(method > 0, "资金状态迁移需要独立的带条件更新方法");
        String body = dao.substring(method, Math.min(dao.length(), method + 700));
        assertTrue(body.contains("AND status = ?"),
            "对账更新必须带 from 状态条件：裸 UPDATE 会把已落终态的退款改回处理中（重复结算）");

        String service = read(SERVICE);
        int reconcile = service.indexOf("static boolean reconcileRefund(");
        assertTrue(reconcile > 0, "对账单笔收敛逻辑应可独立测试");
        String reconcileBody = service.substring(reconcile, Math.min(service.length(), reconcile + 2500));
        assertTrue(reconcileBody.contains("updateRefundStatusIfNotFinalWithConnection"),
            "对账必须走带条件的迁移方法，不得用裸 updateRefundStatus");
        assertTrue(!reconcileBody.contains("updateRefundStatusWithConnection"),
            "对账路径不得使用无条件的状态写入");
    }

    @Test
    @DisplayName("对账任务：isRunning 守卫 + daemon 线程 + 兜住异常（一次异常不得永久取消调度）")
    void reconcileTaskIsSafeToSchedule() throws Exception {
        String source = read(RECONCILE_SERVICE);
        assertTrue(source.contains("if (isRunning)"), "重复 start() 必须被守卫拦住（TD-033 同类缺陷）");
        assertTrue(source.contains("setDaemon(true)"), "调度线程必须是 daemon，否则退出应用时被吊住");
        assertTrue(source.contains("catch (Throwable"),
            "任务体必须兜住所有异常：ScheduledExecutorService 的任务抛一次异常就会被永久取消");
        assertTrue(source.contains("scheduleWithFixedDelay"),
            "对账应按固定延迟调度（固定频率在慢批次下会堆叠执行）");
    }

    @Test
    @DisplayName("没有任何成功退款时不得标成部分退款；对账服务必须在登录后启动/停止")
    void noSuccessfulRefundMustNotFlipOrderToPartialRefund() throws Exception {
        String service = read(SERVICE);
        int settle = service.indexOf("private static void settleRefund(");
        assertTrue(settle > 0);
        String body = service.substring(settle, Math.min(service.length(), settle + 1400));
        assertTrue(body.contains("settledAmount.compareTo(BigDecimal.ZERO) == 0"),
            "已成功退款合计为 0 时必须直接返回：渠道只是受理（PROCESSING）不代表部分退款");
        assertTrue(body.indexOf("settledAmount.compareTo(BigDecimal.ZERO) == 0")
                < body.indexOf("PaymentOrder.PaymentStatus.PARTIAL_REFUND"),
            "零成功退款的短路必须出现在 PARTIAL_REFUND 判定之前");

        String app = read(APP);
        assertTrue(app.contains("PaymentRefundReconcileService.getInstance().start()"),
            "登录后必须启动对账，否则 PROCESSING 永远不会收敛");
        assertTrue(app.contains("PaymentRefundReconcileService.getInstance().stop()"),
            "登出/退出必须停止对账，避免调度器泄漏");
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
