package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.model.ReturnOrder;
import com.cashier.model.ReturnOrderItem;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 退货**行级**可退量校验（F10-c）。
 *
 * <p>F10-a 的商品级台账能挡住"同一商品退超总销量"，但同一商品在同一笔交易里可能出现多行
 * （不同单价/促销）。只按商品合计校验时，行归属靠调用方自报：1×20.00 与 1×10.00 两行，
 * 商品级看到"2 ≤ 2"就放行，而界面把这 2 件都算在 20.00 那一行 → 退 40.00（真实应退 30.00）。</p>
 *
 * <p>因此在商品级校验之外，再按 {@code return_order_items.transaction_item_id} 做一次行级校验：
 * {@code 该行已退 + 本次 ≤ 该行原数量}（已驳回的退货单不占）。没有行 id 的老数据/脏数据不参与
 * 行级校验，保持商品级校验的原有行为。</p>
 */
@DisplayName("退货行级可退量校验（F10-c）")
class ReturnLineLevelValidationTest extends DatabaseTestBase {

    private static final String TX = "TX-F10C-0001";
    private static final int PRODUCT_A = 201;

    @Test
    @DisplayName("同一商品两行：把 2 件都算在只卖了 1 件的那一行必须被拦下（否则按更贵的行多退）")
    void returningMoreThanOneLineSoldIsRejected() throws Exception {
        int expensiveLine = insertTwoLinesOfSameProduct();
        int cheaperLine = otherLine(expensiveLine);

        // 商品级：合计 2 ≤ 原销量 2，会放行；行级：该行只卖了 1 件 → 必须拦下
        ReturnService.ReturnQuantityExceededException e = assertThrows(
            ReturnService.ReturnQuantityExceededException.class,
            () -> ReturnService.createReturnOrder(order(), List.of(item(expensiveLine, PRODUCT_A, 2))),
            "把超出该行销量的数量算到这一行会多退钱，必须拦下");
        assertTrue(e.getMessage() != null && e.getMessage().contains("商品" + PRODUCT_A),
            "提示应带商品名: " + e.getMessage());

        // 各行退各自的 1 件则合法
        assertTrue(ReturnService.createReturnOrder(order(), List.of(
            item(expensiveLine, PRODUCT_A, 1), item(cheaperLine, PRODUCT_A, 1))),
            "两行各退 1 件（合计 = 原销量）应当允许");
    }

    @Test
    @DisplayName("同一行不能重复退：第一次退掉后再退同一行必须被拦下")
    void oneLineCannotBeReturnedTwice() throws Exception {
        int line = insertTwoLinesOfSameProduct();

        assertTrue(ReturnService.createReturnOrder(order(), List.of(item(line, PRODUCT_A, 1))));
        assertThrows(ReturnService.ReturnQuantityExceededException.class,
            () -> ReturnService.createReturnOrder(order(), List.of(item(line, PRODUCT_A, 1))),
            "这一行只卖了 1 件，不能再退一次");
    }

    @Test
    @DisplayName("已驳回的退货单不占该行额度（行级口径与台账一致）")
    void rejectedReturnReleasesLineQuota() throws Exception {
        int line = insertTwoLinesOfSameProduct();

        ReturnOrder first = order();
        assertTrue(ReturnService.createReturnOrder(first, List.of(item(line, PRODUCT_A, 1))));
        assertTrue(ReturnService.approveReturnOrder(first.returnOrderId, "admin", "拒绝", false));

        assertTrue(ReturnService.createReturnOrder(order(), List.of(item(line, PRODUCT_A, 1))),
            "驳回后该行额度应释放");
    }

    @Test
    @DisplayName("没有行 id 的明细退回商品级校验（老数据/脏数据行为不变）")
    void itemsWithoutLineIdFallBackToProductLevel() throws Exception {
        insertTwoLinesOfSameProduct();

        ReturnOrderItem noLine = item(null, PRODUCT_A, 2);
        assertTrue(ReturnService.createReturnOrder(order(), List.of(noLine)),
            "没有行信息时只能按商品级校验（合计 2 ≤ 2）");
        assertThrows(ReturnService.ReturnQuantityExceededException.class,
            () -> ReturnService.createReturnOrder(order(), List.of(item(null, PRODUCT_A, 1))),
            "商品级校验仍然生效：合计不能再超");
        assertEquals(0, lineIdsRecorded(TX), "行 id 缺失时应落 NULL（不能写成 0，否则行级校验会当成第 0 行）");
        assertEquals(0, rowsWithZeroLineId(TX), "更不允许退化成 0（0 会被行级校验当成第 0 行）");
    }

