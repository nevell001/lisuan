package com.cashier.service;

import com.cashier.dao.ProductDAO;
import com.cashier.model.Product;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Locale;

/**
 * 库存服务类
 * 封装库存相关的业务逻辑
 */
public class InventoryService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(InventoryService.class);

    /**
     * 从数据库加载所有库存数据
     * @return 库存数据映射（商品名称 -> 商品对象）
     */
    public static Map<String, Product> loadAllInventory() {
        // 缓存有效时直接从缓存返回，无需查数据库
        if (com.cashier.util.CacheManager.isCacheValid()) {
            Map<String, Product> cached = com.cashier.util.CacheManager.getAllCachedInventory();
            if (!cached.isEmpty()) {
                logger.info("从缓存加载 {} 个商品", cached.size());
                return cached;
            }
        }

        // 缓存失效或为空，从数据库加载并写入缓存
        Map<String, Product> inventory = new HashMap<>();
        try {
            logger.info("缓存未命中，从数据库加载库存...");
            List<Product> products = ProductDAO.findAll();
            for (Product product : products) {
                inventory.put(product.name, product);
            }
            com.cashier.util.CacheManager.batchAddToCache(products);
            logger.info("从数据库加载 {} 个商品并写入缓存", inventory.size());
        } catch (SQLException e) {
            logger.error("加载库存数据失败", e);
        }
        return inventory;
    }

    /**
     * 刷新库存数据（从数据库重新加载）
     * @param inventory 库存数据映射
     */
    public static void refreshInventory(Map<String, Product> inventory) {
        logger.info("开始刷新库存数据...");
        Map<String, Product> latestInventory = loadAllInventory();
        inventory.clear();
        inventory.putAll(latestInventory);
        logger.info("库存数据刷新完成，共 {} 个商品", inventory.size());
    }

    /**
     * 搜索商品（支持名称、商品编号、条形码）
     * @param keyword 搜索关键词
     * @param inventory 库存数据
     * @return 匹配的商品列表
     */
    public static List<Product> searchProducts(String keyword, Map<String, Product> inventory) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>(inventory.values());
        }

        String lowerKeyword = keyword.toLowerCase(Locale.ROOT).trim();
        return inventory.values().stream()
                .filter(p -> p.name.toLowerCase(Locale.ROOT).contains(lowerKeyword) ||
                          p.barcode.toLowerCase(Locale.ROOT).contains(lowerKeyword) ||
                          (p.productCode != null && p.productCode.toLowerCase(Locale.ROOT).contains(lowerKeyword)))
                .collect(Collectors.toList());
    }

    /**
     * 获取低库存商品列表
     * @param inventory 库存数据
     * @return 低库存商品列表
     */
    public static List<Product> getLowStockProducts(Map<String, Product> inventory) {
        return inventory.values().stream()
                .filter(p -> p.quantity <= p.minStock)
                .sorted(Comparator.comparingInt(p -> p.quantity))
                .collect(Collectors.toList());
    }

    /**
     * 批量更新库存（优化版 - 使用批量SQL）
     * @param productUpdates 商品更新列表（商品ID -> 更新数量）
     * @return 是否成功
     */
    public static boolean batchUpdateInventory(Map<Integer, Integer> productUpdates) {
        if (productUpdates == null || productUpdates.isEmpty()) {
            return true;
        }

        try {
            boolean success = DatabaseManager.executeBooleanTransaction(conn -> {
                Map<Integer, Product> productsMap = ProductDAO.findByIdsWithConnection(conn, productUpdates.keySet());

                for (Map.Entry<Integer, Integer> entry : productUpdates.entrySet()) {
                    int productId = entry.getKey();
                    int quantityChange = entry.getValue();
                    Product product = productsMap.get(productId);

                    if (product == null) {
                        throw new SQLException("商品不存在，ID: " + productId);
                    }

                    if (quantityChange < 0 && product.quantity < Math.abs(quantityChange)) {
                        throw new SQLException("商品 " + product.name + " 库存不足！当前库存: " + product.quantity + ", 需要扣减: " + Math.abs(quantityChange));
                    }

                    product.quantity += quantityChange;
                    if (!ProductDAO.updateWithVersionWithConnection(conn, product)) {
                        throw new SQLException("商品 " + product.name + " 库存更新失败，可能已被其他操作修改");
                    }
                }
                return true;
            });

            if (success) {
                com.cashier.util.CacheManager.clearCache();
                logger.info("批量更新库存成功，共更新 {} 个商品", productUpdates.size());
            }
            return success;
        } catch (SQLException e) {
            logger.error("批量更新库存失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取库存统计信息
     * @param inventory 库存数据
     * @return 统计信息
     */
    public static Map<String, Object> getInventoryStatistics(Map<String, Product> inventory) {
        Map<String, Object> stats = new HashMap<>();

        int totalProducts = inventory.size();
        int totalQuantity = inventory.values().stream().mapToInt(p -> p.quantity).sum();
        BigDecimal totalValueDecimal = inventory.values().stream()
                .map(p -> p.getCost().multiply(BigDecimal.valueOf(p.quantity)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double totalValue = totalValueDecimal.doubleValue();
        int lowStockCount = (int) inventory.values().stream().filter(p -> p.quantity <= p.minStock).count();
        int outOfStockCount = (int) inventory.values().stream().filter(p -> p.quantity <= 0).count();

        stats.put("totalProducts", totalProducts);
        stats.put("totalQuantity", totalQuantity);
        stats.put("totalValue", totalValue);
        stats.put("lowStockCount", lowStockCount);
        stats.put("outOfStockCount", outOfStockCount);

        // 按分类统计
        Map<String, Integer> categoryStats = new HashMap<>();
        for (Product product : inventory.values()) {
            if (product.category != null && !product.category.isEmpty()) {
                categoryStats.merge(product.category, product.quantity, Integer::sum);
            }
        }
        stats.put("categoryStats", categoryStats);

        return stats;
    }

    /**
     * 检查商品库存是否充足
     * @param productId 商品ID
     * @param requiredQuantity 需要的数量
     * @return 是否充足
     */
    public static boolean checkStockAvailable(int productId, int requiredQuantity) {
        try {
            Product product = ProductDAO.findById(productId);
            if (product == null) {
                return false;
            }
            return product.quantity >= requiredQuantity;
        } catch (SQLException e) {
            logger.error("检查库存失败", e);
            return false;
        }
    }

    /**
     * 获取商品最新库存
     * @param productId 商品ID
     * @return 商品对象（包含最新库存），如果不存在返回null
     */
    public static Product getLatestProduct(int productId) {
        try {
            return ProductDAO.findById(productId);
        } catch (SQLException e) {
            logger.error("获取商品信息失败", e);
            return null;
        }
    }

}
