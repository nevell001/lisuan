package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.PaymentDAORefactored;
import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;
import com.cashier.service.payment.MockPaymentChannelProvider;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 支付退款金额约束测试。
 *
 * <p>回归：部分退款后 {@code paid_amount} 不减少，此前每次退款都按全额校验，
 * 同一笔支付可被累计退超过实付金额。</p>
 */
@DisplayName("支付退款累计上限测试")
class PaymentServiceRefundTest extends DatabaseTestBase {

    private static final String CALLBACK_SECRET = "test-callback-secret";

    private final PaymentDAORefactored paymentDAO = DAOFactory.getInstance().getPaymentDAO();

    @BeforeEach
    void setUp() throws SQLException {
        if (!DatabaseTestBase.isInitialized()) {
            DatabaseTestBase.initTestDatabase();
        }
        paymentDAO.createTable();
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM refund_records");
            stmt.execute("DELETE FROM payment_orders");
        }

        PaymentService.PaymentConfig config = new PaymentService.PaymentConfig();
        config.mode = "mock";
        config.mockEnabled = true;
        config.mockCallbackSecret = CALLBACK_SECRET;
        config.wechatEnabled = true;
        PaymentService.setConfig(config);
        PaymentService.registerProvider(new MockPaymentChannelProvider(
            PaymentOrder.PaymentChannel.WECHAT, CALLBACK_SECRET));
    }

    @AfterEach
    void tearDown() throws SQLException {
        PaymentService.setConfig(new PaymentService.PaymentConfig());
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM refund_records");
            stmt.execute("DELETE FROM payment_orders");
        }
    }

    private PaymentOrder paidOrder(String amount) throws SQLException {
        PaymentOrder order = PaymentService.createPaymentOrder(
            "REFUND-T-" + System.nanoTime(), new BigDecimal(amount),
            PaymentOrder.PaymentChannel.WECHAT, "POS-1");

        Map<String, String> notify = new HashMap<>();
        notify.put("out_trade_no", order.merchantOrderNo);
        notify.put("trade_status", "SUCCESS");
        notify.put("total_amount", amount);
        notify.put("transaction_id", "WX-TX-" + order.merchantOrderNo);
        notify.put("mock_signature", CALLBACK_SECRET);
        assertTrue(PaymentService.handlePaymentNotify(PaymentOrder.PaymentChannel.WECHAT, notify));

        return paymentDAO.findByMerchantOrderNo(order.merchantOrderNo);
    }

    @Test
    @DisplayName("部分退款后累计退款不得超过实付金额")
    void cumulativeRefundCannotExceedPaidAmount() throws SQLException {
        PaymentOrder order = paidOrder("100.00");

        RefundRecord first = PaymentService.applyRefund(
            order.paymentId, new BigDecimal("60.00"), "部分退款", "admin");
        assertNotNull(first);
        assertEquals(PaymentOrder.PaymentStatus.PARTIAL_REFUND,
            paymentDAO.findById(order.paymentId).status);

        // 已退 60，再退 60 会超过剩余可退 40
        IllegalArgumentException overRefund = assertThrows(IllegalArgumentException.class,
            () -> PaymentService.applyRefund(order.paymentId, new BigDecimal("60.00"), "超额退款", "admin"));
        assertTrue(overRefund.getMessage().contains("可退余额"), overRefund.getMessage());

        // 退完剩余 40 后进入终态，且不能再退
        PaymentService.applyRefund(order.paymentId, new BigDecimal("40.00"), "退完", "admin");
        assertEquals(PaymentOrder.PaymentStatus.REFUNDED,
            paymentDAO.findById(order.paymentId).status);
        assertThrows(IllegalStateException.class,
            () -> PaymentService.applyRefund(order.paymentId, new BigDecimal("1.00"), "再退", "admin"));
    }

    @Test
    @DisplayName("并发退款不会超过实付金额（行锁 + 额度预占）")
    void concurrentRefundsCannotExceedPaidAmount() throws Exception {
        PaymentOrder order = paidOrder("100.00");

        int threads = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Throwable>> results = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                final int idx = i;
                results.add(pool.submit(() -> {
                    start.await();
                    try {
                        // 每笔都申请 60：只有第一笔能拿到额度，其余必须失败
                        PaymentService.applyRefund(order.paymentId,
                            new BigDecimal("60.00"), "并发退款-" + idx, "admin");
                        return null;
                    } catch (Throwable t) {
                        return t;
                    }
                }));
            }
            start.countDown();

            int succeeded = 0;
            for (Future<Throwable> result : results) {
                if (result.get(30, TimeUnit.SECONDS) == null) {
                    succeeded++;
                }
            }
            assertEquals(1, succeeded, "100 元订单只能成功退出一笔 60 元，其余并发请求必须被拒");
        } finally {
            pool.shutdownNow();
        }

        // 已成功退款合计不得超过实付金额
        assertTrue(paymentDAO.sumSettledRefundAmount(order.paymentId)
                .compareTo(new BigDecimal("100.00")) <= 0,
            "累计成功退款不得突破实付金额");
        assertEquals(0, new BigDecimal("60.00").compareTo(
            paymentDAO.sumSettledRefundAmount(order.paymentId)));
        assertEquals(PaymentOrder.PaymentStatus.PARTIAL_REFUND,
            paymentDAO.findById(order.paymentId).status,
            "只成功退了 60，订单应停留在部分退款");
    }
}
