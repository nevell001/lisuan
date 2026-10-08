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
import java.sql.Types;
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
        String sql = "SELECT ti.id AS item_id, ti.product_id, ti.product_code, ti.barcode, ti.product_name, " +
            "ti.price, ti.quantity, p.category " +
            "FROM transaction_items ti LEFT JOIN products p ON ti.product_id = p.id WHERE ti.transaction_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Product> items = new ArrayList<>();
                while (rs.next()) {
                    Product product = new Product();
                    // 行 id 必须带出来：退货的按行校验/归属靠它（F10-c）
                    product.transactionItemId = rs.getInt("item_id");
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

    /**
     * 按日期范围查询交易并限制条数。
     *
     * <p>limit 作用在交易主表的子查询上（与 {@link #findRecent(int)} 同理），
     * 否则 JOIN transaction_items 后 LIMIT 会按明细行截断，返回的交易条数少于 limit。</p>
     */
    public List<Transaction> findByDateRange(String startDate, String endDate, int limit) throws SQLException {
        if (limit < 1) {
            return List.of();
        }
        String sql = JOIN_SELECT +
            "FROM (SELECT transaction_id, timestamp, total_amount, tax, final_amount, payment_method, " +
            "member_phone, operator_username, operator_name, status FROM transactions " +
            "WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC LIMIT ?) t " +
            "LEFT JOIN users u ON t.operator_username = u.username " +
            "LEFT JOIN transaction_items ti ON t.transaction_id = ti.transaction_id " +
            "LEFT JOIN products p ON ti.product_id = p.id ORDER BY t.timestamp DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            pstmt.setInt(3, limit);
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
        // 商品改名后仍要把历史销量归到改名后的商品上：优先按 product_id 取商品当前名称，
        // 只有 product_id 为空的旧数据才回退用明细里存的名称。
        String sql = "SELECT name, SUM(quantity) AS quantity, " +
            "COALESCE(SUM(amount), 0) AS amount FROM (" +
            "SELECT COALESCE(p.name, ti.product_name) AS name, ti.quantity AS quantity, " +
            "COALESCE(ti.subtotal, ti.price * ti.quantity) AS amount " +
            "FROM transaction_items ti " +
            "LEFT JOIN products p ON p.id = ti.product_id " +
            "WHERE ti.product_name IS NOT NULL) t " +
            "GROUP BY name ORDER BY quantity DESC LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Map<String, Object>> products = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", rs.getString("name"));
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
            "WHERE COALESCE(status, 'NORMAL') <> 'REFUNDED' " +
            "GROUP BY COALESCE(payment_method, '未知') ORDER BY amount DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            // 同一支付方式的多种写法（「现金」/CASH/現金）必须合并成一行：
            // 否则报表里一种方式出现多行，前端看到的"支付方式统计"直接翻倍（TD-002）
            Map<String, Map<String, Object>> mergedByCanonical = new LinkedHashMap<>();
            while (rs.next()) {
                String raw = rs.getString("method");
                String canonical = com.cashier.util.I18nUiUtils.canonicalPaymentMethod(raw);
                Map<String, Object> item = mergedByCanonical.get(canonical);
                if (item == null) {
                    // 标签优先用落库规范中文值，保证已有的中文数据输出不变
                    String label = com.cashier.util.I18nUiUtils.storedPaymentMethod(raw);
                    item = new HashMap<>();
                    item.put("method", label != null ? label : raw);
                    item.put("count", 0);
                    item.put("amount", BigDecimal.ZERO);
                    mergedByCanonical.put(canonical, item);
                }
                item.put("count", ((Number) item.get("count")).intValue() + rs.getInt("count"));
                BigDecimal groupAmount = rs.getBigDecimal("amount");
                item.put("amount", ((BigDecimal) item.get("amount"))
                    .add(groupAmount != null ? groupAmount : BigDecimal.ZERO));
            }
            List<Map<String, Object>> methods = new ArrayList<>(mergedByCanonical.values());
            methods.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));
            return methods;
        }
    }

    public TransactionStatistics getStatistics(String startDate, String endDate) throws SQLException {
        String sql = "SELECT COUNT(*) AS total_transactions, COALESCE(SUM(final_amount), 0) AS total_amount, " +
            // 收银端写入的是中文支付方式，兼容旧数据/接口写入的代码与繁体形式
            "SUM(CASE WHEN payment_method IN ('现金', '現金', 'CASH', 'Cash') THEN 1 ELSE 0 END) AS cash_count, " +
            "SUM(CASE WHEN member_phone IS NOT NULL THEN 1 ELSE 0 END) AS member_count " +
            // 已整单退款的交易不算营业额（营业额为净额口径，见 docs/TECH_DEBT.md 的 TD-003）
            "FROM transactions WHERE timestamp BETWEEN ? AND ? AND COALESCE(status, 'NORMAL') <> 'REFUNDED'";
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
            "SELECT COALESCE(SUM(final_amount), 0) as total FROM transactions " +
                "WHERE timestamp BETWEEN ? AND ? AND COALESCE(status, 'NORMAL') <> 'REFUNDED'",
            startDate, endDate);
        return total != null ? total.doubleValue() : 0.0;
    }

    public int getTransactionCount(String startDate, String endDate) throws SQLException {
        return queryInt(
            "SELECT COUNT(*) as count FROM transactions " +
                "WHERE timestamp BETWEEN ? AND ? AND COALESCE(status, 'NORMAL') <> 'REFUNDED'",
            startDate, endDate);
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

                String detailSql = "INSERT INTO transaction_items (transaction_id, product_id, product_name, price, quantity, subtotal) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(detailSql)) {
                    for (Transaction transaction : transactions) {
                        for (Product item : transaction.items) {
                            pstmt.setString(1, transaction.transactionId);
                            // 有商品 ID 就落库，别让新数据退化成只能按名称关联
                            if (item.id > 0) {
                                pstmt.setInt(2, item.id);
                            } else {
                                pstmt.setNull(2, Types.INTEGER);
                            }
                            pstmt.setString(3, item.name);
                            pstmt.setBigDecimal(4, item.price);
                            pstmt.setInt(5, item.quantity);
                            pstmt.setBigDecimal(6, item.price.multiply(BigDecimal.valueOf(item.quantity)));
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

    /**
     * 原子抢占退款标记：仅当交易当前不是 REFUNDED 时才置为 REFUNDED。
     *
     * <p>用于退款幂等——并发或重复的退款请求只有一次能更新到 1 行，
     * 其余返回 false，避免同一笔交易被重复退款。</p>
     */
    public boolean claimRefundWithConnection(Connection conn, String transactionId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
            "UPDATE transactions SET status = 'REFUNDED' WHERE transaction_id = ? AND status <> 'REFUNDED'")) {
            pstmt.setString(1, transactionId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 锁定原交易行，用于把"退货建单 + 可退量校验"放进同一事务串行化（F10）。
     *
     * <p>没有这把锁时，两个终端各自在事务外校验"已退量 + 本次 ≤ 原销量"都能通过，
     * 随后双双插入退货单——库存和钱都会被退两次。行锁让同一交易的建单排队，
     * 后到者在锁内重算时就能看到先到者已提交的台账占用。</p>
     *
     * @return 交易存在且已加锁（{@code SELECT ... FOR UPDATE}）
     */
    public boolean lockForReturnWithConnection(Connection conn, String transactionId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
            "SELECT transaction_id FROM transactions WHERE transaction_id = ? FOR UPDATE")) {
            pstmt.setString(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 原交易各商品的销量基数（**按商品跨行合计**，历史数据 product_id 为空时并入 0）。
     *
     * <p>退货校验必须按商品合计，不能按明细行比较：同一商品在交易里出现多行时，
     * 按行比较会把"这一行退了 1 件"误判成"只退了这一行的量"。</p>
     */
    /** 原交易每**行**的销售数量（行 id → 数量），供 F10-c 的行级可退量校验。 */
    public Map<Integer, Integer> findItemQuantitiesByLineWithConnection(Connection conn, String transactionId)
            throws SQLException {
        Map<Integer, Integer> quantities = new HashMap<>();
        String sql = "SELECT id, quantity FROM transaction_items WHERE transaction_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, transactionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    quantities.put(rs.getInt("id"), rs.getInt("quantity"));
                }
            }
        }
        return quantities;
    }

    public Map<Integer, Integer> sumItemQuantitiesByProductWithConnection(Connection conn, String transactionId)
            throws SQLException {
        Map<Integer, Integer> quantities = new HashMap<>();
        String sql = "SELECT COALESCE(product_id, 0) AS pid, SUM(quantity) AS qty FROM transaction_items "
            + "WHERE transaction_id = ? GROUP BY COALESCE(product_id, 0)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    quantities.put(rs.getInt("pid"), rs.getInt("qty"));
                }
            }
        }
        return quantities;
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
