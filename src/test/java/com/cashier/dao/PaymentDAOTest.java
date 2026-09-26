package com.cashier.dao;

import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;
import com.cashier.util.DatabaseManager;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("支付订单数据访问对象测试")
class PaymentDAOTest extends DatabaseTestBase {

    private final PaymentDAORefactored paymentDAO = DAOFactory.getInstance().getPaymentDAO();

    @BeforeEach
    void setUpPaymentTables() throws SQLException {
        paymentDAO.createTable();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM refund_records");
            stmt.execute("DELETE FROM payment_orders");
        }
    }

    @Test
    @DisplayName("查询待支付订单时按创建时间倒序并限制数量")
    void testFindWaitingOrdersUsesLimitAndNewestFirst() throws SQLException {
        paymentDAO.insert(createOrder("PAY-OLD", "ORDER-OLD", 1_000L, PaymentOrder.PaymentStatus.CREATED));
        paymentDAO.insert(createOrder("PAY-MIDDLE", "ORDER-MIDDLE", 2_000L, PaymentOrder.PaymentStatus.WAITING));
        paymentDAO.insert(createOrder("PAY-NEW", "ORDER-NEW", 3_000L, PaymentOrder.PaymentStatus.CREATED));
        paymentDAO.insert(createOrder("PAY-SUCCESS", "ORDER-SUCCESS", 4_000L, PaymentOrder.PaymentStatus.SUCCESS));

        var orders = paymentDAO.findWaitingOrders(2);

        assertEquals(2, orders.size());
        assertEquals("PAY-NEW", orders.get(0).paymentId);
        assertEquals("PAY-MIDDLE", orders.get(1).paymentId);
    }

    @Test
    @DisplayName("同一毫秒的多笔退款不会撞 refund_id / merchant_refund_no")
    void refundIdsAreUniqueWithinSameMillisecond() throws SQLException {
        // 回归：refund_id 曾只用毫秒时间戳生成，连续两笔退款必然撞主键
        Set<String> refundIds = new HashSet<>();
        Set<String> merchantRefundNos = new HashSet<>();
        List<RefundRecord> refunds = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) {
            RefundRecord record = RefundRecord.create("PAY-BURST", BigDecimal.TEN, "并发退款-" + i, "op");
            assertTrue(paymentDAO.insertRefund(record));
            refunds.add(record);
        }
        for (RefundRecord record : refunds) {
            refundIds.add(record.refundId);
            merchantRefundNos.add(record.merchantRefundNo);
        }
        assertEquals(50, refundIds.size(), "refund_id 必须唯一");
        assertEquals(50, merchantRefundNos.size(), "merchant_refund_no 必须唯一");
    }

    @Test
    @DisplayName("payment_id 缺失时由 DAO 兜底生成加固单号（不是裸毫秒主键）")
    void insertGeneratesHardenedPaymentIdWhenMissing() throws SQLException {
        // 回归：payment_id 是主键，兜底曾用 "PAY" + System.currentTimeMillis()，
        // 同毫秒两笔必然主键冲突；加固后长度与形状都可区分。
        PaymentOrder order = createOrder(null, "ORDER-AUTO", 5_000L, PaymentOrder.PaymentStatus.CREATED);

        assertTrue(paymentDAO.insert(order), "缺少 payment_id 时应能兜底生成并插入");
        assertTrue(order.paymentId.matches("^PAY\\d{13}\\d{4}[0-9a-f]{6}$"),
            "兜底生成的支付单号必须是加固格式（毫秒+序号+随机段），实际: " + order.paymentId);
        assertEquals(order.paymentId, paymentDAO.findById(order.paymentId).paymentId, "落库的应是兜底生成的那个单号");
    }

    private PaymentOrder createOrder(
            String paymentId,
            String merchantOrderNo,
            long createTime,
            PaymentOrder.PaymentStatus status
    ) {
        PaymentOrder order = new PaymentOrder();
        order.paymentId = paymentId;
        order.transactionId = "TX-" + paymentId;
        order.merchantOrderNo = merchantOrderNo;
        order.paymentType = PaymentOrder.PaymentType.QRCODE_PAY;
        order.channel = PaymentOrder.PaymentChannel.WECHAT;
        order.amount = BigDecimal.TEN;
        order.status = status;
        order.createTime = Date.from(Instant.ofEpochMilli(createTime));
        order.expireTime = Date.from(Instant.now().plusSeconds(3600));
        order.terminalId = "POS-1";
        order.operator = "admin";
        return order;
    }
}
