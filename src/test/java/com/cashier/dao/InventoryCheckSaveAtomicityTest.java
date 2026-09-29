package com.cashier.dao;

import com.cashier.model.InventoryCheck;
import com.cashier.model.InventoryCheckItem;
import com.cashier.model.Product;
import com.cashier.util.DatabaseManager;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 盘点单保存（表头 + 明细重建）的原子性。
 *
 * <p>编辑盘点单时会先删掉全部旧明细再逐条插入。修复前每步各用一条 autocommit 连接，
 * 中途失败会留下"已经提交的 DELETE + 半截明细"——表头仍写着 N 条，差额从此算错，
 * 而且没有任何错误提示能告诉用户明细已经没了。现在整段走一个事务，
 * 本测试验证 DAO 的 {@code *WithConnection} 变体确实参与调用方的事务（回滚能救回旧明细）。</p>
 */
@DisplayName("盘点单保存原子性")
class InventoryCheckSaveAtomicityTest extends DatabaseTestBase {

    private final InventoryCheckDAORefactored checkDAO = DAOFactory.getInstance().getInventoryCheckDAO();
    private final InventoryCheckItemDAORefactored itemDAO = DAOFactory.getInstance().getInventoryCheckItemDAO();
    private final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
    }

    @AfterEach
    void cleanup() throws SQLException {
        clearTestData();
    }

    @Test
    @DisplayName("保存中途失败时整单回滚：旧明细不会被删掉")
    void failedSaveKeepsExistingItems() throws SQLException {
        InventoryCheck check = createCheck("IC202609290001");
        assertTrue(checkDAO.insert(check));

        int productA = createProduct("ICPROD001");
        int productB = createProduct("ICPROD002");
        int productC = createProduct("ICPROD003");

        insertItem(check.id, productA, "旧明细A", 10, 10);
        insertItem(check.id, productB, "旧明细B", 20, 18);
        assertEquals(2, itemDAO.findByCheckId(check.id).size());

        // 模拟控制器里的"编辑保存"：改表头 → 删旧明细 → 插入新明细 → 中途失败
        assertThrows(SQLException.class, () -> DatabaseManager.executeBooleanTransaction(conn -> {
            InventoryCheck updated = createCheck("IC202609290001");
            updated.id = check.id;
            updated.remark = "改过的备注";
            assertTrue(checkDAO.updateWithConnection(conn, updated));
            itemDAO.deleteByCheckIdWithConnection(conn, check.id);
            insertItemWithConnection(conn, check.id, productC, "新明细C", 30, 30);
            throw new SQLException("模拟插入明细时失败");
        }));

        assertEquals(2, itemDAO.findByCheckId(check.id).size(),
            "事务回滚后旧明细必须还在：否则一次失败的保存就把盘点明细永久删掉了");
        assertEquals("", checkDAO.findById(check.id).remark,
            "表头改动也必须一起回滚");
    }

    @Test
    @DisplayName("保存成功时表头与明细一起提交，明细被整体替换")
    void successfulSaveReplacesItems() throws SQLException {
        InventoryCheck check = createCheck("IC202609290002");
        assertTrue(checkDAO.insert(check));
        int oldProduct = createProduct("ICPROD011");
        int newProduct1 = createProduct("ICPROD012");
        int newProduct2 = createProduct("ICPROD013");
        insertItem(check.id, oldProduct, "旧明细", 5, 5);

        assertTrue(DatabaseManager.executeBooleanTransaction(conn -> {
            InventoryCheck updated = createCheck("IC202609290002");
            updated.id = check.id;
            updated.remark = "已保存";
            assertTrue(checkDAO.updateWithConnection(conn, updated));
            itemDAO.deleteByCheckIdWithConnection(conn, check.id);
            insertItemWithConnection(conn, check.id, newProduct1, "新明细1", 6, 7);
            insertItemWithConnection(conn, check.id, newProduct2, "新明细2", 8, 8);
            return true;
        }));

        var items = itemDAO.findByCheckId(check.id);
        assertEquals(2, items.size());
        assertEquals("新明细1", items.get(0).productName);
        assertEquals("已保存", checkDAO.findById(check.id).remark);
    }

    private int createProduct(String productCode) throws SQLException {
        Product product = new Product();
        product.productCode = productCode;
        product.name = "盘点测试商品" + productCode;
        product.price = BigDecimal.valueOf(10);
        product.quantity = 100;
        product.category = "测试分类";
        product.unit = "个";
        product.minStock = 1;
        product.cost = BigDecimal.valueOf(5);
        assertTrue(productDAO.insert(product), "测试前置：商品必须插入成功");
        return product.id;
    }

    private InventoryCheck createCheck(String checkNo) {
        InventoryCheck check = new InventoryCheck();
        check.checkNo = checkNo;
        check.checkDate = "2026-09-29";
        check.checkType = "full";
        check.status = "checking";
        check.operator = "admin";
        check.remark = "";
        check.createTime = new Timestamp(System.currentTimeMillis());
        check.updateTime = check.createTime;
        return check;
    }

    private void insertItem(int checkId, int productId, String name, int book, int actual) throws SQLException {
        itemDAO.insert(newItem(checkId, productId, name, book, actual));
    }

    private void insertItemWithConnection(java.sql.Connection conn, int checkId, int productId, String name,
                                          int book, int actual) throws SQLException {
        assertTrue(itemDAO.insertWithConnection(conn, newItem(checkId, productId, name, book, actual)));
    }

    private InventoryCheckItem newItem(int checkId, int productId, String name, int book, int actual) {
        InventoryCheckItem item = new InventoryCheckItem();
        item.checkId = checkId;
        item.productId = productId;
        item.productName = name;
        item.bookQuantity = book;
        item.actualQuantity = actual;
        item.diffQuantity = actual - book;
        item.createTime = new Timestamp(System.currentTimeMillis());
        return item;
    }
}
