package com.cashier.service;

import com.cashier.model.PurchaseInbound;
import com.cashier.model.PurchaseInboundItem;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseServiceTest extends DatabaseTestBase {

    @Test
    @DisplayName("采购审批同时更新订单并保存审批记录")
    void approveOrderCommitsOrderAndApprovalTogether() throws Exception {
        int orderId = createOrder("PO-APPROVE-1", "pending");

        PurchaseService.approveOrder(orderId, "admin", "approve", "同意采购");

        assertEquals("approved", queryString("SELECT status FROM purchase_orders WHERE id = ?", orderId));
        assertEquals(1, queryInt("SELECT COUNT(*) FROM purchase_approvals WHERE order_id = ?", orderId));
    }

    @Test
    @DisplayName("已审批订单不能重复审批")
    void duplicateApprovalIsRejectedWithoutExtraRecord() throws Exception {
        int orderId = createOrder("PO-APPROVE-2", "pending");
        PurchaseService.approveOrder(orderId, "admin", "approve", "首次审批");

        assertThrows(SQLException.class,
            () -> PurchaseService.approveOrder(orderId, "admin", "reject", "重复审批"));

        assertEquals("approved", queryString("SELECT status FROM purchase_orders WHERE id = ?", orderId));
        assertEquals(1, queryInt("SELECT COUNT(*) FROM purchase_approvals WHERE order_id = ?", orderId));
    }

    @Test
    @DisplayName("采购入库原子更新入库单、明细、采购数量、库存和订单状态")
    void inboundCommitsAllRelatedChanges() throws Exception {
        int productId = createProduct("采购商品A", 10);
        int orderId = createOrder("PO-INBOUND-1", "approved");
        int orderItemId = createOrderItem(orderId, productId, "采购商品A", 5);

        PurchaseInbound inbound = createInbound("IB-1", orderId, 5);
        PurchaseInboundItem item = createInboundItem(orderItemId, productId, "采购商品A", 5);
        PurchaseService.receiveInbound(inbound, List.of(item));

        assertEquals(1, queryInt("SELECT COUNT(*) FROM purchase_inbound WHERE id = ?", inbound.id));
        assertEquals(5, queryInt("SELECT inbound_quantity FROM purchase_order_items WHERE id = ?", orderItemId));
        assertEquals(15, queryInt("SELECT quantity FROM products WHERE id = ?", productId));
        assertEquals("completed", queryString("SELECT status FROM purchase_orders WHERE id = ?", orderId));
    }

    @Test
    @DisplayName("任一商品库存更新失败时整张入库单回滚")
    void inboundRollsBackEveryChangeWhenOneItemFails() throws Exception {
        int productId = createProduct("采购商品B", 10);
        int secondProductId = createProduct("采购商品C", 10);
        int orderId = createOrder("PO-INBOUND-2", "approved");
        int firstOrderItemId = createOrderItem(orderId, productId, "采购商品B", 3);
        // 第二个明细只采购了 2 件，却要入库 5 件 → increaseInboundQuantity 中途失败，触发整单回滚。
        // （此前这里用一个不存在的 product_id 制造失败；补上外键后这种"悬空引用"已不可能，
        //   改用业务上真实存在的失败条件，回滚断言不变。）
        int overInboundOrderItemId = createOrderItem(orderId, secondProductId, "采购商品C", 2);

        PurchaseInbound inbound = createInbound("IB-2", orderId, 5);
        List<PurchaseInboundItem> items = List.of(
            createInboundItem(firstOrderItemId, productId, "采购商品B", 3),
            createInboundItem(overInboundOrderItemId, secondProductId, "采购商品C", 5)
        );

        assertThrows(SQLException.class, () -> PurchaseService.receiveInbound(inbound, items));

        assertEquals(0, queryInt("SELECT COUNT(*) FROM purchase_inbound WHERE inbound_no = ?", "IB-2"));
        assertEquals(0, queryInt("SELECT inbound_quantity FROM purchase_order_items WHERE id = ?", firstOrderItemId));
        assertEquals(10, queryInt("SELECT quantity FROM products WHERE id = ?", productId));
        assertEquals("approved", queryString("SELECT status FROM purchase_orders WHERE id = ?", orderId));
    }

    private int createProduct(String name, int quantity) throws SQLException {
        return insertAndGetId("INSERT INTO products (product_code, name, price, quantity, cost) VALUES (?, ?, ?, ?, ?)",
            "P-" + name, name, BigDecimal.TEN, quantity, BigDecimal.valueOf(5));
    }

    private int createOrder(String orderNo, String status) throws SQLException {
        int supplierId = insertAndGetId("INSERT INTO suppliers (supplier_code, name) VALUES (?, ?)",
            "S-" + orderNo, "测试供应商-" + orderNo);
        return insertAndGetId("INSERT INTO purchase_orders " +
                "(order_no, supplier_id, purchase_date, total_amount, status, purchaser) VALUES (?, ?, CURRENT_DATE, ?, ?, ?)",
            orderNo, supplierId, BigDecimal.valueOf(50), status, "buyer");
    }

    private int createOrderItem(int orderId, int productId, String productName, int quantity) throws SQLException {
        return insertAndGetId("INSERT INTO purchase_order_items " +
                "(order_id, product_id, product_name, quantity, unit_price, total_price, inbound_quantity) " +
                "VALUES (?, ?, ?, ?, ?, ?, 0)",
            orderId, productId, productName, quantity, BigDecimal.TEN,
            BigDecimal.TEN.multiply(BigDecimal.valueOf(quantity)));
    }

    private PurchaseInbound createInbound(String inboundNo, int orderId, int quantity) {
        PurchaseInbound inbound = new PurchaseInbound(orderId, "", "2026-06-21", "admin");
        inbound.inboundNo = inboundNo;
        inbound.totalQuantity = quantity;
        inbound.totalAmount = BigDecimal.TEN.multiply(BigDecimal.valueOf(quantity));
        return inbound;
    }

    private PurchaseInboundItem createInboundItem(int orderItemId, int productId, String name, int quantity) {
        return new PurchaseInboundItem(0, orderItemId, productId, name, quantity, BigDecimal.TEN);
    }

    private int insertAndGetId(String sql, Object... values) throws SQLException {
        try (Connection conn = getTestConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setValues(pstmt, values);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("未返回新增记录 ID");
                }
                return rs.getInt(1);
            }
        }
    }

    private int queryInt(String sql, Object value) throws SQLException {
        try (Connection conn = getTestConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, value);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private String queryString(String sql, Object value) throws SQLException {
        try (Connection conn = getTestConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, value);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private void setValues(PreparedStatement pstmt, Object[] values) throws SQLException {
        for (int index = 0; index < values.length; index++) {
            pstmt.setObject(index + 1, values[index]);
        }
    }
}
