package com.cashier.util;

import com.cashier.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CacheManagerTest {

    private Product createTestProduct(int id, String name, String barcode, double price, int quantity) {
        Product p = new Product();
        p.id = id;
        p.name = name;
        p.barcode = barcode;
        p.price = BigDecimal.valueOf(price);
        p.quantity = quantity;
        return p;
    }

    @BeforeEach
    void clearCache() {
        CacheManager.clearCache();
        CacheManager.initialize();
    }

    @Test
    void testAddAndGetById() {
        Product p = createTestProduct(1, "苹果", "6901234567890", 5.5, 100);
        CacheManager.addToCache(p);

        Product found = CacheManager.getProductFromCache(1);
        assertNotNull(found);
        assertEquals("苹果", found.name);
        assertEquals(0, BigDecimal.valueOf(5.5).compareTo(found.price));
    }

    @Test
    void testGetByIdNotFound() {
        Product found = CacheManager.getProductFromCache(99999);
        assertNull(found);
    }

    @Test
    void testAddAndGetByName() {
        Product p = createTestProduct(2, "香蕉", "6901234567891", 3.0, 200);
        CacheManager.addToCache(p);

        Product found = CacheManager.getProductByNameFromCache("香蕉");
        assertNotNull(found);
        assertEquals(2, found.id);
    }

    @Test
    void testAddAndGetByBarcode() {
        Product p = createTestProduct(3, "橙子", "6901234567892", 4.5, 150);
        CacheManager.addToCache(p);

        Product found = CacheManager.getProductByBarcodeFromCache("6901234567892");
        assertNotNull(found);
        assertEquals("橙子", found.name);
    }

    @Test
    void testClearCache() {
        CacheManager.addToCache(createTestProduct(1, "商品A", "BC001", 10.0, 50));
        CacheManager.addToCache(createTestProduct(2, "商品B", "BC002", 20.0, 60));

        CacheManager.clearCache();

        assertNull(CacheManager.getProductFromCache(1));
        assertNull(CacheManager.getProductFromCache(2));
        assertNull(CacheManager.getProductByNameFromCache("商品A"));
    }

    @Test
    void testUpdateCache() {
        Product p = createTestProduct(1, "原商品", "BC001", 10.0, 50);
        CacheManager.addToCache(p);

        Product updated = createTestProduct(1, "更新商品", "BC001", 15.0, 80);
        CacheManager.updateCache(updated);

        Product found = CacheManager.getProductFromCache(1);
        assertNotNull(found);
        assertEquals("更新商品", found.name);
        assertEquals(0, BigDecimal.valueOf(15.0).compareTo(found.price));
        assertEquals(80, found.quantity);
    }

    @Test
    void testRemoveFromCache() {
        Product p = createTestProduct(5, "待删除", "BC005", 10.0, 50);
        CacheManager.addToCache(p);

        CacheManager.removeFromCache(5);

        assertNull(CacheManager.getProductFromCache(5));
        assertNull(CacheManager.getProductByNameFromCache("待删除"));
        assertNull(CacheManager.getProductByBarcodeFromCache("BC005"));
    }

    @Test
    void testCacheValidity() {
        CacheManager.clearCache();
        assertFalse(CacheManager.isCacheValid());

        CacheManager.initialize();
        CacheManager.addToCache(createTestProduct(1, "测试", "BC001", 10.0, 50));
        assertTrue(CacheManager.isCacheValid());
    }

    @Test
    void testBatchAddToCache() {
        java.util.List<Product> products = java.util.List.of(
            createTestProduct(1, "批量1", "BC001", 10.0, 50),
            createTestProduct(2, "批量2", "BC002", 20.0, 60)
        );
        CacheManager.batchAddToCache(products);

        assertEquals(2, CacheManager.getCacheSize());
        assertNotNull(CacheManager.getProductFromCache(1));
        assertNotNull(CacheManager.getProductFromCache(2));
    }
}
