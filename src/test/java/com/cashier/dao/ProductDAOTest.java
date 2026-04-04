package com.cashier.dao;

import com.cashier.model.Product;
import com.cashier.util.DatabaseManager;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductDAO 单元测试
 */
@DisplayName("商品数据访问对象测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductDAOTest extends DatabaseTestBase {

    private static Product testProduct;
    private static int insertedProductId;

    @BeforeAll
    @DisplayName("初始化测试环境")
    public static void setUpBeforeClass() throws SQLException {
        // 初始化测试数据库
        initTestDatabase();

        // 创建测试商品
        testProduct = new Product(
            0,
            "TEST001",
            "测试商品-单元测试",
            19.99,
            50,
            "测试分类",
            "TEST1234567890",
            "件",
            "这是一个用于单元测试的商品描述",
            "测试品牌",
            "测试供应商",
            "规格信息",
            10,
            15.00
        );
    }

    @BeforeEach
    @DisplayName("准备测试环境")
    public void setUp() {
        // 不需要清理数据，因为测试按顺序执行，依赖前面的测试结果
    }

    @Test
    @Order(1)
    @DisplayName("测试插入商品")
    public void testInsertProduct() throws SQLException {
        boolean result = ProductDAO.insert(testProduct);
        assertTrue(result);
        assertNotNull(testProduct.id);
        assertTrue(testProduct.id > 0);
        insertedProductId = testProduct.id;
    }

    @Test
    @Order(2)
    @DisplayName("测试根据ID查找商品")
    public void testFindById() throws SQLException {
        Product found = ProductDAO.findById(insertedProductId);
        assertNotNull(found);
        assertEquals(insertedProductId, found.id);
        assertEquals("测试商品-单元测试", found.name);
        assertEquals("TEST001", found.productCode);
    }

    @Test
    @Order(3)
    @DisplayName("测试根据商品编号查找商品")
    public void testFindByProductCode() throws SQLException {
        Product found = ProductDAO.findByProductCode("TEST001");
        assertNotNull(found);
        assertEquals("TEST001", found.productCode);
        assertEquals("测试商品-单元测试", found.name);
    }

    @Test
    @Order(4)
    @DisplayName("测试根据条形码查找商品")
    public void testFindByBarcode() throws SQLException {
        Product found = ProductDAO.findByBarcode("TEST1234567890");
        assertNotNull(found);
        assertEquals("TEST1234567890", found.barcode);
        assertEquals("测试商品-单元测试", found.name);
    }

    @Test
    @Order(5)
    @DisplayName("测试查询所有商品")
    public void testFindAll() throws SQLException {
        List<Product> products = ProductDAO.findAll();
        assertNotNull(products);
        assertTrue(products.size() > 0);

        // 验证测试商品在列表中
        boolean found = products.stream()
                .anyMatch(p -> p.productCode.equals("TEST001"));
        assertTrue(found);
    }

    @Test
    @Order(6)
    @DisplayName("测试更新商品")
    public void testUpdateProduct() throws SQLException {
        testProduct.price = BigDecimal.valueOf(25.99);
        testProduct.quantity = 100;
        testProduct.description = "更新后的商品描述";

        boolean result = ProductDAO.update(testProduct);
        assertTrue(result);

        Product updated = ProductDAO.findById(insertedProductId);
        assertEquals(25.99, updated.price.doubleValue(), 0.01);
        assertEquals(100, updated.quantity);
        assertEquals("更新后的商品描述", updated.description);
    }

    @Test
    @Order(7)
    @DisplayName("测试更新商品库存")
    public void testUpdateQuantity() throws SQLException {
        boolean result = ProductDAO.updateQuantity(insertedProductId, 10);
        assertTrue(result);

        Product updated = ProductDAO.findById(insertedProductId);
        assertEquals(110, updated.quantity);
    }

    @Test
    @Order(8)
    @DisplayName("测试搜索商品")
    public void testSearchProduct() throws SQLException {
        List<Product> results = ProductDAO.search("测试");
        assertNotNull(results);
        assertTrue(results.size() > 0);

        boolean found = results.stream()
                .anyMatch(p -> p.productCode.equals("TEST001"));
        assertTrue(found);
    }

    @Test
    @Order(9)
    @DisplayName("测试根据分类查询商品")
    public void testFindByCategory() throws SQLException {
        List<Product> results = ProductDAO.findByCategory("测试分类");
        assertNotNull(results);
        assertTrue(results.size() > 0);

        boolean found = results.stream()
                .anyMatch(p -> p.productCode.equals("TEST001"));
        assertTrue(found);
    }

    @Test
    @Order(10)
    @DisplayName("测试删除商品")
    public void testDeleteProduct() throws SQLException {
        boolean result = ProductDAO.delete(insertedProductId);
        assertTrue(result);

        Product deleted = ProductDAO.findById(insertedProductId);
        assertNull(deleted);
    }

    @Test
    @DisplayName("测试查找不存在的商品")
    public void testFindNonExistentProduct() throws SQLException {
        Product found = ProductDAO.findById(999999);
        assertNull(found);

        found = ProductDAO.findByProductCode("NONEXISTENT");
        assertNull(found);

        found = ProductDAO.findByBarcode("9999999999999");
        assertNull(found);
    }

    @Test
    @DisplayName("测试批量插入商品")
    public void testBatchInsertProducts() throws SQLException {
        List<Product> products = List.of(
            new Product(0, "BATCH001", "批量测试商品1", 10.00, 20, "批量测试", "BATCH1", "件", "描述1", "品牌1", "供应商1", "规格1", 5, 8.00),
            new Product(0, "BATCH002", "批量测试商品2", 20.00, 30, "批量测试", "BATCH2", "件", "描述2", "品牌2", "供应商2", "规格2", 10, 15.00),
            new Product(0, "BATCH003", "批量测试商品3", 30.00, 40, "批量测试", "BATCH3", "件", "描述3", "品牌3", "供应商3", "规格3", 15, 25.00)
        );

        ProductDAO.batchInsert(products);

        // 验证插入成功
        for (Product p : products) {
            assertNotNull(p.id);
            assertTrue(p.id > 0);
        }

        // 清理测试数据
        for (Product p : products) {
            ProductDAO.delete(p.id);
        }
    }

    @AfterAll
    @DisplayName("清理测试环境")
    public static void tearDownAfterClass() throws SQLException {
        DatabaseTestBase.clearTestData();
        DatabaseManager.clearTestConnection();
    }
}