package com.cashier.util;

import com.cashier.model.Product;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理器
 * 提供数据缓存功能，减少数据库查询次数
 */
public class CacheManager {
    private static final Logger logger = LoggerFactoryUtil.getLogger(CacheManager.class);

    // 库存数据缓存（商品ID -> 商品）
    private static final Map<Integer, Product> productCache = new ConcurrentHashMap<>();
    
    // 库存数据缓存（商品名称 -> 商品）
    private static final Map<String, Product> productNameCache = new ConcurrentHashMap<>();
    
    // 库存数据缓存（商品条形码 -> 商品）
    private static final Map<String, Product> productBarcodeCache = new ConcurrentHashMap<>();

    // 缓存过期时间（毫秒）
    private static final long CACHE_EXPIRY_TIME = 5 * 60 * 1000; // 5分钟
    
    // 缓存创建时间
    private static volatile long cacheCreatedTime = 0;

    /**
     * 初始化缓存
     */
    public static void initialize() {
        cacheCreatedTime = System.currentTimeMillis();
        logger.info("缓存管理器已初始化");
    }

    /**
     * 获取商品缓存（通过ID）
     * @param productId 商品ID
     * @return 商品对象，如果缓存不存在或已过期返回null
     */
    public static Product getProductFromCache(int productId) {
        if (!isCacheValid()) {
            return null;
        }
        return productCache.get(productId);
    }

    /**
     * 获取商品缓存（通过名称）
     * @param productName 商品名称
     * @return 商品对象，如果缓存不存在或已过期返回null
     */
    public static Product getProductByNameFromCache(String productName) {
        if (!isCacheValid()) {
            return null;
        }
        return productNameCache.get(productName);
    }

    /**
     * 获取商品缓存（通过条形码）
     * @param barcode 商品条形码
     * @return 商品对象，如果缓存不存在或已过期返回null
     */
    public static Product getProductByBarcodeFromCache(String barcode) {
        if (!isCacheValid()) {
            return null;
        }
        return productBarcodeCache.get(barcode);
    }

    /**
     * 添加商品到缓存
     * @param product 商品对象
     */
    public static void addToCache(Product product) {
        if (product == null) {
            return;
        }
        
        productCache.put(product.id, product);
        if (product.name != null) {
            productNameCache.put(product.name, product);
        }
        if (product.barcode != null && !product.barcode.isEmpty()) {
            productBarcodeCache.put(product.barcode, product);
        }
        
        logger.debug("商品已添加到缓存: {}", product.name);
    }

    /**
     * 批量添加商品到缓存
     * @param products 商品列表
     */
    public static void batchAddToCache(java.util.List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        
        for (Product product : products) {
            addToCache(product);
        }
        
        cacheCreatedTime = System.currentTimeMillis();
        logger.info("批量添加 {} 个商品到缓存", products.size());
    }

    /**
     * 更新缓存中的商品
     * @param product 商品对象
     */
    public static void updateCache(Product product) {
        if (product == null) {
            return;
        }
        
        addToCache(product);
        logger.debug("缓存已更新: {}", product.name);
    }

    /**
     * 从缓存中移除商品
     * @param productId 商品ID
     */
    public static void removeFromCache(int productId) {
        Product product = productCache.remove(productId);
        if (product != null) {
            if (product.name != null) {
                productNameCache.remove(product.name);
            }
            if (product.barcode != null && !product.barcode.isEmpty()) {
                productBarcodeCache.remove(product.barcode);
            }
            logger.debug("商品已从缓存移除: {}", product.name);
        }
    }

    /**
     * 清空缓存
     */
    public static void clearCache() {
        int size = productCache.size();
        productCache.clear();
        productNameCache.clear();
        productBarcodeCache.clear();
        cacheCreatedTime = 0;
        logger.info("缓存已清空，共清除 {} 个商品", size);
    }

    /**
     * 检查缓存是否有效
     * @return 如果缓存有效返回true，否则返回false
     */
    public static boolean isCacheValid() {
        if (cacheCreatedTime == 0) {
            return false;
        }
        
        long age = System.currentTimeMillis() - cacheCreatedTime;
        if (age > CACHE_EXPIRY_TIME) {
            logger.debug("缓存已过期（{}ms > {}ms），需要刷新", age, CACHE_EXPIRY_TIME);
            return false;
        }
        
        return true;
    }

    /**
     * 获取缓存大小
     * @return 缓存中的商品数量
     */
    public static int getCacheSize() {
        return productCache.size();
    }

    /**
     * 获取缓存年龄（毫秒）
     * @return 缓存创建至今的时间（毫秒）
     */
    public static long getCacheAge() {
        if (cacheCreatedTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - cacheCreatedTime;
    }

    /**
     * 刷新缓存时间戳
     */
    public static void refreshCacheTimestamp() {
        cacheCreatedTime = System.currentTimeMillis();
    }

    /**
     * 获取全部缓存商品（名称 -> 商品），仅在缓存有效时使用
     * @return 商品名称到商品对象的不可变快照；缓存无效时返回空 Map
     */
    public static Map<String, Product> getAllCachedInventory() {
        if (!isCacheValid()) {
            return java.util.Collections.emptyMap();
        }
        return new java.util.HashMap<>(productNameCache);
    }

    /**
     * 预热缓存（在应用启动时调用）
     */
    public static void warmupCache() {
        try {
            java.util.List<Product> products = com.cashier.dao.ProductDAO.findAll();
            batchAddToCache(products);
            logger.info("缓存预热完成，共加载 {} 个商品", products.size());
        } catch (Exception e) {
            logger.error("缓存预热失败", e);
        }
    }
}