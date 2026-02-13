package com.cashier.dao;

import com.cashier.model.Transaction;
import com.cashier.model.Product;
import com.cashier.util.DatabaseManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.*;
import java.util.*;

/**
 * 交易数据访问对象
 * 负责交易相关的数据库操作
 */
public class TransactionDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(TransactionDAO.class);

    /**
     * 插入新交易（包含明细）
     */
    public static boolean insert(Transaction transaction) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            // 插入交易主记录
            String sql = "INSERT INTO transactions (transaction_id, timestamp, total_amount, tax, final_amount, " +
                         "payment_method, member_phone, operator_username, operator_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, transaction.transactionId);
                pstmt.setString(2, transaction.timestamp);
                pstmt.setDouble(3, transaction.totalAmount);
                pstmt.setDouble(4, transaction.tax);
                pstmt.setDouble(5, transaction.finalAmount);
                pstmt.setString(6, transaction.paymentMethod);
                // 处理 member 和 operator 字段：空字符串转为 NULL
                pstmt.setString(7, transaction.memberPhone != null && transaction.memberPhone.isEmpty() ? null : transaction.memberPhone);
                pstmt.setString(8, transaction.operatorUsername != null && transaction.operatorUsername.isEmpty() ? null : transaction.operatorUsername);
                pstmt.setString(9, transaction.operatorName != null && transaction.operatorName.isEmpty() ? null : transaction.operatorName);
                pstmt.executeUpdate();
            }

            // 插入交易明细
            String detailSql = "INSERT INTO transaction_items (transaction_id, product_name, price, quantity, subtotal) " +
                              "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(detailSql)) {
                for (Product item : transaction.items) {
                    pstmt.setString(1, transaction.transactionId);
                    pstmt.setString(2, item.name);
                    pstmt.setDouble(3, item.price);
                    pstmt.setInt(4, item.quantity);
                    pstmt.setDouble(5, item.price * item.quantity);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            logger.error("插入交易失败: transactionId={}", transaction.transactionId, e);
            throw e;
        }
    }

    /**
     * 根据交易ID查找交易
     */
    public static Transaction findById(String transactionId) throws SQLException {
        String sql = "SELECT transaction_id, timestamp, total_amount, tax, final_amount, payment_method, member_phone, operator_username, operator_name " +
                     "FROM transactions WHERE transaction_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, transactionId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.transactionId = rs.getString("transaction_id");
                transaction.timestamp = rs.getString("timestamp");
                transaction.totalAmount = rs.getDouble("total_amount");
                transaction.tax = rs.getDouble("tax");
                transaction.finalAmount = rs.getDouble("final_amount");
                transaction.paymentMethod = rs.getString("payment_method");
                transaction.memberPhone = rs.getString("member_phone");
                transaction.operatorUsername = rs.getString("operator_username");
                transaction.operatorName = rs.getString("operator_name");

                // 加载交易明细
                transaction.items = loadItems(transactionId);

                return transaction;
            }
        }
        return null;
    }

    /**
     * 加载交易明细
     */
    private static List<Product> loadItems(String transactionId) throws SQLException {
        List<Product> items = new ArrayList<>();
        String sql = "SELECT product_name, price, quantity FROM transaction_items WHERE transaction_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, transactionId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Product product = new Product(
                    rs.getString("product_name"),
                    rs.getDouble("price"),
                    rs.getInt("quantity")
                );
                items.add(product);
            }
        }
        return items;
    }

    /**
     * 查询所有交易
     */
    public static List<Transaction> findAll() throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT transaction_id, timestamp, total_amount, tax, final_amount, payment_method, member_phone, operator_username, operator_name " +
                     "FROM transactions ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.transactionId = rs.getString("transaction_id");
                transaction.timestamp = rs.getString("timestamp");
                transaction.totalAmount = rs.getDouble("total_amount");
                transaction.tax = rs.getDouble("tax");
                transaction.finalAmount = rs.getDouble("final_amount");
                transaction.paymentMethod = rs.getString("payment_method");
                transaction.memberPhone = rs.getString("member_phone");
                transaction.operatorUsername = rs.getString("operator_username");
                transaction.operatorName = rs.getString("operator_name");

                // 加载交易明细
                transaction.items = loadItems(transaction.transactionId);

                transactions.add(transaction);
            }
        }
        return transactions;
    }

    /**
     * 按日期范围查询交易
     */
    public static List<Transaction> findByDateRange(String startDate, String endDate) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT transaction_id, timestamp, total_amount, tax, final_amount, payment_method, member_phone, operator_username, operator_name " +
                     "FROM transactions WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.transactionId = rs.getString("transaction_id");
                transaction.timestamp = rs.getString("timestamp");
                transaction.totalAmount = rs.getDouble("total_amount");
                transaction.tax = rs.getDouble("tax");
                transaction.finalAmount = rs.getDouble("final_amount");
                transaction.paymentMethod = rs.getString("payment_method");
                transaction.memberPhone = rs.getString("member_phone");
                transaction.operatorUsername = rs.getString("operator_username");
                transaction.operatorName = rs.getString("operator_name");

                transaction.items = loadItems(transaction.transactionId);

                transactions.add(transaction);
            }
        }
        return transactions;
    }

    /**
     * 按支付方式查询交易
     */
    public static List<Transaction> findByPaymentMethod(String paymentMethod) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT transaction_id, timestamp, total_amount, tax, final_amount, payment_method, member_phone, operator_username, operator_name " +
                     "FROM transactions WHERE payment_method = ? ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, paymentMethod);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.transactionId = rs.getString("transaction_id");
                transaction.timestamp = rs.getString("timestamp");
                transaction.totalAmount = rs.getDouble("total_amount");
                transaction.tax = rs.getDouble("tax");
                transaction.finalAmount = rs.getDouble("final_amount");
                transaction.paymentMethod = rs.getString("payment_method");
                transaction.memberPhone = rs.getString("member_phone");
                transaction.operatorUsername = rs.getString("operator_username");
                transaction.operatorName = rs.getString("operator_name");

                transaction.items = loadItems(transaction.transactionId);

                transactions.add(transaction);
            }
        }
        return transactions;
    }

    /**
     * 统计总收入
     */
    public static double getTotalRevenue(String startDate, String endDate) throws SQLException {
        String sql = "SELECT COALESCE(SUM(final_amount), 0) as total FROM transactions WHERE timestamp BETWEEN ? AND ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total");
            }
        }
        return 0.0;
    }

    /**
     * 统计交易数量
     */
    public static int getTransactionCount(String startDate, String endDate) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM transactions WHERE timestamp BETWEEN ? AND ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    /**
     * 批量插入交易
     */
    public static void batchInsert(List<Transaction> transactions) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // 插入交易主记录
            String sql = "INSERT INTO transactions (transaction_id, timestamp, total_amount, tax, final_amount, " +
                         "payment_method, member_phone, operator_username, operator_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Transaction transaction : transactions) {
                    pstmt.setString(1, transaction.transactionId);
                    pstmt.setString(2, transaction.timestamp);
                    pstmt.setDouble(3, transaction.totalAmount);
                    pstmt.setDouble(4, transaction.tax);
                    pstmt.setDouble(5, transaction.finalAmount);
                    pstmt.setString(6, transaction.paymentMethod);
                    // 将空字符串转换为 NULL
                    String memberPhone = transaction.memberPhone;
                    if (memberPhone != null && memberPhone.trim().isEmpty()) {
                        memberPhone = null;
                    }
                    pstmt.setString(7, memberPhone);
                    pstmt.setString(8, transaction.operatorUsername);
                    pstmt.setString(9, transaction.operatorName);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            // 插入所有交易明细
            String detailSql = "INSERT INTO transaction_items (transaction_id, product_name, price, quantity, subtotal) " +
                              "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(detailSql)) {
                for (Transaction transaction : transactions) {
                    for (Product item : transaction.items) {
                        pstmt.setString(1, transaction.transactionId);
                        pstmt.setString(2, item.name);
                        pstmt.setDouble(3, item.price);
                        pstmt.setInt(4, item.quantity);
                        pstmt.setDouble(5, item.price * item.quantity);
                        pstmt.addBatch();
                    }
                }
                pstmt.executeBatch();
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("事务回滚失败", ex);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("关闭数据库连接失败", e);
                }
            }
        }
    }
}
