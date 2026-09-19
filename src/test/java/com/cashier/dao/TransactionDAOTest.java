package com.cashier.dao;

import com.cashier.model.Product;
import com.cashier.model.Transaction;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionDAOTest extends DatabaseTestBase {

    private final TransactionDAORefactored transactionDAO = DAOFactory.getInstance().getTransactionDAO();

    private Transaction insertTransaction(String id, String timestamp, String paymentMethod) throws Exception {
        Transaction transaction = new Transaction(
            id, timestamp, List.of(), BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);
        transaction.paymentMethod = paymentMethod;
        transaction.operatorUsername = "op";
        transaction.operatorName = "操作员";
        assertTrue(transactionDAO.insert(transaction));
        return transaction;
    }

    @Test
    @DisplayName("插入并按ID查询交易")
    void insertAndFindById() throws Exception {
        insertTransaction("T-DAO-001", "2026-08-22 10:00:00", "现金");

        Transaction found = transactionDAO.findById("T-DAO-001");
        assertNotNull(found);
        assertEquals("现金", found.paymentMethod);
    }

    @Test
    @DisplayName("最近交易按时间倒序返回")
    void findRecent() throws Exception {
        insertTransaction("T-REC-001", "2026-08-22 10:00:00", "现金");
        insertTransaction("T-REC-002", "2026-08-22 11:00:00", "微信");

        List<Transaction> recent = transactionDAO.findRecent(10);
        assertEquals(2, recent.size());
    }

    @Test
    @DisplayName("按日期范围与支付方式查询")
    void findByDateRangeAndPaymentMethod() throws Exception {
        insertTransaction("T-RNG-001", "2026-08-22 10:00:00", "现金");

        assertEquals(1, transactionDAO.findByDateRange("2026-08-22 00:00:00", "2026-08-22 23:59:59").size());
        assertEquals(1, transactionDAO.findByPaymentMethod("现金").size());
        assertEquals(0, transactionDAO.findByPaymentMethod("支付宝").size());
    }

    @Test
    @DisplayName("统计聚合查询")
    void statistics() throws Exception {
        insertTransaction("T-STAT-001", "2026-08-22 10:00:00", "现金");

        assertEquals(1, transactionDAO.getTransactionCount("2026-08-22 00:00:00", "2026-08-22 23:59:59"));
        assertEquals(0, BigDecimal.TEN.compareTo(BigDecimal.valueOf(
            transactionDAO.getTotalRevenue("2026-08-22 00:00:00", "2026-08-22 23:59:59"))));
        assertNotNull(transactionDAO.getStatistics("2026-08-22 00:00:00", "2026-08-22 23:59:59"));
        assertNotNull(transactionDAO.getTopProducts(10));
        assertNotNull(transactionDAO.getPaymentMethodStats());
    }

    @Test
    @DisplayName("热销榜按商品ID归并改名后的历史销量")
    void topProductsMergeHistoryAfterRename() throws Exception {
        ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
        Product product = new Product(0, "RPT-TOP-001", "报表热销-旧名", 10.0, 10, "测试分类",
            "RPT-BAR-001", "件", "描述", "品牌", "供应商", "规格", 0, 1.0);
        assertTrue(productDAO.insert(product));

        insertTransaction("T-TOP-RPT-001", "2026-08-22 10:00:00", "现金");
        try (Connection conn = getTestConnection()) {
            try (java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO transaction_items (transaction_id, product_id, product_name, price, quantity, subtotal) "
                    + "VALUES ('T-TOP-RPT-001', " + product.id + ", '报表热销-旧名', 10, 4, 40)");
            }
        }

        Product reloaded = productDAO.findById(product.id);
        reloaded.name = "报表热销-新名";
        assertTrue(productDAO.update(reloaded));

        // 改名后同一商品的历史销量必须归到当前名称下，而不是按明细里的旧名称分裂成两行
        Map<String, Object> row = transactionDAO.getTopProducts(100).stream()
            .filter(item -> "报表热销-新名".equals(item.get("name")))
            .findFirst()
            .orElse(null);
        assertNotNull(row, "改名后热销榜应显示商品当前名称: " + transactionDAO.getTopProducts(100));
        assertEquals(4, ((Number) row.get("quantity")).intValue());
    }

    @Test
    @DisplayName("不存在的交易返回null")
    void findMissingReturnsNull() throws Exception {
        assertNull(transactionDAO.findById("T-MISSING"));
    }

    @Test
    @DisplayName("退款标记只能被抢占一次")
    void claimRefundOnlySucceedsOnce() throws Exception {
        insertTransaction("T-CLAIM-001", "2026-08-22 10:00:00", "现金");

        try (Connection conn = getTestConnection()) {
            assertTrue(transactionDAO.claimRefundWithConnection(conn, "T-CLAIM-001"));
            // 并发/重复退款时第二次必须拿不到，否则同一单会被退两次
            assertFalse(transactionDAO.claimRefundWithConnection(conn, "T-CLAIM-001"));
        }
        assertEquals("REFUNDED", transactionDAO.findById("T-CLAIM-001").status);
    }
}
