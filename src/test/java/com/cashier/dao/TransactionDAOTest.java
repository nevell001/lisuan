package com.cashier.dao;

import com.cashier.model.Transaction;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

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
