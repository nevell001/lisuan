package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.model.ReturnOrder;
import com.cashier.model.ReturnOrderItem;
import com.cashier.util.DatabaseManager;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 退货占用台账（F10-a）行为测试。
 *
 * <p>背景：建单校验此前在 UI 线程、事务外（"已退量 + 本次 ≤ 原销量"），两个终端同时提交时
 * 各自都能通过 → 两张退货单都入库 → 库存恢复两次、退款两次（不可逆）。现在校验在**建单事务内**：
 * 先锁原交易行串行化，再用 {@code return_reservations} 台账重算可退余量。</p>
 */
@DisplayName("退货占用台账（F10）并发与口径测试")
class ReturnReservationConcurrencyTest extends DatabaseTestBase {

    private static final String TX = "TX-F10-0001";
    private static final String TX_LEGACY = "TX-F10-LEGACY";
    private static final int PRODUCT_A = 101;
    private static final int PRODUCT_B = 102;

    @Test
    @DisplayName("并发满额退货：只有一个终端建单成功，另一个被可退余量拦下")
    void concurrentFullReturnsOnlyOneSucceeds() throws Exception {
        insertTransactionWithItems(TX, new int[][]{{PRODUCT_A, 2}, {PRODUCT_B, 3}});

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            futures.add(pool.submit(returnTask(2, 3, start)));
            futures.add(pool.submit(returnTask(2, 3, start)));
            start.countDown();

            int success = 0;
            int rejected = 0;
            for (Future<Boolean> future : futures) {
                try {
                    if (future.get(30, TimeUnit.SECONDS)) {
                        success++;
                    }
                } catch (ExecutionException e) {
                    assertTrue(e.getCause() instanceof ReturnService.ReturnQuantityExceededException,
                        "输掉的那次必须是可退余量不足，而不是别的异常: " + e.getCause());
                    rejected++;
                }
            }

            assertEquals(1, success, "同一交易并发满额退货必须只有一个成功（否则库存与钱会被退两次）");
            assertEquals(1, rejected, "另一个终端必须拿到「可退余量不足」的明确原因");
            assertEquals(2, reservationRowCount(TX), "台账只应留下成功那一张单的两行占用");
            assertEquals(2, reservedQuantity(TX, PRODUCT_A));
            assertEquals(3, reservedQuantity(TX, PRODUCT_B));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("部分退货累计不得超过原销量（可退余量递减）")
    void partialReturnsCannotExceedSoldQuantity() {
        insertTransactionWithItems(TX, new int[][]{{PRODUCT_A, 2}});

        assertTrue(ReturnService.createReturnOrder(order(), items(1, 0)), "第一次退 1 件应成功");
        assertTrue(ReturnService.createReturnOrder(order(), items(1, 0)), "第二次退 1 件应成功（累计 = 原销量）");
        assertThrows(ReturnService.ReturnQuantityExceededException.class,
            () -> ReturnService.createReturnOrder(order(), items(1, 0)), "第三次退 1 件应被余量校验拦下");
        assertEquals(2, reservedQuantity(TX, PRODUCT_A));
    }

    @Test
    @DisplayName("已驳回的退货单立刻释放占用（额度可再次使用）")
    void rejectedReturnOrderReleasesReservation() throws Exception {
        insertTransactionWithItems(TX, new int[][]{{PRODUCT_A, 2}});

        ReturnOrder first = order();
        assertTrue(ReturnService.createReturnOrder(first, items(2, 0)), "首次满额退货应成功");
        assertThrows(ReturnService.ReturnQuantityExceededException.class,
            () -> ReturnService.createReturnOrder(order(), items(2, 0)), "已占满时不能再建单");

        try (Connection conn = getTestConnection()) {
            assertTrue(DAOFactory.getInstance().getReturnOrderDAO()
                    .markApprovalWithConnection(conn, first.returnOrderId, "REJECTED", "admin", "驳回"),
                "驳回状态迁移应成功");
        }

        assertTrue(ReturnService.createReturnOrder(order(), items(2, 0)), "驳回后应释放额度、可重新建单");
        assertEquals(2, reservedQuantity(TX, PRODUCT_A), "统计仍只算未驳回的那张单");
    }

    @Test
    @DisplayName("同一商品在交易里出现多行时按跨行合计校验（旧实现按行比较会误判）")
    void quantitiesAggregateAcrossTransactionLines() {
        insertTransactionWithItems(TX, new int[][]{{PRODUCT_A, 1}, {PRODUCT_A, 1}});

        assertTrue(ReturnService.createReturnOrder(order(), items(2, 0)), "两行合计 2 件，退 2 件应成功");
        assertThrows(ReturnService.ReturnQuantityExceededException.class,
            () -> ReturnService.createReturnOrder(order(), items(1, 0)), "再退 1 件应被拦下（合计已满）");
    }

    @Test
    @DisplayName("回填前：台账里没有老退货单，余量校验确实拦不住（回填要解决的问题）")
    void legacyReturnOrderIsNotCountedBeforeBackfill() {
        insertTransactionWithItems(TX_LEGACY, new int[][]{{PRODUCT_A, 2}});
        insertLegacyReturnOrder("R-LEGACY-0001", TX_LEGACY, PRODUCT_A, 2);

        assertEquals(0, reservationRowCount(TX_LEGACY), "台账表建好之前的老退货单没有台账行");
        assertTrue(ReturnService.createReturnOrder(orderFor(TX_LEGACY), items(2, 0)),
            "没有台账行就无法扣减余量——这正是必须回填的原因");
    }

    @Test
    @DisplayName("老库回填：既有退货单占用被补进台账，且回填幂等")
    void backfillMakesLegacyReturnsOccupy() {
        insertTransactionWithItems(TX_LEGACY, new int[][]{{PRODUCT_A, 2}});
        insertLegacyReturnOrder("R-LEGACY-0001", TX_LEGACY, PRODUCT_A, 2);
        assertEquals(0, reservationRowCount(TX_LEGACY), "回填前台账为空");

        assertEquals(1, DatabaseManager.backfillReturnReservations(), "首次回填应写入 1 行");
        assertEquals(1, reservationRowCount(TX_LEGACY));
        assertEquals(0, DatabaseManager.backfillReturnReservations(), "重复回填必须幂等（不重复插入）");

        assertThrows(ReturnService.ReturnQuantityExceededException.class,
            () -> ReturnService.createReturnOrder(orderFor(TX_LEGACY), items(2, 0)),
            "老退货单已被回填占用，不能再退满额");
    }

    @Test
    @DisplayName("原交易没有明细行时跳过余量校验但仍记台账（老/测试数据不阻塞）")
    void missingItemRowsSkipValidationButStillRecordLedger() {
        insertTransactionWithItems(TX_LEGACY, new int[0][]);

        assertTrue(ReturnService.createReturnOrder(orderFor(TX_LEGACY), items(5, 0)),
            "无从得知可退基数时应放行（这条兜底只影响脏数据，且会留 WARN）");
        assertEquals(1, reservationRowCount(TX_LEGACY), "放行也要记台账，便于事后审计");
    }

    @Test
    @DisplayName("审批 / 完成会同步台账行状态（F10-b）")
    void ledgerStatusFollowsApprovalAndCompletion() throws Exception {
        insertTransactionWithItems(TX, new int[][]{{PRODUCT_A, 2}});

        ReturnOrder order = order();
        assertTrue(ReturnService.createReturnOrder(order, items(2, 0)));
        assertEquals("PENDING", reservationStatus(order.returnOrderId));

        assertTrue(ReturnService.approveReturnOrder(order.returnOrderId, "admin", "同意", true));
        assertEquals("APPROVED", reservationStatus(order.returnOrderId));

        assertTrue(ReturnService.completeReturnOrder(order.returnOrderId));
        assertEquals("COMPLETED", reservationStatus(order.returnOrderId));
    }

    @Test
    @DisplayName("驳回会同步台账为 REJECTED（占用统计随即归零）")
    void ledgerStatusFollowsRejection() throws Exception {
        insertTransactionWithItems(TX, new int[][]{{PRODUCT_A, 2}});

        ReturnOrder order = order();
        assertTrue(ReturnService.createReturnOrder(order, items(2, 0)));
        assertTrue(ReturnService.approveReturnOrder(order.returnOrderId, "admin", "拒绝", false));

        assertEquals("REJECTED", reservationStatus(order.returnOrderId));
        assertEquals(0, reservedQuantity(TX, PRODUCT_A), "驳回后占用统计必须归零");
    }

    @Test
    @DisplayName("超量建单的提示由服务层用 i18n 组装（含商品名与数量），界面不必自己拼")
    void exceededQuantityMessageIsBuiltByService() {
        insertTransactionWithItems(TX, new int[][]{{PRODUCT_A, 1}});

        ReturnService.ReturnQuantityExceededException e = assertThrows(
            ReturnService.ReturnQuantityExceededException.class,
            () -> ReturnService.createReturnOrder(order(), items(3, 0)));

        assertTrue(e.getMessage() != null && e.getMessage().contains("商品" + PRODUCT_A),
            "提示应包含商品名（证明服务层查了商品）: " + e.getMessage());
        assertTrue(e.getMessage().contains("3"), "提示应包含本次退货数量: " + e.getMessage());
    }

    // ===== 辅助 =====

    private Callable<Boolean> returnTask(int qtyA, int qtyB, CountDownLatch start) {
        return () -> {
            start.await();
            return ReturnService.createReturnOrder(order(), items(qtyA, qtyB));
        };
    }

    private ReturnOrder order() {
        return orderFor(TX);
    }

    private ReturnOrder orderFor(String transactionId) {
        ReturnOrder order = new ReturnOrder();
        order.originalTransactionId = transactionId;
        order.returnDate = Instant.now();
        order.totalAmount = new BigDecimal("50.00");
        order.paymentMethod = "CASH";
        order.operatorName = "tester";
        return order;
    }

    private List<ReturnOrderItem> items(int qtyA, int qtyB) {
        List<ReturnOrderItem> items = new ArrayList<>();
        if (qtyA > 0) {
            items.add(item(PRODUCT_A, qtyA));
        }
        if (qtyB > 0) {
            items.add(item(PRODUCT_B, qtyB));
        }
        return items;
    }

    private ReturnOrderItem item(int productId, int quantity) {
        ReturnOrderItem item = new ReturnOrderItem();
        item.productId = productId;
        item.productName = "商品" + productId;
        item.returnQuantity = quantity;
        item.unitPrice = new BigDecimal("10.00");
        item.returnAmount = new BigDecimal("10.00").multiply(BigDecimal.valueOf(quantity));
        item.reason = "测试";
        item.condition = "GOOD";
        return item;
    }

    private void insertTransactionWithItems(String transactionId, int[][] productQuantities) {
        try (Connection conn = getTestConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO transactions (transaction_id, timestamp, total_amount, tax, final_amount, " +
                "payment_method, operator_name, status) VALUES ('" + transactionId + "', '2026-10-08 10:00:00', " +
                "100.00, 0, 100.00, '现金', 'tester', 'NORMAL')");
            java.util.Set<Integer> productsInserted = new java.util.HashSet<>();
            for (int[] pair : productQuantities) {
                if (productsInserted.add(pair[0])) {
                    // 商品行存在时超量提示才能显示商品名（服务层会查一次商品）
                    stmt.execute("INSERT INTO products (id, product_code, name, price, quantity) VALUES (" + pair[0]
                        + ", 'P" + pair[0] + "', '商品" + pair[0] + "', 10.00, 100)");
                }
                stmt.execute("INSERT INTO transaction_items (transaction_id, product_id, product_code, product_name, " +
                    "price, quantity, subtotal) VALUES ('" + transactionId + "', " + pair[0] + ", 'P" + pair[0] + "', " +
                    "'商品" + pair[0] + "', 10.00, " + pair[1] + ", " + (10 * pair[1]) + ".00)");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("准备交易数据失败", e);
        }
    }

    /** 模拟"台账表建好之前就已存在"的退货单：只写 return_orders + return_order_items。 */
    private void insertLegacyReturnOrder(String returnOrderId, String transactionId, int productId, int quantity) {
        try (Connection conn = getTestConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO return_orders (return_order_id, original_transaction_id, return_date, " +
                "total_amount, status, operator_name) VALUES ('" + returnOrderId + "', '" + transactionId + "', '" +
                Timestamp.from(Instant.now()) + "', 20.00, 'PENDING', 'tester')");
            stmt.execute("INSERT INTO return_order_items (return_order_id, product_id, product_name, return_quantity, " +
                "unit_price, return_amount) VALUES ('" + returnOrderId + "', " + productId + ", '商品" + productId +
                "', " + quantity + ", 10.00, " + (10 * quantity) + ".00)");
        } catch (SQLException e) {
            throw new IllegalStateException("准备老退货单失败", e);
        }
    }

    private int reservationRowCount(String transactionId) {
        return queryInt("SELECT COUNT(*) FROM return_reservations WHERE original_transaction_id = '"
            + transactionId + "'");
    }

    /** 台账行状态（同单据只有一行商品时即该行）。 */
    private String reservationStatus(String returnOrderId) {
        return queryString("SELECT status FROM return_reservations WHERE return_order_id = '"
            + returnOrderId + "' LIMIT 1");
    }

    private int reservedQuantity(String transactionId, int productId) {
        return queryInt("SELECT COALESCE(SUM(rr.quantity), 0) FROM return_reservations rr " +
            "JOIN return_orders ro ON rr.return_order_id = ro.return_order_id " +
            "WHERE rr.original_transaction_id = '" + transactionId + "' AND rr.product_id = " + productId +
            " AND ro.status <> 'REJECTED'");
    }

    private int queryInt(String sql) {
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("查询失败: " + sql, e);
        }
    }

    private String queryString(String sql) {
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        } catch (SQLException e) {
            throw new IllegalStateException("查询失败: " + sql, e);
        }
    }
}
