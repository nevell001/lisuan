package com.cashier.util;

import com.cashier.model.Product;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 缓存管理器
 * 提供数据缓存功能，减少数据库查询次数
 * 使用 LRU (Least Recently Used) 策略管理缓存大小
 */
public class CacheManager {
    private static final Logger logger = LoggerFactoryUtil.getLogger(CacheManager.class);

    // 最大缓存大小 - 防止内存占用过高
    private static final int MAX_CACHE_SIZE = 5000;

    // 缓存过期时间（毫秒）
    private static final long CACHE_EXPIRY_TIME = 5 * 60 * 1000; // 5分钟

    // 读写锁用于线程安全
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    // LRU 缓存实现：LinkedHashMap
    // 注意：accessOrder 必须为 false —— accessOrder=true 时 get() 会修改链表结构，
    // 在读锁（readLock）下多线程并发 get() 会导致竞态条件。
    // 缓存每 5 分钟整体刷新，FIFO 驱逐（按插入顺序）已足够。
    private static final Map<Integer, Product> productCache = new LinkedHashMap<Integer, Product>(128, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Product> eldest) {
            // 当缓存大小超过限制时，移除最久未使用的条目
            if (size() > MAX_CACHE_SIZE) {
                // 同时从其他缓存中移除
                Product removed = eldest.getValue();
                if (removed.name != null) {
                    productNameCache.remove(removed.name);
                }
                if (removed.barcode != null && !removed.barcode.isEmpty()) {
                    productBarcodeCache.remove(removed.barcode);
                }
                return true;
            }
            return false;
        }
    };

    // 库存数据缓存（商品名称 -> 商品）
    // 注意：所有访问都通过 ReadWriteLock 保护，accessOrder=false 保证读操作不修改内部结构
    private static final Map<String, Product> productNameCache = new LinkedHashMap<>(128, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Product> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    // 库存数据缓存（商品条形码 -> 商品）
    // 注意：所有访问都通过 ReadWriteLock 保护，accessOrder=false 保证读操作不修改内部结构
    private static final Map<String, Product> productBarcodeCache = new LinkedHashMap<>(128, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Product> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    // 缓存创建时间
    private static volatile long cacheCreatedTime = 0;

    /**
     * 初始化缓存
     */
    public static void initialize() {
        cacheCreatedTime = System.currentTimeMillis();
        logger.info("缓存管理器已初始化，最大缓存大小: {}", MAX_CACHE_SIZE);
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
        lock.readLock().lock();
        try {
            return productCache.get(productId);
        } finally {
            lock.readLock().unlock();
        }
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
        lock.readLock().lock();
        try {
            return productNameCache.get(productName);
        } finally {
            lock.readLock().unlock();
        }
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
        lock.readLock().lock();
        try {
            return productBarcodeCache.get(barcode);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 添加商品到缓存
     * @param product 商品对象
     */
    public static void addToCache(Product product) {
        if (product == null) {
            return;
        }

        lock.writeLock().lock();
        try {
            productCache.put(product.id, product);
            if (product.name != null) {
                productNameCache.put(product.name, product);
            }
            if (product.barcode != null && !product.barcode.isEmpty()) {
                productBarcodeCache.put(product.barcode, product);
            }
            // 缓存被清空/过期后（cacheCreatedTime=0），单条添加也要重新武装缓存时钟，
            // 否则该路径填出的缓存会因 isCacheValid() 恒 false 而永远回源数据库。
            if (cacheCreatedTime == 0) {
                cacheCreatedTime = System.currentTimeMillis();
            }
            logger.debug("商品已添加到缓存: {}", product.name);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 批量添加商品到缓存
     * @param products 商品列表
     */
    public static void batchAddToCache(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }

        lock.writeLock().lock();
        try {
            for (Product product : products) {
                productCache.put(product.id, product);
                if (product.name != null) {
                    productNameCache.put(product.name, product);
                }
                if (product.barcode != null && !product.barcode.isEmpty()) {
                    productBarcodeCache.put(product.barcode, product);
                }
            }
            cacheCreatedTime = System.currentTimeMillis();
            logger.info("批量添加 {} 个商品到缓存，当前缓存大小: {}", products.size(), productCache.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 更新缓存中的商品
     * @param product 商品对象
     */
    public static void updateCache(Product product) {
        addToCache(product);
        logger.debug("缓存已更新: {}", product.name);
    }

    /**
     * 从缓存中移除商品
     * @param productId 商品ID
     */
    public static void removeFromCache(int productId) {
        lock.writeLock().lock();
        try {
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
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 清空缓存
     */
    public static void clearCache() {
        lock.writeLock().lock();
        try {
            int size = productCache.size();
            productCache.clear();
            productNameCache.clear();
            productBarcodeCache.clear();
            cacheCreatedTime = 0;
            logger.info("缓存已清空，共清除 {} 个商品", size);
        } finally {
            lock.writeLock().unlock();
        }
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
            // 过期时自动清除数据，避免内存泄漏
            clearCache();
            return false;
        }

        return true;
    }

    /**
     * 获取缓存大小
     * @return 缓存中的商品数量
     */
    public static int getCacheSize() {
        lock.readLock().lock();
        try {
            return productCache.size();
        } finally {
            lock.readLock().unlock();
        }
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
            return Collections.emptyMap();
        }
        lock.readLock().lock();
        try {
            return new java.util.HashMap<>(productNameCache);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 预热缓存（在应用启动时调用）
     * L-3: 分批加载全部商品，而非仅前 MAX_CACHE_SIZE 个
     */
    public static void warmupCache() {
        try {
            long startTime = System.currentTimeMillis();
            int pageSize = MAX_CACHE_SIZE;
            int page = 1;
            int totalLoaded = 0;
            List<Product> allProducts = new java.util.ArrayList<>();

            // 分页加载所有商品
            while (true) {
                List<Product> batch = com.cashier.dao.DAOFactory.getInstance()
                    .getProductDAO()
                    .findAll(page, pageSize)
                    .getData();
                if (batch == null || batch.isEmpty()) {
                    break;
                }
                allProducts.addAll(batch);
                totalLoaded += batch.size();
                if (batch.size() < pageSize) {
                    break; // 最后一页
                }
                page++;
            }

            batchAddToCache(allProducts);
            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("缓存预热完成，分页加载 {} 个商品（每页 {}），实际缓存 {} 个，耗时: {}ms",
                totalLoaded, pageSize, allProducts.size(), elapsed);
        } catch (Exception e) {
            logger.error("缓存预热失败", e);
        }
    }
}
