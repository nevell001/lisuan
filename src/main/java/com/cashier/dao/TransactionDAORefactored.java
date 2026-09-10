package com.cashier.dao;

import com.cashier.model.Product;
import com.cashier.model.Transaction;
import com.cashier.model.TransactionStatistics;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class TransactionDAORefactored extends BaseDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(TransactionDAORefactored.class);

    public boolean insert(Transaction transaction) throws SQLException {
        try {
            return executeInTransaction(conn -> insertWithConnection(conn, transaction));
        } catch (SQLException e) {
            logger.error("插入交易失败: transactionId={}", transaction.transactionId, e);
            throw e;
        }
    }

    public boolean insertWithConnection(Connection conn, Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transactions (transaction_id, timestamp, total_amount, tax, final_amount, " +
            "payment_method, member_phone, operator_username, operator_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transaction.transactionId);
            pstmt.setString(2, transaction.timestamp);
            pstmt.setBigDecimal(3, transaction.totalAmount);
            pstmt.setBigDecimal(4, transaction.tax);
            pstmt.setBigDecimal(5, transaction.finalAmount);
            pstmt.setString(6, transaction.paymentMethod);
            pstmt.setString(7, transaction.memberPhone != null && transaction.memberPhone.isEmpty() ? null : transaction.memberPhone);
            pstmt.setString(8, transaction.operatorUsername != null && transaction.operatorUsername.isEmpty() ? null : transaction.operatorUsername);
            pstmt.setString(9, transaction.operatorName != null && transaction.operatorName.isEmpty() ? null : transaction.operatorName);
            pstmt.executeUpdate();
        }

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

    public Transaction findById(String transactionId) throws SQLException {
        String sql = "SELECT t.transaction_id, t.timestamp, t.total_amount, t.tax, t.final_amount, t.payment_method, " +
            "t.member_phone, t.operator_username, t.status, " +
            "COALESCE(t.operator_name, u.name, t.operator_username) AS operator_name " +
            "FROM transactions t LEFT JOIN users u ON t.operator_username = u.username WHERE t.transaction_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Transaction transaction = mapTransaction(rs);
                    transaction.items = loadItems(transactionId);
                    return transaction;
                }
            }
        }
        return null;
    }

    private List<Product> loadItems(String transactionId) throws SQLException {
        String sql = "SELECT ti.product_id, ti.product_code, ti.barcode, ti.product_name, ti.price, ti.quantity, p.category " +
            "FROM transaction_items ti LEFT JOIN products p ON ti.product_id = p.id WHERE ti.transaction_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Product> items = new ArrayList<>();
                while (rs.next()) {
                    Product product = new Product();
                    product.id = rs.getInt("product_id");
                    product.productCode = rs.getString("product_code");
                    product.barcode = rs.getString("barcode");
                    product.name = rs.getString("product_name");
                    product.price = rs.getBigDecimal("price");
                    product.quantity = rs.getInt("quantity");
                    product.category = rs.getString("category");
                    items.add(product);
                }
                return items;
            }
        }
    }

    private static final String JOIN_SELECT =
        "SELECT t.transaction_id, t.timestamp, t.total_amount, t.tax, t.final_amount, t.payment_method, " +
        "t.member_phone, t.operator_username, t.status, " +
        "COALESCE(t.operator_name, u.name, t.operator_username) AS operator_name, " +
        "ti.id as item_id, ti.product_id, ti.product_code, ti.barcode, ti.product_name, ti.price, ti.quantity, ti.subtotal, " +
        "p.category AS category ";

    public List<Transaction> findAll() throws SQLException {
        String sql = JOIN_SELECT +
            "FROM transactions t LEFT JOIN users u ON t.operator_username = u.username " +
            "LEFT JOIN transaction_items ti ON t.transaction_id = ti.transaction_id " +
            "LEFT JOIN products p ON ti.product_id = p.id ORDER BY t.timestamp DESC";
        return queryJoinedTransactions(sql);
    }

    public List<Transaction> findRecent(int limit) throws SQLException {
        if (limit < 1) {
            return List.of();
        }
        String sql = JOIN_SELECT +
            "FROM (SELECT transaction_id, timestamp, total_amount, tax, final_amount, payment_method, " +
            "member_phone, operator_username, operator_name, status FROM transactions ORDER BY timestamp DESC LIMIT ?) t " +
            "LEFT JOIN users u ON t.operator_username = u.username " +
            "LEFT JOIN transaction_items ti ON t.transaction_id = ti.transaction_id " +
            "LEFT JOIN products p ON ti.product_id = p.id ORDER BY t.timestamp DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            return readJoinedTransactions(pstmt.executeQuery());
        }
    }

    public List<Transaction> findByDateRange(String startDate, String endDate) throws SQLException {
        String sql = JOIN_SELECT +
            "FROM transactions t LEFT JOIN users u ON t.operator_username = u.username " +
            "LEFT JOIN transaction_items ti ON t.transaction_id = ti.transaction_id " +
            "LEFT JOIN products p ON ti.product_id = p.id " +
            "WHERE t.timestamp BETWEEN ? AND ? ORDER BY t.timestamp DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            return readJoinedTransactions(pstmt.executeQuery());
        }
    }

    public List<Transaction> findByPaymentMethod(String paymentMethod) throws SQLException {
        String sql = JOIN_SELECT +
            "FROM transactions t LEFT JOIN users u ON t.operator_username = u.username " +
            "LEFT JOIN transaction_items ti ON t.transaction_id = ti.transaction_id " +
            "LEFT JOIN products p ON ti.product_id = p.id " +
            "WHERE t.payment_method = ? ORDER BY t.timestamp DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, paymentMethod);
            return readJoinedTransactions(pstmt.executeQuery());
        }
    }

    private List<Transaction> queryJoinedTransactions(String sql) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return readJoinedTransactions(rs);
        }
    }

    private static List<Transaction> readJoinedTransactions(ResultSet rs) throws SQLException {
        Map<String, Transaction> transactionMap = new LinkedHashMap<>();
        while (rs.next()) {
            addJoinedTransactionRow(transactionMap, rs);
        }
        return new ArrayList<>(transactionMap.values());
    }

    public List<Map<String, Object>> getTopProducts(int limit) throws SQLException {
        if (limit < 1) {
            return List.of();
        }
        String sql = "SELECT product_name, SUM(quantity) AS quantity, " +
            "COALESCE(SUM(COALESCE(subtotal, price * quantity)), 0) AS amount " +
            "FROM transaction_items WHERE product_name IS NOT NULL " +
            "GROUP BY product_name ORDER BY quantity DESC LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Map<String, Object>> products = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", rs.getString("product_name"));
                    item.put("quantity", rs.getInt("quantity"));
                    item.put("amount", rs.getBigDecimal("amount"));
                    products.add(item);
                }
                return products;
            }
        }
    }

    public List<Map<String, Object>> getPaymentMethodStats() throws SQLException {
        String sql = "SELECT COALESCE(payment_method, '未知') AS method, COUNT(*) AS count, " +
            "COALESCE(SUM(final_amount), 0) AS amount FROM transactions " +
            "GROUP BY COALESCE(payment_method, '未知') ORDER BY amount DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            List<Map<String, Object>> methods = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("method", rs.getString("method"));
                item.put("count", rs.getInt("count"));
                item.put("amount", rs.getBigDecimal("amount"));
                methods.add(item);
            }
            return methods;
        }
    }

    public TransactionStatistics getStatistics(String startDate, String endDate) throws SQLException {
        String sql = "SELECT COUNT(*) AS total_transactions, COALESCE(SUM(final_amount), 0) AS total_amount, " +
            // 收银端写入的是中文支付方式，兼容旧数据/接口写入的代码形式
            "SUM(CASE WHEN payment_method IN ('现金', 'CASH') THEN 1 ELSE 0 END) AS cash_count, " +
            "SUM(CASE WHEN member_phone IS NOT NULL THEN 1 ELSE 0 END) AS member_count " +
            "FROM transactions WHERE timestamp BETWEEN ? AND ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new TransactionStatistics(
                        rs.getInt("total_transactions"),
                        rs.getBigDecimal("total_amount"),
                        0,
                        rs.getInt("cash_count"),
                        rs.getInt("member_count"));
                }
            }
        }
        return new TransactionStatistics(0, BigDecimal.ZERO, 0, 0, 0);
    }

    public double getTotalRevenue(String startDate, String endDate) throws SQLException {
        Number total = (Number) queryScalar(
            "SELECT COALESCE(SUM(final_amount), 0) as total FROM transactions WHERE timestamp BETWEEN ? AND ?",
            startDate, endDate);
        return total != null ? total.doubleValue() : 0.0;
    }

    public int getTransactionCount(String startDate, String endDate) throws SQLException {
        return queryInt(
            "SELECT COUNT(*) as count FROM transactions WHERE timestamp BETWEEN ? AND ?", startDate, endDate);
    }

    public void batchInsert(List<Transaction> transactions) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
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
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("事务回滚失败", ex);
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.error("恢复自动提交失败", e);
                }
            }
        }
    }

    public boolean updateStatusWithConnection(Connection conn, String transactionId, String status) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
            "UPDATE transactions SET status = ? WHERE transaction_id = ?")) {
            pstmt.setString(1, status);
            pstmt.setString(2, transactionId);
            return pstmt.executeUpdate() > 0;
        }
    }

    private static void addJoinedTransactionRow(Map<String, Transaction> transactionMap, ResultSet rs) throws SQLException {
        String transactionId = rs.getString("transaction_id");
        Transaction transaction = transactionMap.get(transactionId);
        if (transaction == null) {
            transaction = mapTransaction(rs);
            transactionMap.put(transactionId, transaction);
        }
        addJoinedItem(transaction, rs);
    }

    private static Transaction mapTransaction(ResultSet rs) throws SQLException {
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
        transaction.status = rs.getString("status");
        transaction.items = new ArrayList<>();
        return transaction;
    }

    private static void addJoinedItem(Transaction transaction, ResultSet rs) throws SQLException {
        String productName = rs.getString("product_name");
        if (productName == null) {
            return;
        }
        Product product = new Product();
        product.id = rs.getInt("product_id");
        product.productCode = rs.getString("product_code");
        product.barcode = rs.getString("barcode");
        product.name = productName;
        product.price = rs.getBigDecimal("price");
        product.quantity = rs.getInt("quantity");
        product.category = rs.getString("category");
        transaction.items.add(product);
    }
}
