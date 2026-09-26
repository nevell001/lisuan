package com.cashier.dao;

import com.cashier.model.ReturnOrderItem;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReturnOrderItemDAOTest extends DatabaseTestBase {

    private final ReturnOrderItemDAORefactored returnOrderItemDAO =
        DAOFactory.getInstance().getReturnOrderItemDAO();

    @BeforeEach
    void setUp() throws Exception {
        if (!DatabaseTestBase.isInitialized()) {
            DatabaseTestBase.initTestDatabase();
        }
        clearTestData();
    }

    @Test
    @DisplayName("按退货单号查询明细应正确读取商品状态")
    void findByReturnOrderIdShouldReadCondition() throws Exception {
        // 明细表 return_order_id 有外键（TD-009）：先造退货单
        executeSql("INSERT INTO return_orders (return_order_id, return_date, total_amount, status, operator_name) "
            + "VALUES ('R202606170001', CURRENT_TIMESTAMP, 0, 'APPROVED', 'test')");

        ReturnOrderItem item = new ReturnOrderItem();
        item.returnOrderId = "R202606170001";
        item.productId = 1;
        item.productCode = "P202606170001";
        item.productName = "测试退货商品";
        item.barcode = "6900000000001";
        item.category = "默认分类";
        item.returnQuantity = 2;
        item.unitPrice = new BigDecimal("10.00");
        item.returnAmount = new BigDecimal("20.00");
        item.reason = "顾客退货";
        item.condition = "DAMAGED";

        returnOrderItemDAO.insert(item);

        List<ReturnOrderItem> items = returnOrderItemDAO.findByReturnOrderId(item.returnOrderId);

        assertFalse(items.isEmpty());
        assertEquals("DAMAGED", items.get(0).condition);
    }
}
