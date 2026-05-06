package com.cashier.dao;

import com.cashier.model.Transaction;
import com.cashier.model.Product;
import com.cashier.util.DatabaseManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.*;
import java.math.BigDecimal;
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
        try {
            return DatabaseManager.executeInTransaction(conn -> insertWithConnection(conn, transaction));
        } catch (SQLException e) {
            logger.error("插入交易失败: transactionId={}", transaction.transactionId, e);
            throw e;
        }
    }

    /**
     * 使用指定的数据库连接插入交易记录
     * @param conn 数据库连接
     * @param transaction 交易记录
     * @return 如果插入成功返回true，否则返回false
     * @throws SQLException 数据库操作异常
     */
    public static boolean insertWithConnection(Connection conn, Transaction transaction) throws SQLException {
        // 插入交易主记录
        String sql = "INSERT INTO transactions (transaction_id, timestamp, total_amount, tax, final_amount, " +
                     "payment_method, member_phone, operator_username, operator_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transaction.transactionId);
            pstmt.setString(2, transaction.timestamp);
            pstmt.setBigDecimal(3, transaction.totalAmount);
            pstmt.setBigDecimal(4, transaction.tax);
            pstmt.setBigDecimal(5, transaction.finalAmount);
            pstmt.setString(6, transaction.paymentMethod);
            // 处理 member 和 operator 字段：空字符串转为 NULL
            pstmt.setString(7, transaction.memberPhone != null && transaction.memberPhone.isEmpty() ? null : transaction.memberPhone);
            pstmt.setString(8, transaction.operatorUsername != null && transaction.operatorUsername.isEmpty() ? null : transaction.operatorUsername);
            pstmt.setString(9, transaction.operatorName != null && transaction.operatorName.isEmpty() ? null : transaction.operatorName);
            pstmt.executeUpdate();
        }

        // 插入交易明细
        String detailSql = "INSERT INTO transaction_items (transaction_id, product_id, product_code, product_name, price, quantity, subtotal) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(detailSql)) {
            for (Product item : transaction.items) {
                pstmt.setString(1, transaction.transactionId);
                pstmt.setInt(2, item.id);
                pstmt.setString(3, item.productCode);
                pstmt.setString(4, item.name);
                pstmt.setBigDecimal(5, item.price);
                pstmt.setInt(6, item.quantity);
                pstmt.setBigDecimal(7, item.price.multiply(BigDecimal.valueOf(item.quantity)));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }

        return true;
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
                transaction.totalAmount = rs.getBigDecimal("total_amount");
                transaction.tax = rs.getBigDecimal("tax");
                transaction.finalAmount = rs.getBigDecimal("final_amount");
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
        String sql = "SELECT product_id, product_code, barcode, product_name, price, quantity FROM transaction_items WHERE transaction_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, transactionId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Product product = new Product();
                product.id = rs.getInt("product_id");
                product.productCode = rs.getString("product_code");
                product.barcode = rs.getString("barcode");
                product.name = rs.getString("product_name");
                product.price = rs.getBigDecimal("price");
                product.quantity = rs.getInt("quantity");
                items.add(product);
            }
        }
        return items;
    }

    /**
     * 查询所有交易
     */
    public static List<Transaction> findAll() throws SQLException {
        Map<String, Transaction> transactionMap = new LinkedHashMap<>();

        // 使用 JOIN 查询一次性获取所有交易和明细
        String sql = "SELECT t.transaction_id, t.timestamp, t.total_amount, t.tax, t.final_amount, t.payment_method, " +
                     "t.member_phone, t.operator_username, t.operator_name, " +
                     "ti.id as item_id, ti.product_id, ti.product_code, ti.barcode, ti.product_name, ti.price, ti.quantity, ti.subtotal " +
                     "FROM transactions t " +
                     "LEFT JOIN transaction_items ti ON t.transaction_id = ti.transaction_id " +
                     "ORDER BY t.timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String transactionId = rs.getString("transaction_id");

                // 如果交易还未加载，创建新的交易对象
                if (!transactionMap.containsKey(transactionId)) {
                    Transaction transaction = new Transaction();
                    transaction.transactionId = rs.getString("transaction_id");
                    transaction.timestamp = rs.getString("timestamp");
                    transaction.totalAmount = rs.getBigDecimal("total_amount");
                    transaction.tax = rs.getBigDecimal("tax");
                    transaction.finalAmount = rs.getBigDecimal("final_amount");
                    transaction.paymentMethod = rs.getString("payment_method");
                    transaction.memberPhone = rs.getString("member_phone");
                    transaction.operatorUsername = rs.getString("operator_username");
                    transaction.operatorName = rs.getString("operator_name");
                    transaction.items = new ArrayList<>();

                    transactionMap.put(transactionId, transaction);
                }

                // 添加交易明细
                String productName = rs.getString("product_name");
                if (productName != null) {
                    Product product = new Product();
                    product.id = rs.getInt("product_id");
                    product.productCode = rs.getString("product_code");
                    product.barcode = rs.getString("barcode");
                    product.name = rs.getString("product_name");
                    product.price = rs.getBigDecimal("price");
                    product.quantity = rs.getInt("quantity");
                    transactionMap.get(transactionId).items.add(product);
                }
            }
        }
        return new ArrayList<>(transactionMap.values());
    }

    /**
     * 按日期范围查询交易
     */
    public static List<Transaction> findByDateRange(String startDate, String endDate) throws SQLException {
        Map<String, Transaction> transactionMap = new LinkedHashMap<>();

        String sql = "SELECT t.transaction_id, t.timestamp, t.total_amount, t.tax, t.final_amount, t.payment_method, " +
                     "t.member_phone, t.operator_username, t.operator_name, " +
                     "ti.id as item_id, ti.product_id, ti.product_code, ti.barcode, ti.product_name, ti.price, ti.quantity, ti.subtotal " +
                     "FROM transactions t " +
                     "LEFT JOIN transaction_items ti ON t.transaction_id = ti.transaction_id " +
                     "WHERE t.timestamp BETWEEN ? AND ? " +
                     "ORDER BY t.timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String transactionId = rs.getString("transaction_id");

                // 如果交易还未加载，创建新的交易对象
                if (!transactionMap.containsKey(transactionId)) {
                    Transaction transaction = new Transaction();
                    transaction.transactionId = rs.getString("transaction_id");
                    transaction.timestamp = rs.getString("timestamp");
                    transaction.totalAmount = rs.getBigDecimal("total_amount");
                    transaction.tax = rs.getBigDecimal("tax");
                    transaction.finalAmount = rs.getBigDecimal("final_amount");
                    transaction.paymentMethod = rs.getString("payment_method");
                    transaction.memberPhone = rs.getString("member_phone");
                    transaction.operatorUsername = rs.getString("operator_username");
                    transaction.operatorName = rs.getString("operator_name");
                    transaction.items = new ArrayList<>();

                    transactionMap.put(transactionId, transaction);
                }

                // 添加交易明细
                String productName = rs.getString("product_name");
                if (productName != null) {
                    Product product = new Product();
                    product.id = rs.getInt("product_id");
                    product.productCode = rs.getString("product_code");
                    product.barcode = rs.getString("barcode");
                    product.name = rs.getString("product_name");
                    product.price = rs.getBigDecimal("price");
                    product.quantity = rs.getInt("quantity");
                    transactionMap.get(transactionId).items.add(product);
                }
            }
        }
        return new ArrayList<>(transactionMap.values());
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
                transaction.totalAmount = rs.getBigDecimal("total_amount");
                transaction.tax = rs.getBigDecimal("tax");
                transaction.finalAmount = rs.getBigDecimal("final_amount");
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
                    pstmt.setBigDecimal(3, transaction.totalAmount);
                    pstmt.setBigDecimal(4, transaction.tax);
                    pstmt.setBigDecimal(5, transaction.finalAmount);
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
                        pstmt.setBigDecimal(3, item.price);
                        pstmt.setInt(4, item.quantity);
                        pstmt.setBigDecimal(5, item.price.multiply(BigDecimal.valueOf(item.quantity)));
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

    /**
     * 更新交易状态（带 Connection，用于事务）
     */
    public static boolean updateStatusWithConnection(Connection conn, String transactionId, String status) throws SQLException {
        String sql = "UPDATE transactions SET status = ? WHERE transaction_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, transactionId);
            return pstmt.executeUpdate() > 0;
        }
    }
}