    // ===== 辅助 =====

    /** 造一笔"同一商品两行、各 1 件（20.00 / 10.00）"的交易，返回第一行的 id。 */
    private int insertTwoLinesOfSameProduct() {
        try (Connection conn = getTestConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM return_reservations");
            stmt.execute("DELETE FROM return_order_items");
            stmt.execute("DELETE FROM return_orders");
            stmt.execute("DELETE FROM transaction_items");
            stmt.execute("DELETE FROM transactions");
            stmt.execute("INSERT INTO products (id, product_code, name, price, quantity) VALUES (" + PRODUCT_A
                + ", 'P" + PRODUCT_A + "', '商品" + PRODUCT_A + "', 10.00, 100)");
            stmt.execute("INSERT INTO transactions (transaction_id, timestamp, total_amount, tax, final_amount, "
                + "payment_method, operator_name, status) VALUES ('" + TX + "', '2026-10-08 10:00:00', "
                + "30.00, 0, 30.00, '现金', 'tester', 'NORMAL')");
            stmt.execute("INSERT INTO transaction_items (transaction_id, product_id, product_code, product_name, "
                + "price, quantity, subtotal) VALUES ('" + TX + "', " + PRODUCT_A + ", 'P" + PRODUCT_A
                + "', '商品" + PRODUCT_A + "', 20.00, 1, 20.00)");
            stmt.execute("INSERT INTO transaction_items (transaction_id, product_id, product_code, product_name, "
                + "price, quantity, subtotal) VALUES ('" + TX + "', " + PRODUCT_A + ", 'P" + PRODUCT_A
                + "', '商品" + PRODUCT_A + "', 10.00, 1, 10.00)");
            return firstLineId();
        } catch (SQLException e) {
            throw new IllegalStateException("准备交易数据失败", e);
        }
    }

    private int firstLineId() {
        return queryInt("SELECT MIN(id) FROM transaction_items WHERE transaction_id = '" + TX + "'");
    }

    private int otherLine(int firstLineId) {
        return queryInt("SELECT MAX(id) FROM transaction_items WHERE transaction_id = '" + TX + "'");
    }

    private ReturnOrder order() {
        ReturnOrder order = new ReturnOrder();
        order.originalTransactionId = TX;
        order.returnDate = Instant.now();
        order.totalAmount = new BigDecimal("10.00");
        order.paymentMethod = "CASH";
        order.operatorName = "tester";
        return order;
    }

    private ReturnOrderItem item(Integer transactionItemId, int productId, int quantity) {
        ReturnOrderItem item = new ReturnOrderItem();
        item.productId = productId;
        item.transactionItemId = transactionItemId;
        item.productName = "商品" + productId;
        item.productCode = "P" + productId;
        item.returnQuantity = quantity;
        item.unitPrice = new BigDecimal("10.00");
        item.returnAmount = item.unitPrice.multiply(BigDecimal.valueOf(quantity));
        item.reason = "测试";
        item.condition = "GOOD";
        return item;
    }

    private int lineIdsRecorded(String transactionId) {
        return queryInt("SELECT COUNT(*) FROM return_order_items roi JOIN return_orders ro "
            + "ON roi.return_order_id = ro.return_order_id WHERE ro.original_transaction_id = '" + transactionId
            + "' AND roi.transaction_item_id IS NOT NULL");
    }

    private int rowsWithZeroLineId(String transactionId) {
        return queryInt("SELECT COUNT(*) FROM return_order_items roi JOIN return_orders ro "
            + "ON roi.return_order_id = ro.return_order_id WHERE ro.original_transaction_id = '" + transactionId
            + "' AND roi.transaction_item_id = 0");
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
}