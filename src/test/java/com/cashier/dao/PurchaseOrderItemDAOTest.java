package com.cashier.dao;

import com.cashier.model.PurchaseOrderItem;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseOrderItemDAOTest extends DatabaseTestBase {

    private final PurchaseOrderItemDAORefactored purchaseOrderItemDAO = DAOFactory.getInstance().getPurchaseOrderItemDAO();

    /** 明细表的 order_id / product_id 都有外键（TD-009），先造父行。 */
    private void seedParents(int orderId, int productId) throws SQLException {
        executeSql("INSERT INTO suppliers (id, supplier_code, name) VALUES (1, 'S-FK-1', '外键供应商')");
        executeSql("INSERT INTO purchase_orders (id, order_no, supplier_id, purchase_date, total_amount, status) "
            + "VALUES (?, ?, 1, CURRENT_DATE, 0, 'pending')", orderId, "PO-FK-" + orderId);
        executeSql("INSERT INTO products (id, product_code, name, price, quantity, cost) VALUES (?, ?, ?, 10, 1, 5)",
            productId, "P-FK-" + productId, "外键商品" + productId);
    }

    @Test
    @DisplayName("插入并按订单查询明细")
    void insertAndFindByOrder() throws Exception {
        seedParents(1, 1);
        PurchaseOrderItem item = new PurchaseOrderItem(1, 1, "测试商品", 2, BigDecimal.valueOf(10));
        item.totalPrice = BigDecimal.valueOf(20);

        assertTrue(purchaseOrderItemDAO.insert(item));
        assertTrue(item.id > 0);
        assertEquals(1, purchaseOrderItemDAO.findByOrderId(1).size());
        assertEquals(1, purchaseOrderItemDAO.findByOrder(1).size());
    }

    @Test
    @DisplayName("按商品与订单组合查询")
    void findByOrderAndProduct() throws Exception {
        seedParents(2, 7);
        PurchaseOrderItem item = new PurchaseOrderItem(2, 7, "商品B", 1, BigDecimal.valueOf(5));
        item.totalPrice = BigDecimal.valueOf(5);
        purchaseOrderItemDAO.insert(item);

        assertTrue(purchaseOrderItemDAO.findByOrderAndProduct(2, 7) != null);
        assertEquals(1, purchaseOrderItemDAO.findByProductId(7).size());
    }
}
