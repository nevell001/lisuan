package com.cashier.dao;

import com.cashier.model.PurchaseOrder;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseOrderDAOTest extends DatabaseTestBase {

    private final PurchaseOrderDAORefactored purchaseOrderDAO = DAOFactory.getInstance().getPurchaseOrderDAO();

    private PurchaseOrder insertOrder(String orderNo) throws Exception {
        // supplier_id 有外键（TD-009）：每个用例先保证供应商 1 存在
        executeSql("INSERT INTO suppliers (id, supplier_code, name) VALUES (1, 'S-FK-1', '外键供应商')");

        PurchaseOrder order = new PurchaseOrder();
        order.orderNo = orderNo;
        order.supplierId = 1;
        order.purchaseDate = "2026-08-22";
        order.totalAmount = BigDecimal.valueOf(100);
        order.status = "pending";
        order.purchaser = "采购员";
        assertTrue(purchaseOrderDAO.insert(order));
        return order;
    }

    @Test
    @DisplayName("插入并按单号/ID查询")
    void insertAndFind() throws Exception {
        PurchaseOrder order = insertOrder("PO-DAO-001");

        assertTrue(order.id > 0);
        assertNotNull(purchaseOrderDAO.findById(order.id));
        assertEquals("PO-DAO-001", purchaseOrderDAO.findByOrderNo("PO-DAO-001").orderNo);
    }

    @Test
    @DisplayName("按状态与供应商查询")
    void findByStatusAndSupplier() throws Exception {
        insertOrder("PO-DAO-002");

        assertEquals(1, purchaseOrderDAO.findByStatus("pending").size());
        assertEquals(1, purchaseOrderDAO.findBySupplier(1).size());
    }

    @Test
    @DisplayName("更新状态")
    void updateStatus() throws Exception {
        PurchaseOrder order = insertOrder("PO-DAO-003");

        assertTrue(purchaseOrderDAO.updateStatus(order.id, "approved"));
        assertEquals("approved", purchaseOrderDAO.findById(order.id).status);
    }

    @Test
    @DisplayName("不存在的单号返回null")
    void findMissingReturnsNull() throws Exception {
        assertNull(purchaseOrderDAO.findByOrderNo("PO-MISSING"));
    }
}
