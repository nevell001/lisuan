package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.PaymentDAORefactored;
import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;
import com.cashier.service.payment.PaymentChannelProvider;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 退款终态对账测试（F9）。
 *
 * <p>回归的缺陷：微信退款是异步的，{@code refund()} 拿到非 SUCCESS 只落 PROCESSING，而退款请求没带
 * {@code notify_url}（微信不推送结果）。此前没有任何回查：
 * ① 预占额度永久占用——同一笔支付再也退不了；
 * ② {@code settleRefund} 把"已成功退款合计 = 0"也当成部分退款，订单状态被错标成"部分退款"。</p>
 */
@DisplayName("退款终态对账测试（F9）")
class PaymentRefundReconcileTest extends DatabaseTestBase {

    private final PaymentDAORefactored paymentDAO = DAOFactory.getInstance().getPaymentDAO();

    private FakeChannelProvider provider;

    @BeforeEach
    void setUp() throws SQLException {
        if (!DatabaseTestBase.isInitialized()) {
            DatabaseTestBase.initTestDatabase();
        }
        paymentDAO.createTable();
        cleanRefundTables();

        PaymentService.setConfig(new PaymentService.PaymentConfig());
        provider = new FakeChannelProvider();
        // 必须在 setConfig 之后注册：setConfig 会按配置重建渠道表
        PaymentService.registerProvider(provider);
    }

    @AfterEach
    void tearDown() throws SQLException {
        PaymentService.setConfig(new PaymentService.PaymentConfig());
        cleanRefundTables();
    }

    @Test
    @DisplayName("异步受理的退款必须停在处理中：不误标部分退款，且预占额度挡住重复退")
    void asyncRefundStaysProcessingAndOccupiesQuota() throws SQLException {
        PaymentOrder order = paidOrder("50.00");

        provider.refundResult = RefundRecord.RefundStatus.PROCESSING;
        RefundRecord refund = PaymentService.applyRefund(order.paymentId, new BigDecimal("50.00"), "退货", "tester");

        assertEquals(RefundRecord.RefundStatus.PROCESSING, refund.status, "渠道未回 SUCCESS 只能记处理中");
        PaymentOrder afterRefund = paymentDAO.findById(order.paymentId);
        assertEquals(PaymentOrder.PaymentStatus.SUCCESS, afterRefund.status,
            "一分钱都还没退成功时，订单状态不能被标成部分退款（此前会显示 PARTIAL_REFUND）");
        assertNotNull(paymentDAO.listRefunds("PROCESSING", 10), "处理中的退款单应可查");

        // 处理中计入预占：同一笔支付不能再退（这正是必须对账收敛的原因）
        assertThrows(IllegalArgumentException.class,
            () -> PaymentService.applyRefund(order.paymentId, new BigDecimal("50.00"), "再退", "tester"),
            "处理中的退款已预占额度，重复退款必须被拒");
    }

    @Test
    @DisplayName("对账把处理中收敛为成功：退款单落终态、订单转已退款、退款时间写入")
    void reconcileConvergesProcessingRefundToSuccess() throws SQLException {
        PaymentOrder order = paidOrder("50.00");
        RefundRecord refund = processingRefund(order, "50.00");

        provider.queryResult = RefundRecord.RefundStatus.SUCCESS;
        assertEquals(1, PaymentService.reconcileRefunds(10), "应有一笔退款被收敛");

        RefundRecord settled = refundById(refund.refundId);
        assertEquals(RefundRecord.RefundStatus.SUCCESS, settled.status);
        assertNotNull(settled.refundTime, "成功退款必须写 refund_time");
        assertEquals("FAKE-RFD-" + refund.merchantRefundNo, settled.channelRefundNo, "渠道退款号应回填");
        assertEquals(PaymentOrder.PaymentStatus.REFUNDED, paymentDAO.findById(order.paymentId).status,
            "全额退款成功后订单转已退款");
    }

