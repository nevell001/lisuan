package com.cashier.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchOperationUtilTest extends DatabaseTestBase {

    private static final String INSERT_PRODUCT_SQL = "INSERT INTO products (name, price, quantity) VALUES (?, ?, ?)";

    @BeforeEach
    void setUp() throws Exception {
        if (!DatabaseTestBase.isInitialized()) {
            DatabaseTestBase.initTestDatabase();
        }
        clearTestData();
    }

    @Test
    @DisplayName("batchInsert 在自管事务时会提交并恢复 autoCommit")
    void testBatchInsertCommitsWhenManagingOwnTransaction() throws Exception {
        try (Connection conn = getTestConnection()) {
            List<Object[]> params = List.of(
                new Object[]{"批量商品A", new BigDecimal("12.50"), 3},
                new Object[]{"批量商品B", new BigDecimal("18.00"), 5}
            );

            int[] results = BatchOperationUtil.batchInsert(conn, INSERT_PRODUCT_SQL, params);

            assertEquals(2, results.length);
            assertTrue(conn.getAutoCommit());
        }

        assertEquals(2, countProducts());
    }

    @Test
    @DisplayName("batchInsert 不会擅自提交外部事务")
    void testBatchInsertDoesNotCommitExternalTransaction() throws Exception {
        try (Connection conn = getTestConnection()) {
            conn.setAutoCommit(false);
            List<Object[]> params = Collections.singletonList(
                new Object[]{"未提交商品", new BigDecimal("9.90"), 1}
            );

            int[] results = BatchOperationUtil.batchInsert(conn, INSERT_PRODUCT_SQL, params);

            assertEquals(1, results.length);
            assertFalse(conn.getAutoCommit());
            assertEquals(1, countProducts(conn));

            conn.rollback();
        }

        assertEquals(0, countProducts());
    }

    @Test
    @DisplayName("executeInTransaction 不会接管外部事务的提交")
    void testExecuteInTransactionDoesNotCommitExternalTransaction() throws Exception {
        try (Connection conn = getTestConnection()) {
            conn.setAutoCommit(false);

            String insertedName = BatchOperationUtil.executeInTransaction(conn, () -> {
                try (PreparedStatement pstmt = conn.prepareStatement(INSERT_PRODUCT_SQL)) {
                    pstmt.setString(1, "事务内商品");
                    pstmt.setBigDecimal(2, new BigDecimal("20.00"));
                    pstmt.setInt(3, 2);
                    pstmt.executeUpdate();
                }
                return "事务内商品";
            });

            assertEquals("事务内商品", insertedName);
            assertFalse(conn.getAutoCommit());
            assertEquals(1, countProducts(conn));

            conn.rollback();
        }

        assertEquals(0, countProducts());
    }

    private int countProducts() throws SQLException {
        try (Connection conn = getTestConnection()) {
            return countProducts(conn);
        }
    }

    private int countProducts(Connection conn) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM products");
             ResultSet rs = pstmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
