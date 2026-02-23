package com.cashier.service;

import com.cashier.dao.ProductDAO;
import com.cashier.model.Product;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

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
        Map<String, Product> inventory = new HashMap<>();
        try {
            // 先检查缓存
            if (com.cashier.util.CacheManager.isCacheValid()) {
                List<Product> products = ProductDAO.findAll();
                for (Product product : products) {
                    inventory.put(product.name, product);
                }
                logger.info("从数据库加载 {} 个商品", inventory.size());
            } else {
                // 缓存失效，刷新缓存
                logger.info("缓存已失效，刷新缓存...");
                List<Product> products = ProductDAO.findAll();
                for (Product product : products) {
                    inventory.put(product.name, product);
                }
                com.cashier.util.CacheManager.batchAddToCache(products);
                logger.info("从数据库加载 {} 个商品并更新缓存", inventory.size());
            }
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

        String lowerKeyword = keyword.toLowerCase().trim();
        return inventory.values().stream()
                .filter(p -> p.name.toLowerCase().contains(lowerKeyword) ||
                          p.barcode.toLowerCase().contains(lowerKeyword) ||
                          (p.productCode != null && p.productCode.toLowerCase().contains(lowerKeyword)))
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

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            DatabaseManager.beginTransaction(conn);

            // 先批量查询所有需要更新的商品（使用同一连接）
            List<Integer> productIds = new ArrayList<>(productUpdates.keySet());
            Map<Integer, Product> productsMap = new HashMap<>();

            String querySql = "SELECT id, product_code, name, price, quantity, category, barcode, unit, description, " +
                             "brand, supplier, spec, min_stock, cost, version FROM products WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(querySql)) {
                for (Integer productId : productIds) {
                    pstmt.setInt(1, productId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            Product product = new Product();
                            product.id = rs.getInt("id");
                            product.productCode = rs.getString("product_code");
                            product.name = rs.getString("name");
                            product.price = rs.getDouble("price");
                            product.quantity = rs.getInt("quantity");
                            product.category = rs.getString("category");
                            product.barcode = rs.getString("barcode");
                            product.unit = rs.getString("unit");
                            product.description = rs.getString("description");
                            product.brand = rs.getString("brand");
                            product.supplier = rs.getString("supplier");
                            product.spec = rs.getString("spec");
                            product.minStock = rs.getInt("min_stock");
                            product.cost = rs.getDouble("cost");
                            product.version = rs.getInt("version");
                            productsMap.put(productId, product);
                        } else {
                            throw new SQLException("商品不存在，ID: " + productId);
                        }
                    }
                }
            }

            // 构建批量更新SQL（使用 CASE WHEN 语句）
            StringBuilder sql = new StringBuilder("UPDATE products SET quantity = CASE id ");
            StringBuilder whereClause = new StringBuilder(" WHERE id IN (");

            int index = 0;
            for (Map.Entry<Integer, Integer> entry : productUpdates.entrySet()) {
                int productId = entry.getKey();
                int quantityChange = entry.getValue();
                Product product = productsMap.get(productId);

                // 检查库存是否足够
                if (quantityChange < 0 && product.quantity < Math.abs(quantityChange)) {
                    throw new SQLException("商品 " + product.name + " 库存不足！当前库存: " + product.quantity + ", 需要扣减: " + Math.abs(quantityChange));
                }

                sql.append("WHEN ").append(productId).append(" THEN quantity + ").append(quantityChange);

                if (index > 0) {
                    whereClause.append(", ");
                }
                whereClause.append(productId);
                index++;
            }

            sql.append(" END, version = version + 1").append(whereClause).append(")");

            // 执行批量更新
            String finalSql = sql.toString();
            logger.debug("批量更新SQL: {}", finalSql);

            try (Statement stmt = conn.createStatement()) {
                int affectedRows = stmt.executeUpdate(finalSql);
                logger.info("批量更新库存完成，影响 {} 行", affectedRows);
            }

            DatabaseManager.commitTransaction(conn);
            
            // 清除缓存，强制下次从数据库重新加载
            com.cashier.util.CacheManager.clearCache();
            
            logger.info("批量更新库存成功，共更新 {} 个商品", productUpdates.size());
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                DatabaseManager.rollbackTransaction(conn);
            }
            logger.error("批量更新库存失败, SQL错误: {}", e.getMessage(), e);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("关闭数据库连接失败", e);
                }
            }
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
        double totalValue = inventory.values().stream().mapToDouble(p -> p.quantity * p.cost).sum();
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