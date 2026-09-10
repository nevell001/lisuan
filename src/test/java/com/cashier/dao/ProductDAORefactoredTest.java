package com.cashier.dao;

import com.cashier.model.InventoryStatistics;
import com.cashier.model.PageResult;
import com.cashier.model.Product;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductDAORefactored 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductDAORefactoredTest extends DatabaseTestBase {

    private static ProductDAORefactored productDAO;

    @BeforeAll
    static void setUp() throws SQLException {
        initTestDatabase();
        productDAO = new ProductDAORefactored();
    }

    @Test
    @Order(1)
    @DisplayName("测试分页查询")
    void testFindAllWithPagination() throws SQLException {
        // 先插入一些测试数据
        List<Product> testProducts = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            Product p = new Product();
            p.productCode = "TEST" + String.format("%03d", i);
            p.name = "测试商品" + i;
            p.price = BigDecimal.valueOf(10.0 * i);
            p.quantity = 100;
            p.category = "测试分类";
            p.unit = "个";
            p.minStock = 10;
            p.cost = BigDecimal.valueOf(5.0 * i);
            testProducts.add(p);
        }
        
        // 批量插入
        productDAO.batchInsert(testProducts);
        
        // 测试分页查询 - 第一页
        PageResult<Product> page1 = productDAO.findAll(1, 10);
        assertNotNull(page1);
        assertEquals(1, page1.getPageNum());
        assertEquals(10, page1.getPageSize());
        assertTrue(page1.getData().size() <= 10);
        
        // 测试分页查询 - 第二页
        PageResult<Product> page2 = productDAO.findAll(2, 10);
        assertNotNull(page2);
        assertEquals(2, page2.getPageNum());
        
        // 验证分页信息
        assertTrue(page1.hasNextPage() || page1.getPages() <= 1);
        assertTrue(page2.hasPreviousPage() || page2.getPages() <= 1);
    }

    @Test
    @Order(2)
    @DisplayName("测试CRUD操作")
    void testCRUD() throws SQLException {
        // 创建
        Product product = new Product();
        product.productCode = "CRUD001";
        product.name = "CRUD测试商品";
        product.price = BigDecimal.valueOf(99.99);
        product.quantity = 50;
        product.category = "测试";
        product.unit = "件";
        product.minStock = 5;
        product.cost = BigDecimal.valueOf(50.0);
        
        boolean inserted = productDAO.insert(product);
        assertTrue(inserted);
        assertTrue(product.id > 0, "插入后应该有自增ID");
        
        // 读取
        Product found = productDAO.findById(product.id);
        assertNotNull(found);
        assertEquals("CRUD001", found.productCode);
        assertEquals("CRUD测试商品", found.name);
        
        // 更新
        found.name = "CRUD测试商品-已更新";
        found.price = BigDecimal.valueOf(199.99);
        boolean updated = productDAO.update(found);
        assertTrue(updated);
        assertEquals(found.version, 1, "更新后版本号应该增加");
        
        // 验证更新
        Product updatedProduct = productDAO.findById(product.id);
        assertEquals("CRUD测试商品-已更新", updatedProduct.name);
        assertEquals(199.99, updatedProduct.price.doubleValue(), 0.01);
        
        // 删除
        boolean deleted = productDAO.delete(product.id);
        assertTrue(deleted);
        
        // 验证删除
        Product deletedProduct = productDAO.findById(product.id);
        assertNull(deletedProduct);
    }

    @Test
    @Order(3)
    @DisplayName("测试乐观锁冲突")
    void testOptimisticLock() throws SQLException {
        // 创建商品
        Product product = new Product();
        product.productCode = "LOCK001";
        product.name = "乐观锁测试";
        product.price = BigDecimal.valueOf(100.0);
        product.quantity = 10;
        product.category = "测试";
        product.unit = "个";
        product.minStock = 1;
        product.cost = BigDecimal.valueOf(50.0);
        
        productDAO.insert(product);
        assertEquals(0, product.version);
        
        // 模拟两个并发更新
        Product copy1 = productDAO.findById(product.id);
        Product copy2 = productDAO.findById(product.id);
        
        // 第一个更新成功
        copy1.name = "第一次更新";
        boolean updated1 = productDAO.update(copy1);
        assertTrue(updated1);
        assertEquals(1, copy1.version);
        
        // 第二个更新失败（版本号已过期）
        copy2.name = "第二次更新";
        boolean updated2 = productDAO.update(copy2);
        assertFalse(updated2);
        
        // 清理
        productDAO.delete(product.id);
    }

    @Test
    @Order(4)
    @DisplayName("测试统计数量")
    void testCount() throws SQLException {
        long count1 = productDAO.count();
        assertTrue(count1 >= 0);
        
        // 插入一个新商品
        Product product = new Product();
        product.productCode = "COUNT001";
        product.name = "计数测试";
        product.price = BigDecimal.valueOf(10.0);
        product.quantity = 10;
        product.category = "测试";
        product.unit = "个";
        product.minStock = 1;
        product.cost = BigDecimal.valueOf(5.0);
        
        productDAO.insert(product);
        
        long count2 = productDAO.count();
        assertEquals(count1 + 1, count2);
        
        // 清理
        productDAO.delete(product.id);
    }

    @Test
    @Order(5)
    @DisplayName("测试事务回滚")
    void testTransactionRollback() {
        // 创建一个无效的商品（名称空）触发验证失败
        Product invalidProduct = new Product();
        invalidProduct.productCode = "INVALID001";
        // name 为空，应该触发验证失败
        
        assertThrows(SQLException.class, () -> {
            productDAO.insert(invalidProduct);
        });
    }

    @Test
    @Order(6)
    @DisplayName("测试数据库侧库存统计和预警查询")
    void testInventoryStatisticsAndAlertQuery() throws SQLException {
        InventoryStatistics baseline = productDAO.getInventoryStatistics();

        Product lowStockProduct = new Product();
        lowStockProduct.productCode = "STAT_LOW_001";
        lowStockProduct.name = "库存统计低库存商品";
        lowStockProduct.price = BigDecimal.valueOf(20.0);
        lowStockProduct.quantity = 3;
        lowStockProduct.category = "统计测试";
        lowStockProduct.unit = "个";
        lowStockProduct.minStock = 5;
        lowStockProduct.cost = BigDecimal.valueOf(8.0);

        Product healthyProduct = new Product();
        healthyProduct.productCode = "STAT_OK_001";
        healthyProduct.name = "库存统计正常商品";
        healthyProduct.price = BigDecimal.valueOf(50.0);
        healthyProduct.quantity = 12;
        healthyProduct.category = "统计测试";
        healthyProduct.unit = "个";
        healthyProduct.minStock = 5;
        healthyProduct.cost = BigDecimal.valueOf(30.0);

        productDAO.insert(lowStockProduct);
        productDAO.insert(healthyProduct);

        InventoryStatistics stats = productDAO.getInventoryStatistics();
        BigDecimal expectedAddedValue = BigDecimal.valueOf(384.0);

        assertEquals(baseline.getTotalProducts() + 2, stats.getTotalProducts());
        assertEquals(baseline.getTotalQuantity() + 15, stats.getTotalQuantity());
        assertEquals(baseline.getLowStockCount() + 1, stats.getLowStockCount());
        assertEquals(0, baseline.getTotalValue().add(expectedAddedValue).compareTo(stats.getTotalValue()));
        assertTrue(productDAO.findProductsRequiringStockAlert().stream()
            .anyMatch(product -> "STAT_LOW_001".equals(product.productCode)));
        assertFalse(productDAO.findProductsRequiringStockAlert().stream()
            .anyMatch(product -> "STAT_OK_001".equals(product.productCode)));

        productDAO.delete(lowStockProduct.id);
        productDAO.delete(healthyProduct.id);
    }

    @Test
    @Order(7)
    @DisplayName("测试根据商品名称批量查询")
    void testFindByNames() throws SQLException {
        Product firstProduct = new Product();
        firstProduct.productCode = "NAME_BATCH_001";
        firstProduct.name = "批量名称商品一";
        firstProduct.price = BigDecimal.valueOf(12.0);
        firstProduct.quantity = 5;
        firstProduct.category = "批量测试";
        firstProduct.unit = "个";
        firstProduct.minStock = 1;
        firstProduct.cost = BigDecimal.valueOf(6.0);

        Product secondProduct = new Product();
        secondProduct.productCode = "NAME_BATCH_002";
        secondProduct.name = "批量名称商品二";
        secondProduct.price = BigDecimal.valueOf(18.0);
        secondProduct.quantity = 8;
        secondProduct.category = "批量测试";
        secondProduct.unit = "个";
        secondProduct.minStock = 1;
        secondProduct.cost = BigDecimal.valueOf(9.0);

        productDAO.insert(firstProduct);
        productDAO.insert(secondProduct);

        var products = productDAO.findByNames(Set.of("批量名称商品一", "批量名称商品二", "不存在商品"));

        assertEquals(2, products.size());
        assertEquals("NAME_BATCH_001", products.get("批量名称商品一").productCode);
        assertEquals("NAME_BATCH_002", products.get("批量名称商品二").productCode);
        assertFalse(products.containsKey("不存在商品"));

        productDAO.delete(firstProduct.id);
        productDAO.delete(secondProduct.id);
    }

    @Test
    @DisplayName("商品编号序号按已用最大值生成，删过商品也不会撞号")
    void nextProductCodeSequenceUsesMaxNotCount() throws SQLException {
        String prefix = "SEQ20260101";
        for (int i = 1; i <= 3; i++) {
            assertTrue(productDAO.insert(newProduct(prefix + String.format("%04d", i), "序列商品" + i)));
        }
        assertEquals(3, productDAO.findMaxProductCodeSequence(prefix));

        // 删掉中间一个：按“当天数量 +1”会重新生成 0003（已存在），按最大序号才是 0004
        Product middle = productDAO.findByName("序列商品2");
        assertTrue(productDAO.delete(middle.id));

        assertEquals(3, productDAO.findMaxProductCodeSequence(prefix));
        assertEquals(prefix + "0004",
            prefix + String.format("%04d", productDAO.findMaxProductCodeSequence(prefix) + 1));

        for (int i = 1; i <= 3; i++) {
            if (i == 2) {
                continue;
            }
            Product product = productDAO.findByName("序列商品" + i);
            productDAO.delete(product.id);
        }
    }

    @Test
    @DisplayName("商品名称重复时拒绝新增与改名（v2.4.3 唯一约束）")
    void duplicateProductNameIsRejected() throws SQLException {
        Product first = newProduct("DUPNAME001", "重名测试商品");
        assertTrue(productDAO.insert(first));

        SQLException onInsert = assertThrows(SQLException.class,
            () -> productDAO.insert(newProduct("DUPNAME002", "重名测试商品")));
        assertTrue(onInsert.getMessage().contains("商品名称已存在"),
            "重复名称应给出友好提示，实际: " + onInsert.getMessage());

        Product other = newProduct("DUPNAME003", "另一个测试商品");
        assertTrue(productDAO.insert(other));

        other.name = "重名测试商品";
        assertThrows(SQLException.class, () -> productDAO.update(other),
            "改名为已存在的名称应被拒绝");

        // 保持自身名称的更新不受影响
        first.price = BigDecimal.valueOf(12.34);
        assertTrue(productDAO.update(first));

        productDAO.delete(first.id);
        productDAO.delete(other.id);
    }

    private Product newProduct(String code, String name) {
        Product product = new Product();
        product.productCode = code;
        product.name = name;
        product.price = BigDecimal.TEN;
        product.quantity = 1;
        product.category = "测试分类";
        product.unit = "个";
        product.minStock = 0;
        product.cost = BigDecimal.ONE;
        return product;
    }

    @AfterAll
    static void tearDown() throws SQLException {
        // 清理测试数据
        // 注意：这里应该清理所有以 TEST、CRUD、LOCK、COUNT 开头的测试数据
    }
}