    @Test
    @DisplayName("对账判定失败：订单状态保持成功（不改部分退款），预占额度释放后可重退")
    void reconcileReleasesQuotaWhenChannelFailsRefund() throws SQLException {
        PaymentOrder order = paidOrder("50.00");
        RefundRecord refund = processingRefund(order, "50.00");

        provider.queryResult = RefundRecord.RefundStatus.FAILED;
        assertEquals(1, PaymentService.reconcileRefunds(10));

        assertEquals(RefundRecord.RefundStatus.FAILED, refundById(refund.refundId).status);
        assertEquals(PaymentOrder.PaymentStatus.SUCCESS, paymentDAO.findById(order.paymentId).status,
            "退款失败不得把订单标成部分退款");

        // 失败退款不计入已退金额：额度释放，可以重新发起
        provider.refundResult = RefundRecord.RefundStatus.PROCESSING;
        RefundRecord retry = PaymentService.applyRefund(order.paymentId, new BigDecimal("50.00"), "重试", "tester");
        assertNotNull(retry.refundId);
    }

    @Test
    @DisplayName("渠道仍在处理时不改本地状态（下一轮再查）")
    void reconcileLeavesProcessingRefundUntouched() throws SQLException {
        PaymentOrder order = paidOrder("50.00");
        RefundRecord refund = processingRefund(order, "50.00");

        provider.queryResult = RefundRecord.RefundStatus.PROCESSING;
        assertEquals(0, PaymentService.reconcileRefunds(10), "仍在处理中的不算收敛");
        assertEquals(RefundRecord.RefundStatus.PROCESSING, refundById(refund.refundId).status);
    }

    @Test
    @DisplayName("已落终态的退款不会被对账改回去（渠道后回 FAILED 也不行）")
    void reconcileNeverRegressesFinalRefund() throws SQLException {
        PaymentOrder order = paidOrder("50.00");
        RefundRecord refund = processingRefund(order, "50.00");

        provider.queryResult = RefundRecord.RefundStatus.SUCCESS;
        assertEquals(1, PaymentService.reconcileRefunds(10));

        RefundRecord settled = refundById(refund.refundId);
        provider.queryResult = RefundRecord.RefundStatus.FAILED;
        assertFalse(PaymentService.reconcileRefund(settled, provider), "已终态的退款不再参与对账");
        assertEquals(RefundRecord.RefundStatus.SUCCESS, refundById(refund.refundId).status,
            "状态不得回退");
        assertEquals(PaymentOrder.PaymentStatus.REFUNDED, paymentDAO.findById(order.paymentId).status);
    }

    @Test
    @DisplayName("超过最长跟踪时长仍未终态的退款不再自动重试，转人工核对")
    void staleUnsettledRefundIsLeftToManualReview() throws SQLException {
        PaymentOrder order = paidOrder("50.00");
        RefundRecord refund = processingRefund(order, "50.00");
        // 把这笔退款的创建时间挪到 2 小时前（默认最长跟踪 1 小时，见下面的配置）
        try (Connection conn = getTestConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE refund_records SET create_time = DATEADD('HOUR', -2, CURRENT_TIMESTAMP) "
                + "WHERE refund_id = '" + refund.refundId + "'");
        }
        PaymentService.PaymentConfig config = new PaymentService.PaymentConfig();
        config.refundMaxTrackHours = 1;
        PaymentService.setConfig(config);
        PaymentService.registerProvider(provider);

