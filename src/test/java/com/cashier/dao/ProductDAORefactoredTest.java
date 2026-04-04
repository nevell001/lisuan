package com.cashier.dao;

import com.cashier.model.PageResult;
import com.cashier.model.Product;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    @AfterAll
    static void tearDown() throws SQLException {
        // 清理测试数据
        // 注意：这里应该清理所有以 TEST、CRUD、LOCK、COUNT 开头的测试数据
    }
}
