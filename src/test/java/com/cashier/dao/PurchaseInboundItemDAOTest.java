package com.cashier.dao;

import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("采购入库明细数据访问对象测试")
class PurchaseInboundItemDAOTest extends DatabaseTestBase {

    private final PurchaseInboundItemDAORefactored inboundItemDAO =
        DAOFactory.getInstance().getPurchaseInboundItemDAO();

    @Test
    @DisplayName("按商品聚合加权平均入库成本")
    void testFindAverageUnitCostByProductId() throws SQLException {
        // 入库明细的 inbound_id / order_item_id / product_id 都有外键（TD-009）：先造整条父链
        executeSql("INSERT INTO suppliers (id, supplier_code, name) VALUES (1, 'S-FK-1', '外键供应商')");
        executeSql("INSERT INTO purchase_orders (id, order_no, supplier_id, purchase_date, total_amount, status) "
            + "VALUES (1, 'PO-FK-1', 1, CURRENT_DATE, 0, 'approved')");
        executeSql("INSERT INTO purchase_inbound (id, inbound_no, order_id, inbound_date) "
            + "VALUES (1, 'IB-FK-1', 1, CURRENT_DATE)");
        for (int productId = 1; productId <= 3; productId++) {
            executeSql("INSERT INTO products (id, product_code, name, price, quantity, cost) VALUES (?, ?, ?, 10, 1, 5)",
                productId, "P-FK-" + productId, "外键商品" + productId);
            executeSql("INSERT INTO purchase_order_items (id, order_id, product_id, product_name, quantity, unit_price, total_price) "
                + "VALUES (?, 1, ?, ?, 10, 10, 100)", productId, productId, "外键商品" + productId);
        }

        insertInboundItem(1, 1, 10, BigDecimal.valueOf(8));
        insertInboundItem(1, 1, 30, BigDecimal.valueOf(12));
        insertInboundItem(1, 2, 5, BigDecimal.valueOf(20));
        insertInboundItem(1, 3, 0, BigDecimal.valueOf(99));

        var averageCosts = inboundItemDAO.findAverageUnitCostByProductId();

        assertEquals(0, BigDecimal.valueOf(11).compareTo(averageCosts.get(1)));
        assertEquals(0, BigDecimal.valueOf(20).compareTo(averageCosts.get(2)));
        assertFalse(averageCosts.containsKey(3));
    }

    private void insertInboundItem(int inboundId, int productId, int quantity, BigDecimal unitPrice) throws SQLException {
        try (Connection conn = getTestConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO purchase_inbound_items " +
                 "(inbound_id, order_item_id, product_id, quantity, unit_price, total_price) " +
                 "VALUES (?, ?, ?, ?, ?, ?)")) {

            pstmt.setInt(1, inboundId);
            pstmt.setInt(2, productId);
            pstmt.setInt(3, productId);
            pstmt.setInt(4, quantity);
            pstmt.setBigDecimal(5, unitPrice);
            pstmt.setBigDecimal(6, unitPrice.multiply(BigDecimal.valueOf(quantity)));
            pstmt.executeUpdate();
        }
    }
}
