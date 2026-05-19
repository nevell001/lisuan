package com.cashier.api.controller;

import com.cashier.api.ApiServer;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.Product;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品管理 REST API
 * 已重构为使用重构版 DAO
 */
public class ProductApiController {
    private static final Logger logger = LoggerFactory.getLogger(ProductApiController.class);
    private static final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
    
    /**
     * 获取商品列表
     * GET /api/products
     */
    public static void list(Context ctx) {
        try {
            String category = ctx.queryParam("category");
            String keyword = ctx.queryParam("keyword");
            
            List<Product> products;
            if (keyword != null && !keyword.isEmpty()) {
                products = productDAO.search(keyword);
            } else if (category != null && !category.isEmpty()) {
                products = productDAO.findByCategory(category);
            } else {
                products = productDAO.findAll();
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", products);
            result.put("total", products.size());
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取商品列表失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取商品列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取单个商品
     * GET /api/products/:id
     */
    public static void get(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            Product product = productDAO.findById(id);
            
            if (product == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "商品不存在"));
                return;
            }
            
            ctx.json(Map.of("success", true, "data", product));
        } catch (Exception e) {
            logger.error("获取商品详情失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取商品详情失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建商品
     * POST /api/products
     */
    public static void create(Context ctx) {
        try {
            ProductRequest request = ctx.bodyAsClass(ProductRequest.class);
            
            Product product = new Product();
            product.productCode = request.productCode != null ? request.productCode : "";
            product.name = request.name;
            product.price = request.price != null ? request.price : BigDecimal.ZERO;
            product.quantity = request.quantity != null ? request.quantity : 0;
            product.category = request.category != null ? request.category : "默认分类";
            product.barcode = request.barcode != null ? request.barcode : "";
            product.unit = request.unit != null ? request.unit : "个";
            product.description = request.description != null ? request.description : "";
            product.brand = request.brand != null ? request.brand : "";
            product.supplier = request.supplier != null ? request.supplier : "";
            product.spec = request.spec != null ? request.spec : "";
            product.minStock = request.minStock != null ? request.minStock : 10;
            product.cost = request.cost != null ? request.cost : BigDecimal.ZERO;
            
            productDAO.insert(product);
            
            logger.info("创建商品: {} ({})", product.name, product.productCode);
            ctx.status(HttpStatus.CREATED)
               .json(Map.of("success", true, "data", product, "message", "商品创建成功"));
        } catch (Exception e) {
            logger.error("创建商品失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "创建商品失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新商品
     * PUT /api/products/:id
     */
    public static void update(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            ProductRequest request = ctx.bodyAsClass(ProductRequest.class);
            
            Product product = productDAO.findById(id);
            if (product == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "商品不存在"));
                return;
            }
            
            if (request.name != null) product.name = request.name;
            if (request.price != null) product.price = request.price;
            if (request.quantity != null) product.quantity = request.quantity;
            if (request.category != null) product.category = request.category;
            if (request.barcode != null) product.barcode = request.barcode;
            if (request.unit != null) product.unit = request.unit;
            if (request.description != null) product.description = request.description;
            if (request.brand != null) product.brand = request.brand;
            if (request.supplier != null) product.supplier = request.supplier;
            if (request.spec != null) product.spec = request.spec;
            if (request.minStock != null) product.minStock = request.minStock;
            if (request.cost != null) product.cost = request.cost;
            if (request.productCode != null) product.productCode = request.productCode;
            
            productDAO.update(product);
            
            logger.info("更新商品: {} ({})", product.name, product.id);
            ctx.json(Map.of("success", true, "data", product, "message", "商品更新成功"));
        } catch (Exception e) {
            logger.error("更新商品失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "更新商品失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除商品
     * DELETE /api/products/:id
     */
    public static void delete(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            
            Product product = productDAO.findById(id);
            if (product == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "商品不存在"));
                return;
            }
            
            productDAO.delete(id);
            
            logger.info("删除商品: {} ({})", product.name, product.id);
            ctx.json(Map.of("success", true, "message", "商品删除成功"));
        } catch (Exception e) {
            logger.error("删除商品失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "删除商品失败: " + e.getMessage()));
        }
    }
    
    /**
     * 库存预警列表
     * GET /api/products/low-stock
     */
    public static void lowStock(Context ctx) {
        try {
            List<Product> products = productDAO.findLowStock();
            ctx.json(Map.of("success", true, "data", products, "total", products.size()));
        } catch (Exception e) {
            logger.error("获取低库存商品失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取低库存商品失败: " + e.getMessage()));
        }
    }
    
    /**
     * 商品请求DTO
     */
    public static class ProductRequest {
        public String productCode;
        public String name;
        public BigDecimal price;
        public Integer quantity;
        public String category;
        public String barcode;
        public String unit;
        public String description;
        public String brand;
        public String supplier;
        public String spec;
        public Integer minStock;
        public BigDecimal cost;
    }
}