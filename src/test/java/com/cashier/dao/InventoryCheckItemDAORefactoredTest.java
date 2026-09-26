package com.cashier.dao;

import com.cashier.model.InventoryCheckItem;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("库存盘点明细数据访问对象测试")
class InventoryCheckItemDAORefactoredTest extends DatabaseTestBase {

    private final InventoryCheckItemDAORefactored itemDAO =
        DAOFactory.getInstance().getInventoryCheckItemDAO();

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
    }

    @AfterEach
    void cleanup() throws SQLException {
        clearTestData();
    }

    @Test
    @DisplayName("明细插入后可查、可更新、可删除")
    void itemCrudLifecycle() throws SQLException {
        // 明细表有外键（TD-009）：先造父行——盘点单 1 与商品 10
        executeSql("INSERT INTO inventory_check (id, check_no, check_date, status) VALUES (1, 'IC-FK-1', CURRENT_DATE, 'checking')");
        executeSql("INSERT INTO products (id, product_code, name, price, quantity, cost) VALUES (10, 'P-FK-10', '盘点测试商品', 10, 5, 5)");

        InventoryCheckItem item = new InventoryCheckItem();
        item.checkId = 1;
        item.productId = 10;
        item.productName = "测试商品";
        item.bookQuantity = 5;
        item.actualQuantity = 3;
        item.diffQuantity = -2;
        item.diffReason = "破损";

        assertTrue(itemDAO.insert(item));
        assertTrue(item.id > 0);

        List<InventoryCheckItem> items = itemDAO.findByCheckId(1);
        assertEquals(1, items.size());
        assertEquals("测试商品", items.get(0).productName);
        assertEquals(-2, items.get(0).diffQuantity);

        items.get(0).actualQuantity = 7;
        items.get(0).diffQuantity = 2;
        assertTrue(itemDAO.update(items.get(0)));

        InventoryCheckItem updated = itemDAO.findById(item.id);
        assertEquals(7, updated.actualQuantity);
        assertEquals(2, updated.diffQuantity);

        assertTrue(itemDAO.deleteByCheckId(1));
        assertTrue(itemDAO.findByCheckId(1).isEmpty());
    }
}
