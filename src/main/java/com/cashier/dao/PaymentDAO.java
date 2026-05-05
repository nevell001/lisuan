package com.cashier.dao;

import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;
import com.cashier.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 支付订单数据访问层
 */
public class PaymentDAO {
    private static final Logger logger = LoggerFactory.getLogger(PaymentDAO.class);
    
    /**
     * 创建支付订单表
     */
    public static void createTable() throws SQLException {
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
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute(refundSql);
            logger.info("支付订单表创建成功");
        }
    }
    
    /**
     * 插入支付订单
     */
    public static boolean insert(PaymentOrder order) throws SQLException {
        String sql = """
            INSERT INTO payment_orders (
                payment_id, transaction_id, merchant_order_no, payment_type, channel,
                amount, status, qr_code_url, qr_code_content, paid_amount, discount_amount,
                create_time, pay_time, expire_time, channel_transaction_id, channel_user_id,
                remark, terminal_id, operator, notify_time, notify_content
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        // 生成支付ID
        if (order.paymentId == null) {
            order.paymentId = "PAY" + System.currentTimeMillis();
        }
        
        try (Connection conn = DatabaseManager.getConnection();
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
    
    /**
     * 根据ID查询
     */
    public static PaymentOrder findById(String paymentId) throws SQLException {
        String sql = "SELECT * FROM payment_orders WHERE payment_id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, paymentId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaymentOrder(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * 根据商户订单号查询
     */
    public static PaymentOrder findByMerchantOrderNo(String merchantOrderNo) throws SQLException {
        String sql = "SELECT * FROM payment_orders WHERE merchant_order_no = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, merchantOrderNo);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaymentOrder(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * 根据交易ID查询
     */
    public static List<PaymentOrder> findByTransactionId(String transactionId) throws SQLException {
        String sql = "SELECT * FROM payment_orders WHERE transaction_id = ? ORDER BY create_time DESC";
        List<PaymentOrder> orders = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, transactionId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToPaymentOrder(rs));
                }
            }
        }
        return orders;
    }
    
    /**
     * 查询待支付订单
     */
    public static List<PaymentOrder> findWaitingOrders() throws SQLException {
        String sql = "SELECT * FROM payment_orders WHERE status IN ('CREATED', 'WAITING') AND expire_time > NOW() ORDER BY create_time DESC";
        List<PaymentOrder> orders = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                orders.add(mapResultSetToPaymentOrder(rs));
            }
        }
        return orders;
    }
    
    /**
     * 更新支付状态
     */
    public static boolean updateStatus(String paymentId, PaymentOrder.PaymentStatus status) throws SQLException {
        String sql = "UPDATE payment_orders SET status = ? WHERE payment_id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status.name());
            pstmt.setString(2, paymentId);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * 更新支付成功信息
     */
    public static boolean updatePaymentSuccess(String paymentId, String channelTransactionId,
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
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(2, channelTransactionId);
            pstmt.setString(3, channelUserId);
            pstmt.setBigDecimal(4, paidAmount);
            pstmt.setBigDecimal(5, discountAmount);
            pstmt.setString(6, paymentId);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * 更新回调信息
     */
    public static boolean updateNotifyInfo(String paymentId, String notifyContent) throws SQLException {
        String sql = "UPDATE payment_orders SET notify_time = ?, notify_content = ? WHERE payment_id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(2, notifyContent);
            pstmt.setString(3, paymentId);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * 关闭过期订单
     */
    public static int closeExpiredOrders() throws SQLException {
        String sql = "UPDATE payment_orders SET status = 'CLOSED' WHERE status IN ('CREATED', 'WAITING') AND expire_time < NOW()";
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            
            return stmt.executeUpdate(sql);
        }
    }
    
    /**
     * 查询支付统计
     */
    public static Map<String, Object> getDailyStats(Date date) throws SQLException {
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
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, new Timestamp(date.getTime()));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> stats = new java.util.HashMap<>();
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
        return new java.util.HashMap<>();
    }
    
    // ========== 退款记录操作 ==========
    
    /**
     * 插入退款记录
     */
    public static boolean insertRefund(RefundRecord record) throws SQLException {
        String sql = """
            INSERT INTO refund_records (
                refund_id, payment_id, transaction_id, merchant_refund_no, channel_refund_no,
                refund_amount, original_amount, reason, status, channel,
                create_time, refund_time, operator
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        if (record.refundId == null) {
            record.refundId = "RFD" + System.currentTimeMillis();
        }
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
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
    
    /**
     * 更新退款状态
     */
    public static boolean updateRefundStatus(String refundId, RefundRecord.RefundStatus status, 
                                              String channelRefundNo) throws SQLException {
        String sql = """
            UPDATE refund_records SET 
                status = ?, 
                channel_refund_no = ?, 
                refund_time = ?
            WHERE refund_id = ?
            """;
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status.name());
            pstmt.setString(2, channelRefundNo);
            pstmt.setTimestamp(3, status.isSuccess() ? new Timestamp(System.currentTimeMillis()) : null);
            pstmt.setString(4, refundId);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * ResultSet 映射
     */
    private static PaymentOrder mapResultSetToPaymentOrder(ResultSet rs) throws SQLException {
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
        
        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) order.createTime = new Date(createTime.getTime());
        
        Timestamp payTime = rs.getTimestamp("pay_time");
        if (payTime != null) order.payTime = new Date(payTime.getTime());
        
        Timestamp expireTime = rs.getTimestamp("expire_time");
        if (expireTime != null) order.expireTime = new Date(expireTime.getTime());
        
        order.channelTransactionId = rs.getString("channel_transaction_id");
        order.channelUserId = rs.getString("channel_user_id");
        order.remark = rs.getString("remark");
        order.terminalId = rs.getString("terminal_id");
        order.operator = rs.getString("operator");
        
        Timestamp notifyTime = rs.getTimestamp("notify_time");
        if (notifyTime != null) order.notifyTime = new Date(notifyTime.getTime());
        
        order.notifyContent = rs.getString("notify_content");
        
        return order;
    }
}