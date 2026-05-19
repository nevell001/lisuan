package com.cashier.api.controller;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.Product;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存管理 REST API
 * 已重构为使用重构版 DAO
 */
public class InventoryApiController {
    private static final Logger logger = LoggerFactory.getLogger(InventoryApiController.class);
    private static final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
    
    /**
     * 库存列表
     * GET /api/inventory
     */
    public static void list(Context ctx) {
        try {
            List<Product> products = productDAO.findAll();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", products);
            result.put("total", products.size());
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取库存列表失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取库存列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 库存预警列表
     * GET /api/inventory/alerts
     */
    public static void alerts(Context ctx) {
        try {
            List<Product> products = productDAO.findLowStock();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", products);
            result.put("total", products.size());
            result.put("alert", true);
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取库存预警失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取库存预警失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新库存
     * PUT /api/inventory/:id
     */
    public static void updateStock(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            StockRequest request = ctx.bodyAsClass(StockRequest.class);
            
            Product product = productDAO.findById(id);
            if (product == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "商品不存在"));
                return;
            }
            
            if (request.quantity != null) {
                product.quantity = request.quantity;
            } else if (request.adjustment != null) {
                product.quantity += request.adjustment;
            }
            
            productDAO.update(product);
            
            logger.info("更新库存: {} -> {}", product.name, product.quantity);
            ctx.json(Map.of("success", true, "data", product, "message", "库存更新成功"));
        } catch (Exception e) {
            logger.error("更新库存失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "更新库存失败: " + e.getMessage()));
        }
    }
    
    /**
     * 库存盘点检查
     * POST /api/inventory/check
     */
    public static void check(Context ctx) {
        try {
            List<Product> products = productDAO.findAll();
            int totalProducts = products.size();
            int lowStockCount = 0;
            int zeroStockCount = 0;
            
            for (Product p : products) {
                if (p.quantity <= 0) zeroStockCount++;
                else if (p.quantity < p.minStock) lowStockCount++;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("totalProducts", totalProducts);
            result.put("lowStockCount", lowStockCount);
            result.put("zeroStockCount", zeroStockCount);
            result.put("healthyCount", totalProducts - lowStockCount - zeroStockCount);
            
            ctx.json(result);
        } catch (Exception e) {
            logger.error("库存盘点失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "库存盘点失败: " + e.getMessage()));
        }
    }
    
    /**
     * 库存请求 DTO
     */
    public static class StockRequest {
        public Integer quantity;
        public Integer adjustment;
    }
}