        provider.queryResult = RefundRecord.RefundStatus.SUCCESS;
        assertEquals(0, PaymentService.reconcileRefunds(10), "超期退款不再自动重试");
        assertEquals(RefundRecord.RefundStatus.PROCESSING, refundById(refund.refundId).status);
        assertEquals(1, paymentDAO.countStaleUnsettledRefunds(
            new java.util.Date(System.currentTimeMillis() - 3600_000L)), "超期未终态要能被统计出来（供告警/人工核对）");
    }

    @Test
    @DisplayName("对账服务重复启动不叠调度器，停止后状态复位（TD-033 同类守卫）")
    void reconcileServiceCannotBeDoubleStarted() {
        PaymentRefundReconcileService service = PaymentRefundReconcileService.getInstance();
        service.start();
        try {
            assertTrue(service.isRunning());
            service.start();
            assertTrue(service.isRunning());
        } finally {
            service.stop();
        }
        assertFalse(service.isRunning());
    }

    // ===== 辅助 =====

    private void cleanRefundTables() throws SQLException {
        try (Connection conn = getTestConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM refund_records");
            stmt.execute("DELETE FROM payment_orders");
        }
    }

    private PaymentOrder paidOrder(String amount) throws SQLException {
        PaymentOrder order = PaymentService.createPaymentOrder(
            "F9-T-" + System.nanoTime(), new BigDecimal(amount),
            PaymentOrder.PaymentChannel.WECHAT, "POS-1", "tester");

        Map<String, String> notify = new HashMap<>();
        notify.put("out_trade_no", order.merchantOrderNo);
        notify.put("trade_status", "SUCCESS");
        notify.put("total_amount", amount);
        notify.put("transaction_id", "WX-TX-" + order.merchantOrderNo);
        assertTrue(PaymentService.handlePaymentNotify(PaymentOrder.PaymentChannel.WECHAT, notify));

        PaymentOrder paid = paymentDAO.findByMerchantOrderNo(order.merchantOrderNo);
        assertEquals(PaymentOrder.PaymentStatus.SUCCESS, paid.status);
        return paid;
    }

    /** 走真实退款入口制造一笔"渠道已受理、结果未知"的退款（微信异步退款的形态）。 */
    private RefundRecord processingRefund(PaymentOrder order, String amount) throws SQLException {
        provider.refundResult = RefundRecord.RefundStatus.PROCESSING;
        RefundRecord refund = PaymentService.applyRefund(order.paymentId, new BigDecimal(amount), "退货", "tester");
        assertEquals(RefundRecord.RefundStatus.PROCESSING, refund.status);
        return refund;
    }

    private RefundRecord refundById(String refundId) throws SQLException {
        List<RefundRecord> candidates = paymentDAO.listRefunds(null, 50);
        for (RefundRecord refund : candidates) {
            if (refundId.equals(refund.refundId)) {
                return refund;
            }
        }
        // 已落终态的退款不在"未终态"列表里，按状态逐个兜一遍
        for (String status : List.of("SUCCESS", "FAILED", "CLOSED", "APPLYING", "PROCESSING")) {
            for (RefundRecord refund : paymentDAO.listRefunds(status, 50)) {
                if (refundId.equals(refund.refundId)) {
                    return refund;
                }
            }
        }
        throw new IllegalStateException("找不到退款单: " + refundId);
    }

    /**
     * 假渠道：退款一律"受理但结果未知"（模拟微信异步退款），回查结果由测试控制。
     */
    private static final class FakeChannelProvider implements PaymentChannelProvider {
        private RefundRecord.RefundStatus refundResult = RefundRecord.RefundStatus.PROCESSING;
        private RefundRecord.RefundStatus queryResult = RefundRecord.RefundStatus.PROCESSING;

        @Override public PaymentOrder.PaymentChannel channel() { return PaymentOrder.PaymentChannel.WECHAT; }
        @Override public boolean isAvailable() { return true; }
        @Override public String unavailableReason() { return null; }
        @Override public void createOrder(PaymentOrder order) { }
        @Override public PaymentOrder.PaymentStatus queryStatus(PaymentOrder order) {
            return PaymentOrder.PaymentStatus.SUCCESS;
        }
        @Override public boolean verifyNotification(Map<String, String> notification) { return true; }

        @Override
        public void refund(PaymentOrder order, RefundRecord refund) {
            refund.status = refundResult;
            refund.channelRefundNo = refundResult == RefundRecord.RefundStatus.SUCCESS
                ? "FAKE-RFD-" + refund.merchantRefundNo : null;
        }

        @Override
        public RefundRecord.RefundStatus queryRefund(PaymentOrder order, RefundRecord refund) {
            if (queryResult == RefundRecord.RefundStatus.SUCCESS) {
                refund.channelRefundNo = "FAKE-RFD-" + refund.merchantRefundNo;
            }
            return queryResult;
        }
    }
}
