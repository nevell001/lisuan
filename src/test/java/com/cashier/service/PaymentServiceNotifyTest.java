package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.PaymentDAORefactored;
import com.cashier.model.PaymentOrder;
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
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 支付回调业务处理测试：验签后订单状态机、金额校验、幂等与终态保护。
 */
@DisplayName("支付回调业务处理测试")
class PaymentServiceNotifyTest extends DatabaseTestBase {

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
            stmt.execute("DELETE FROM payment_orders");
        }
    }

    private PaymentOrder createWechatOrder(BigDecimal amount) throws SQLException {
        PaymentOrder order = PaymentService.createPaymentOrder(
            "NOTIFY-T-" + System.nanoTime(), amount, PaymentOrder.PaymentChannel.WECHAT, "POS-1", "tester");
        assertNotNull(order);
        assertNotNull(order.merchantOrderNo);
        return order;
    }

    private Map<String, String> wechatNotify(PaymentOrder order, String totalAmount,
                                             String mockSignature) {
        Map<String, String> notify = new HashMap<>();
        notify.put("out_trade_no", order.merchantOrderNo);
        notify.put("trade_status", "SUCCESS");
        notify.put("total_amount", totalAmount);
        notify.put("transaction_id", "WX-TX-" + order.merchantOrderNo);
        notify.put("buyer_id", "openid-test");
        if (mockSignature != null) {
            notify.put("mock_signature", mockSignature);
        }
        return notify;
    }

    @Test
    @DisplayName("合法回调更新订单为成功")
    void validNotifyMarksOrderSuccess() throws SQLException {
        PaymentOrder order = createWechatOrder(BigDecimal.valueOf(88.50));

        boolean handled = PaymentService.handlePaymentNotify(
            PaymentOrder.PaymentChannel.WECHAT, wechatNotify(order, "88.50", CALLBACK_SECRET));

        assertTrue(handled);
        PaymentOrder reloaded = paymentDAO.findByMerchantOrderNo(order.merchantOrderNo);
        assertEquals(PaymentOrder.PaymentStatus.SUCCESS, reloaded.status);
    }

    @Test
    @DisplayName("金额不一致的回调被拒绝且状态不变")
    void amountMismatchIsRejected() throws SQLException {
        PaymentOrder order = createWechatOrder(BigDecimal.valueOf(88.50));

        boolean handled = PaymentService.handlePaymentNotify(
            PaymentOrder.PaymentChannel.WECHAT, wechatNotify(order, "99.00", CALLBACK_SECRET));

        assertFalse(handled);
        assertEquals(PaymentOrder.PaymentStatus.WAITING,
            paymentDAO.findByMerchantOrderNo(order.merchantOrderNo).status);
    }

    @Test
    @DisplayName("签名错误的回调被拒绝")
    void invalidSignatureIsRejected() throws SQLException {
        PaymentOrder order = createWechatOrder(BigDecimal.valueOf(10.00));

        boolean handled = PaymentService.handlePaymentNotify(
            PaymentOrder.PaymentChannel.WECHAT, wechatNotify(order, "10.00", "wrong-secret"));

        assertFalse(handled);
        assertEquals(PaymentOrder.PaymentStatus.WAITING,
            paymentDAO.findByMerchantOrderNo(order.merchantOrderNo).status);
    }

    @Test
    @DisplayName("重复成功回调幂等确认，不改变状态")
    void duplicateNotifyIsIdempotent() throws SQLException {
        PaymentOrder order = createWechatOrder(BigDecimal.valueOf(20.00));
        Map<String, String> notify = wechatNotify(order, "20.00", CALLBACK_SECRET);

        assertTrue(PaymentService.handlePaymentNotify(PaymentOrder.PaymentChannel.WECHAT, notify));
        assertTrue(PaymentService.handlePaymentNotify(PaymentOrder.PaymentChannel.WECHAT, notify));

        assertEquals(PaymentOrder.PaymentStatus.SUCCESS,
            paymentDAO.findByMerchantOrderNo(order.merchantOrderNo).status);
    }

    @Test
    @DisplayName("已退款订单收到迟到成功回调被拒绝，状态不回退")
    void refundedOrderRejectsLateNotify() throws SQLException {
        PaymentOrder order = createWechatOrder(BigDecimal.valueOf(30.00));
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE payment_orders SET status = 'REFUNDED' WHERE payment_id = '"
                + order.paymentId + "'");
        }

        boolean handled = PaymentService.handlePaymentNotify(
            PaymentOrder.PaymentChannel.WECHAT, wechatNotify(order, "30.00", CALLBACK_SECRET));

        assertFalse(handled);
        assertEquals(PaymentOrder.PaymentStatus.REFUNDED,
            paymentDAO.findByMerchantOrderNo(order.merchantOrderNo).status);
    }
}
