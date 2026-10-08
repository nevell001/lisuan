package com.cashier.dao;

import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 支付订单数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class PaymentDAORefactored extends BaseDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PaymentDAORefactored.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /** 进程内序号：refund_id 仅用毫秒时间戳会在同毫秒两笔退款时撞主键 */
    private static final AtomicLong REFUND_ID_SEQ = new AtomicLong();

    public void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS payment_orders (
                payment_id VARCHAR(50) PRIMARY KEY,
                transaction_id VARCHAR(50),
                merchant_order_no VARCHAR(50) UNIQUE,
                payment_type VARCHAR(20),
                channel VARCHAR(20),
                amount DECIMAL(10,2),
                status VARCHAR(20),
                qr_code_url VARCHAR(500),
                qr_code_content VARCHAR(500),
                paid_amount DECIMAL(10,2),
                discount_amount DECIMAL(10,2),
                create_time DATETIME,
                pay_time DATETIME,
                expire_time DATETIME,
                channel_transaction_id VARCHAR(100),
                channel_user_id VARCHAR(100),
                remark VARCHAR(200),
                terminal_id VARCHAR(50),
                operator VARCHAR(50),
                notify_time DATETIME,
                notify_content TEXT
            )
            """;
        String refundSql = """
            CREATE TABLE IF NOT EXISTS refund_records (
                refund_id VARCHAR(50) PRIMARY KEY,
                payment_id VARCHAR(50),
                transaction_id VARCHAR(50),
                merchant_refund_no VARCHAR(50) UNIQUE,
                channel_refund_no VARCHAR(100),
                refund_amount DECIMAL(10,2),
                original_amount DECIMAL(10,2),
                reason VARCHAR(200),
                status VARCHAR(20),
                channel VARCHAR(20),
                create_time DATETIME,
                refund_time DATETIME,
                operator VARCHAR(50)
            )
            """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute(refundSql);
            logger.info("支付订单表创建成功");
        }
    }

    public boolean insert(PaymentOrder order) throws SQLException {
        String sql = """
            INSERT INTO payment_orders (
                payment_id, transaction_id, merchant_order_no, payment_type, channel,
                amount, status, qr_code_url, qr_code_content, paid_amount, discount_amount,
                create_time, pay_time, expire_time, channel_transaction_id, channel_user_id,
                remark, terminal_id, operator, notify_time, notify_content
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        if (order.paymentId == null) {
            // 主键不能只用毫秒时间戳：同毫秒两笔会撞主键、支付单丢失
            order.paymentId = PaymentOrder.generatePaymentId();
        }
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, order.paymentId);
            pstmt.setString(2, order.transactionId);
            pstmt.setString(3, order.merchantOrderNo);
            pstmt.setString(4, order.paymentType != null ? order.paymentType.name() : "QRCODE_PAY");
            pstmt.setString(5, order.channel != null ? order.channel.name() : "WECHAT");
            pstmt.setBigDecimal(6, order.amount);
            pstmt.setString(7, order.status != null ? order.status.name() : "CREATED");
            pstmt.setString(8, order.qrCodeUrl);
            pstmt.setString(9, order.qrCodeContent);
            pstmt.setBigDecimal(10, order.paidAmount);
            pstmt.setBigDecimal(11, order.discountAmount);
            pstmt.setTimestamp(12, order.createTime != null ? new Timestamp(order.createTime.getTime()) : null);
            pstmt.setTimestamp(13, order.payTime != null ? new Timestamp(order.payTime.getTime()) : null);
            pstmt.setTimestamp(14, order.expireTime != null ? new Timestamp(order.expireTime.getTime()) : null);
            pstmt.setString(15, order.channelTransactionId);
            pstmt.setString(16, order.channelUserId);
            pstmt.setString(17, order.remark);
            pstmt.setString(18, order.terminalId);
            pstmt.setString(19, order.operator);
            pstmt.setTimestamp(20, order.notifyTime != null ? new Timestamp(order.notifyTime.getTime()) : null);
            pstmt.setString(21, order.notifyContent);
            return pstmt.executeUpdate() > 0;
        }
    }

    public PaymentOrder findById(String paymentId) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM payment_orders WHERE payment_id = ?")) {
            pstmt.setString(1, paymentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapResultSetToPaymentOrder(rs) : null;
            }
        }
    }

    /**
     * 在事务内按主键锁定支付单（{@code SELECT ... FOR UPDATE}）。
     *
     * <p>并发退款需要先串行化到同一行：拿到行锁后再统计已退金额并预占额度，
     * 第二个事务必须等第一个提交后才能读取，避免两边读到相同的"已退 0 元"。</p>
     */
    public PaymentOrder findByIdForUpdate(Connection conn, String paymentId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
            "SELECT * FROM payment_orders WHERE payment_id = ? FOR UPDATE")) {
            pstmt.setString(1, paymentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapResultSetToPaymentOrder(rs) : null;
            }
        }
    }

    public PaymentOrder findByMerchantOrderNo(String merchantOrderNo) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM payment_orders WHERE merchant_order_no = ?")) {
            pstmt.setString(1, merchantOrderNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapResultSetToPaymentOrder(rs) : null;
            }
        }
    }

    public List<PaymentOrder> findByTransactionId(String transactionId) throws SQLException {
        String sql = "SELECT * FROM payment_orders WHERE transaction_id = ? ORDER BY create_time DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<PaymentOrder> orders = new ArrayList<>();
                while (rs.next()) {
                    orders.add(mapResultSetToPaymentOrder(rs));
                }
                return orders;
            }
        }
    }

    public List<PaymentOrder> findWaitingOrders() throws SQLException {
        return findWaitingOrders(100);
    }

    public List<PaymentOrder> findWaitingOrders(int limit) throws SQLException {
        int safeLimit = limit > 0 ? limit : 100;
        String sql = "SELECT * FROM payment_orders WHERE status IN ('CREATED', 'WAITING') " +
            "AND expire_time > NOW() ORDER BY create_time DESC LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, safeLimit);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<PaymentOrder> orders = new ArrayList<>();
                while (rs.next()) {
                    orders.add(mapResultSetToPaymentOrder(rs));
                }
                return orders;
            }
        }
    }

    public boolean updateStatus(String paymentId, PaymentOrder.PaymentStatus status) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE payment_orders SET status = ? WHERE payment_id = ?")) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, paymentId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updateStatusWithConnection(Connection conn, String paymentId, PaymentOrder.PaymentStatus status)
            throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
            "UPDATE payment_orders SET status = ? WHERE payment_id = ?")) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, paymentId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updateStatusIfPending(String paymentId, PaymentOrder.PaymentStatus status) throws SQLException {
        String sql = "UPDATE payment_orders SET status = ? WHERE payment_id = ? " +
            "AND status IN ('CREATED', 'WAITING', 'PAYING')";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, paymentId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updatePaymentSuccess(String paymentId, String channelTransactionId,
                                        String channelUserId, BigDecimal paidAmount,
                                        BigDecimal discountAmount) throws SQLException {
        try (Connection conn = getConnection()) {
            return updatePaymentSuccessWithConnection(conn, paymentId, channelTransactionId,
                channelUserId, paidAmount, discountAmount);
        }
    }

    public boolean updatePaymentSuccessWithConnection(Connection conn, String paymentId, String channelTransactionId,
                                                      String channelUserId, BigDecimal paidAmount,
                                                      BigDecimal discountAmount) throws SQLException {
        String sql = """
            UPDATE payment_orders SET
                status = 'SUCCESS',
                pay_time = ?,
                channel_transaction_id = ?,
                channel_user_id = ?,
                paid_amount = ?,
                discount_amount = ?
            WHERE payment_id = ?
            """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(2, channelTransactionId);
            pstmt.setString(3, channelUserId);
            pstmt.setBigDecimal(4, paidAmount);
            pstmt.setBigDecimal(5, discountAmount);
            pstmt.setString(6, paymentId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updateNotifyInfo(String paymentId, String notifyContent) throws SQLException {
        try (Connection conn = getConnection()) {
            return updateNotifyInfoWithConnection(conn, paymentId, notifyContent);
        }
    }

    public boolean updateNotifyInfoWithConnection(Connection conn, String paymentId, String notifyContent) throws SQLException {
        String sql = "UPDATE payment_orders SET notify_time = ?, notify_content = ? WHERE payment_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(2, notifyContent);
            pstmt.setString(3, paymentId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public int closeExpiredOrders() throws SQLException {
        String sql = "UPDATE payment_orders SET status = 'CLOSED' WHERE status IN ('CREATED', 'WAITING') AND expire_time < NOW()";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    public Map<String, Object> getDailyStats(Date date) throws SQLException {
        String sql = """
            SELECT
                COUNT(*) as total_count,
                SUM(amount) as total_amount,
                SUM(paid_amount) as paid_amount,
                COUNT(CASE WHEN status = 'SUCCESS' THEN 1 END) as success_count,
                COUNT(CASE WHEN channel = 'WECHAT' THEN 1 END) as wechat_count,
                COUNT(CASE WHEN channel = 'ALIPAY' THEN 1 END) as alipay_count
            FROM payment_orders
            WHERE DATE(create_time) = DATE(?)
            """;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, new Timestamp(date.getTime()));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("totalCount", rs.getInt("total_count"));
                    stats.put("totalAmount", rs.getBigDecimal("total_amount"));
                    stats.put("paidAmount", rs.getBigDecimal("paid_amount"));
                    stats.put("successCount", rs.getInt("success_count"));
                    stats.put("wechatCount", rs.getInt("wechat_count"));
                    stats.put("alipayCount", rs.getInt("alipay_count"));
                    return stats;
                }
            }
        }
        return new HashMap<>();
    }

    public boolean insertRefund(RefundRecord record) throws SQLException {
        try (Connection conn = getConnection()) {
            return insertRefundWithConnection(conn, record);
        }
    }

    public boolean insertRefundWithConnection(Connection conn, RefundRecord record) throws SQLException {
        String sql = """
            INSERT INTO refund_records (
                refund_id, payment_id, transaction_id, merchant_refund_no, channel_refund_no,
                refund_amount, original_amount, reason, status, channel,
                create_time, refund_time, operator
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        if (record.refundId == null) {
            record.refundId = "RFD" + System.currentTimeMillis()
                + String.format("%04d", REFUND_ID_SEQ.incrementAndGet() % 10000)
                + String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        }
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, record.refundId);
            pstmt.setString(2, record.paymentId);
            pstmt.setString(3, record.transactionId);
            pstmt.setString(4, record.merchantRefundNo);
            pstmt.setString(5, record.channelRefundNo);
            pstmt.setBigDecimal(6, record.refundAmount);
            pstmt.setBigDecimal(7, record.originalAmount);
            pstmt.setString(8, record.reason);
            pstmt.setString(9, record.status != null ? record.status.name() : "APPLYING");
            pstmt.setString(10, record.channel);
            pstmt.setTimestamp(11, record.createTime != null ? new Timestamp(record.createTime.getTime()) : null);
            pstmt.setTimestamp(12, record.refundTime != null ? new Timestamp(record.refundTime.getTime()) : null);
            pstmt.setString(13, record.operator);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updateRefundStatus(String refundId, RefundRecord.RefundStatus status,
                                      String channelRefundNo) throws SQLException {
        try (Connection conn = getConnection()) {
            return updateRefundStatusWithConnection(conn, refundId, status, channelRefundNo);
        }
    }

    /**
     * 在调用方事务内更新退款单状态。
     */
    public boolean updateRefundStatusWithConnection(Connection conn, String refundId,
                                                    RefundRecord.RefundStatus status,
                                                    String channelRefundNo) throws SQLException {
        String sql = """
            UPDATE refund_records SET
                status = ?,
                channel_refund_no = ?,
                refund_time = ?
            WHERE refund_id = ?
            """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setString(2, channelRefundNo);
            pstmt.setTimestamp(3, status.isSuccess() ? new Timestamp(System.currentTimeMillis()) : null);
            pstmt.setString(4, refundId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 统计某支付单已发生的退款金额（失败/关闭的退款单不计入）。
     *
     * <p>部分退款后 {@code payment_orders.paid_amount} 不会减少，
     * 必须用它来限制累计退款额，否则同一笔支付可以被退超过实付金额。</p>
     */
    public BigDecimal sumRefundedAmount(String paymentId) throws SQLException {
        try (Connection conn = getConnection()) {
            return sumRefundedAmount(conn, paymentId);
        }
    }

    /**
     * 统计某支付单已发生的退款金额（失败/关闭的退款单不计入），使用调用方事务连接。
     *
     * <p>申请中/处理中的退款单也计入，这样并发退款在预占额度后立即互相可见。</p>
     */
    public BigDecimal sumRefundedAmount(Connection conn, String paymentId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(refund_amount), 0) FROM refund_records "
            + "WHERE payment_id = ? AND status NOT IN ('FAILED', 'CLOSED')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, paymentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                BigDecimal total = rs.next() ? rs.getBigDecimal(1) : null;
                return total != null ? total : BigDecimal.ZERO;
            }
        }
    }

    /**
     * 统计某支付单**已成功**的退款金额。
     *
     * <p>用于判定订单终态：申请中/处理中的退款只是预占额度（防超额），
     * 不能算作已退款，否则并发场景下订单会被提前标成 REFUNDED。</p>
     */
    public BigDecimal sumSettledRefundAmount(String paymentId) throws SQLException {
        try (Connection conn = getConnection()) {
            return sumSettledRefundAmountWithConnection(conn, paymentId);
        }
    }

    /**
     * 统计某支付单**已成功**的退款金额，使用调用方事务连接。
     */
    public BigDecimal sumSettledRefundAmountWithConnection(Connection conn, String paymentId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(refund_amount), 0) FROM refund_records "
            + "WHERE payment_id = ? AND status = 'SUCCESS'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, paymentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                BigDecimal total = rs.next() ? rs.getBigDecimal(1) : null;
                return total != null ? total : BigDecimal.ZERO;
            }
        }
    }

    private PaymentOrder mapResultSetToPaymentOrder(ResultSet rs) throws SQLException {
        PaymentOrder order = new PaymentOrder();
        order.paymentId = rs.getString("payment_id");
        order.transactionId = rs.getString("transaction_id");
        order.merchantOrderNo = rs.getString("merchant_order_no");
        order.paymentType = PaymentOrder.PaymentType.valueOf(rs.getString("payment_type"));
        order.channel = PaymentOrder.PaymentChannel.valueOf(rs.getString("channel"));
        order.amount = rs.getBigDecimal("amount");
        order.status = PaymentOrder.PaymentStatus.valueOf(rs.getString("status"));
        order.qrCodeUrl = rs.getString("qr_code_url");
        order.qrCodeContent = rs.getString("qr_code_content");
        order.paidAmount = rs.getBigDecimal("paid_amount");
        order.discountAmount = rs.getBigDecimal("discount_amount");
        order.createTime = toDate(rs.getTimestamp("create_time"));
        order.payTime = toDate(rs.getTimestamp("pay_time"));
        order.expireTime = toDate(rs.getTimestamp("expire_time"));
        order.channelTransactionId = rs.getString("channel_transaction_id");
        order.channelUserId = rs.getString("channel_user_id");
        order.remark = rs.getString("remark");
        order.terminalId = rs.getString("terminal_id");
        order.operator = rs.getString("operator");
        order.notifyTime = toDate(rs.getTimestamp("notify_time"));
        order.notifyContent = rs.getString("notify_content");
        return order;
    }

    private static Date toDate(Timestamp timestamp) {
        return timestamp != null ? new Date(timestamp.getTime()) : null;
    }

    // ===== 退款对账（F9）=====

    private static final String REFUND_COLUMNS =
        "refund_id, payment_id, transaction_id, merchant_refund_no, channel_refund_no, refund_amount, "
            + "original_amount, reason, status, channel, create_time, refund_time, operator";

    private static RefundRecord mapRefund(ResultSet rs) throws SQLException {
        RefundRecord refund = new RefundRecord();
        refund.refundId = rs.getString("refund_id");
        refund.paymentId = rs.getString("payment_id");
        refund.transactionId = rs.getString("transaction_id");
        refund.merchantRefundNo = rs.getString("merchant_refund_no");
        refund.channelRefundNo = rs.getString("channel_refund_no");
        refund.refundAmount = rs.getBigDecimal("refund_amount");
        refund.originalAmount = rs.getBigDecimal("original_amount");
        refund.reason = rs.getString("reason");
        String status = rs.getString("status");
        try {
            refund.status = status != null ? RefundRecord.RefundStatus.valueOf(status) : RefundRecord.RefundStatus.APPLYING;
        } catch (IllegalArgumentException e) {
            logger.warn("退款单 {} 的状态无法识别，按处理中对待: {}", refund.refundId, status);
            refund.status = RefundRecord.RefundStatus.PROCESSING;
        }
        refund.channel = rs.getString("channel");
        refund.createTime = toDate(rs.getTimestamp("create_time"));
        refund.refundTime = toDate(rs.getTimestamp("refund_time"));
        refund.operator = rs.getString("operator");
        return refund;
    }

    /**
     * 取仍未落终态的退款单（{@code PROCESSING}/{@code APPLYING}），供对账任务回查渠道（F9）。
     *
     * @param createdAfter 只取该时间之后建的（超过最长跟踪时长的留给人工核对，不再反复重试）
     */
    public List<RefundRecord> findUnsettledRefunds(Date createdAfter, int limit) throws SQLException {
        List<RefundRecord> refunds = new ArrayList<>();
        String sql = "SELECT " + REFUND_COLUMNS + " FROM refund_records "
            + "WHERE status NOT IN ('SUCCESS', 'FAILED', 'CLOSED') AND create_time >= ? "
            + "ORDER BY create_time LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, new Timestamp(createdAfter != null ? createdAfter.getTime() : 0L));
            pstmt.setInt(2, Math.max(1, limit));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    refunds.add(mapRefund(rs));
                }
            }
        }
        return refunds;
    }

    /** 按商户退款单号取退款单（F9-c 退款回调用）。 */
    public RefundRecord findRefundByMerchantRefundNo(String merchantRefundNo) throws SQLException {
        if (merchantRefundNo == null || merchantRefundNo.isBlank()) {
            return null;
        }
        String sql = "SELECT " + REFUND_COLUMNS + " FROM refund_records WHERE merchant_refund_no = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, merchantRefundNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapRefund(rs) : null;
            }
        }
    }

    /**
     * 列出退款单（财务对账 / 人工核对用）。
     *
     * @param status 退款状态名（如 {@code PROCESSING}）；为空/空白时只列**未落终态**的
     */
    public List<RefundRecord> listRefunds(String status, int limit) throws SQLException {
        List<RefundRecord> refunds = new ArrayList<>();
        boolean filterByStatus = status != null && !status.isBlank();
        String sql = "SELECT " + REFUND_COLUMNS + " FROM refund_records "
            + (filterByStatus ? "WHERE status = ? " : "WHERE status NOT IN ('SUCCESS', 'FAILED', 'CLOSED') ")
            + "ORDER BY create_time DESC LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int index = 1;
            if (filterByStatus) {
                pstmt.setString(index++, status.trim().toUpperCase(java.util.Locale.ROOT));
            }
            pstmt.setInt(index, Math.max(1, limit));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    refunds.add(mapRefund(rs));
                }
            }
        }
        return refunds;
    }

    /** 超过最长跟踪时长仍未终态的退款单数量：进入"需人工核对"，对账任务只告警不再重试。 */
    public int countStaleUnsettledRefunds(Date createdBefore) throws SQLException {
        String sql = "SELECT COUNT(*) FROM refund_records "
            + "WHERE status NOT IN ('SUCCESS', 'FAILED', 'CLOSED') AND create_time < ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, new Timestamp(createdBefore != null ? createdBefore.getTime() : System.currentTimeMillis()));
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * 带状态条件的退款状态迁移（F9）：{@code WHERE refund_id = ? AND status = ?}。
     *
     * <p>不能沿用 {@link #updateRefundStatusWithConnection}（裸 UPDATE）——对账任务与迟到回调可能并发，
     * 裸 UPDATE 会把已经落定 SUCCESS 的退款改回 PROCESSING，造成状态回退与重复结算。</p>
     *
     * @return 是否发生迁移（false = 该单已被他人推进到其它状态）
     */
    public boolean updateRefundStatusIfNotFinalWithConnection(Connection conn, String refundId,
                                                              RefundRecord.RefundStatus fromStatus,
                                                              RefundRecord.RefundStatus toStatus,
                                                              String channelRefundNo) throws SQLException {
        String sql = "UPDATE refund_records SET status = ?, channel_refund_no = COALESCE(?, channel_refund_no), "
            + "refund_time = ? WHERE refund_id = ? AND status = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, toStatus.name());
            pstmt.setString(2, channelRefundNo);
            pstmt.setTimestamp(3, toStatus.isSuccess() ? new Timestamp(System.currentTimeMillis()) : null);
            pstmt.setString(4, refundId);
            pstmt.setString(5, fromStatus.name());
            return pstmt.executeUpdate() > 0;
        }
    }
}
