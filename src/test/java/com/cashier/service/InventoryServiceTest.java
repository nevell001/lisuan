package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.util.DatabaseTestBase;
import com.cashier.model.Product;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InventoryService 单元测试
 * 测试库存服务的核心功能
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InventoryServiceTest extends DatabaseTestBase {

    private Product testProduct;

    @BeforeEach
    void setUp() throws Exception {
        // 确保使用测试数据库
        if (!DatabaseTestBase.isInitialized()) {
            DatabaseTestBase.initTestDatabase();
        }
        
        // 清空数据库
        clearTestData();

        // 创建测试商品
        testProduct = createProduct("测试商品", 10.0, 100);
    }

    @Test
    @Order(1)
    @DisplayName("测试批量更新库存 - 正常情况")
    void testBatchUpdateInventoryNormal() throws Exception {
        int initialQuantity = testProduct.quantity;
        int deductQuantity = 10;

        // 扣减库存
        Map<Integer, Integer> productUpdates = new HashMap<>();
        productUpdates.put(testProduct.id, -deductQuantity);
        int count = InventoryService.batchUpdateInventory(productUpdates);

        assertTrue(count > 0);

        // 验证库存已扣减
        Product updatedProduct = DAOFactory.getInstance().getProductDAO().findById(testProduct.id);
        assertEquals(initialQuantity - deductQuantity, updatedProduct.quantity);
    }

    @Test
    @Order(2)
    @DisplayName("测试批量更新库存 - 库存不足")
    void testBatchUpdateInventoryInsufficientStock() throws Exception {
        int deductQuantity = testProduct.quantity + 10; // 超出库存

        // 扣减库存（应该失败）
        Map<Integer, Integer> productUpdates = new HashMap<>();
        productUpdates.put(testProduct.id, -deductQuantity);
        int count = InventoryService.batchUpdateInventory(productUpdates);

        assertEquals(0, count);

        // 验证库存未变化
        Product updatedProduct = DAOFactory.getInstance().getProductDAO().findById(testProduct.id);
        assertEquals(testProduct.quantity, updatedProduct.quantity);
    }

    @Test
    @Order(3)
    @DisplayName("测试批量更新库存 - 增加库存")
    void testBatchUpdateInventoryIncrease() throws Exception {
        int initialQuantity = testProduct.quantity;
        int increaseQuantity = 50;

        // 增加库存
        Map<Integer, Integer> productUpdates = new HashMap<>();
        productUpdates.put(testProduct.id, increaseQuantity);
        int count = InventoryService.batchUpdateInventory(productUpdates);

        assertTrue(count > 0);

        // 验证库存已增加
        Product updatedProduct = DAOFactory.getInstance().getProductDAO().findById(testProduct.id);
        assertEquals(initialQuantity + increaseQuantity, updatedProduct.quantity);
    }

    @Test
    @Order(4)
    @DisplayName("测试批量更新库存 - 多商品失败时整体回滚")
    void testBatchUpdateInventoryRollbackAcrossMultipleProducts() throws Exception {
        Product secondProduct = createProduct("第二个测试商品", 12.0, 5);

        Map<Integer, Integer> productUpdates = new java.util.LinkedHashMap<>();
        productUpdates.put(testProduct.id, -10);
        productUpdates.put(secondProduct.id, -10);

        int count = InventoryService.batchUpdateInventory(productUpdates);

        assertEquals(0, count);

        Product firstUpdatedProduct = DAOFactory.getInstance().getProductDAO().findById(testProduct.id);
        Product secondUpdatedProduct = DAOFactory.getInstance().getProductDAO().findById(secondProduct.id);
        assertEquals(testProduct.quantity, firstUpdatedProduct.quantity);
        assertEquals(secondProduct.quantity, secondUpdatedProduct.quantity);
    }

    @Test
    @Order(5)
    @DisplayName("测试检查库存是否充足")
    void testCheckStockAvailable() throws Exception {
        // 充足库存
        assertTrue(InventoryService.checkStockAvailable(testProduct.id, 50));

        // 库存不足
        assertFalse(InventoryService.checkStockAvailable(testProduct.id, 150));

        // 刚好充足
        assertTrue(InventoryService.checkStockAvailable(testProduct.id, testProduct.quantity));
    }

    /**
     * 辅助方法：创建测试商品
     */
    private Product createProduct(String name, double price, int quantity) throws Exception {
        Product product = new Product();
        product.productCode = "P" + name.hashCode();
        product.name = name;
        product.price = BigDecimal.valueOf(price);
        product.quantity = quantity;
        product.category = "测试分类";
        product.barcode = "TEST" + name.hashCode();
        product.unit = "个";
        product.minStock = 10;
        product.cost = BigDecimal.valueOf(price).multiply(new BigDecimal("0.7"));
        product.version = 0;

        DAOFactory.getInstance().getProductDAO().insert(product);
        return DAOFactory.getInstance().getProductDAO().findByName(name);
    }

    }