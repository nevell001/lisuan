package com.cashier.api.controller;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.PageResult;
import com.cashier.model.Product;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 商品管理 REST API
 * 已重构为使用重构版 DAO
 */
public class ProductApiController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ProductApiController.class);
    private static final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
    
    /**
     * 获取商品列表
     * GET /api/products
     */
    public static void list(Context ctx) {
        try {
            String category = ctx.queryParam("category");
            String keyword = ctx.queryParam("keyword");
            ApiPagination.PageRequest page = ApiPagination.from(ctx);

            PageResult<Product> products;
            if (keyword != null && !keyword.isEmpty()) {
                products = productDAO.search(keyword, page.page(), page.pageSize());
            } else if (category != null && !category.isEmpty()) {
                products = productDAO.findByCategory(category, page.page(), page.pageSize());
            } else {
                products = productDAO.findAll(page.page(), page.pageSize());
            }

            ctx.json(ApiPagination.success(products));
        } catch (Exception e) {
            logger.error("获取商品列表失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message",
                   com.cashier.api.ApiMessages.text(ctx, "api.product.list_failed")));
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
                   .json(Map.of("success", false, "message",
                       com.cashier.api.ApiMessages.text(ctx, "api.product.not_found")));
                return;
            }
            
            ctx.json(Map.of("success", true, "data", product));
        } catch (Exception e) {
            logger.error("获取商品详情失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message",
                   com.cashier.api.ApiMessages.text(ctx, "api.product.detail_failed")));
        }
    }
    
    /**
     * 创建商品
     * POST /api/products
     */
    public static void create(Context ctx) {
        try {
            ProductRequest request = ApiRequest.parse(ctx, ProductRequest.class);
            if (request == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message",
                       com.cashier.api.ApiMessages.text(ctx, "api.common.bad_request")));
                return;
            }
            
            // 必填字段就地校验：留到落库才由 DAO 抛 SQLException 的话，
            // 会被下面的 catch 兜成 500「创建商品失败」，客户端看不出少了哪个字段
            if (request.productCode == null || request.productCode.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message",
                       com.cashier.api.ApiMessages.text(ctx, "api.product.missing_field", "productCode")));
                return;
            }
            if (request.name == null || request.name.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message",
                       com.cashier.api.ApiMessages.text(ctx, "api.product.missing_field", "name")));
                return;
            }
            
            Product product = new Product();
            // productCode 已在上方校验过非空，这里不再需要兜底
            product.productCode = request.productCode;
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
               .json(Map.of("success", true, "data", product, "message",
                   com.cashier.api.ApiMessages.text(ctx, "api.product.created")));
        } catch (Exception e) {
            logger.error("创建商品失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message",
                   com.cashier.api.ApiMessages.text(ctx, "api.product.create_failed")));
        }
    }
    
    /**
     * 更新商品
     * PUT /api/products/:id
     */
    public static void update(Context ctx) {
        try {
            int id = ctx.pathParamAsClass("id", Integer.class).get();
            ProductRequest request = ApiRequest.parse(ctx, ProductRequest.class);
            if (request == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message",
                       com.cashier.api.ApiMessages.text(ctx, "api.common.bad_request")));
                return;
            }
            
            Product product = productDAO.findById(id);
            if (product == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message",
                       com.cashier.api.ApiMessages.text(ctx, "api.product.not_found")));
                return;
            }
            
            applyProductUpdates(product, request);
            
            if (!productDAO.update(product)) {
                // 乐观锁：version 不匹配说明并发修改已提交，不能回 success 让调用方以为写成功了
                logger.warn("更新商品冲突（乐观锁未命中）: {} ({})", product.name, product.id);
                ctx.status(HttpStatus.CONFLICT)
                   .json(Map.of("success", false, "message",
                       com.cashier.api.ApiMessages.text(ctx, "api.product.update_conflict")));
                return;
            }
            
            logger.info("更新商品: {} ({})", product.name, product.id);
            ctx.json(Map.of("success", true, "data", product, "message",
                com.cashier.api.ApiMessages.text(ctx, "api.product.updated")));
        } catch (Exception e) {
            logger.error("更新商品失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message",
                   com.cashier.api.ApiMessages.text(ctx, "api.product.update_failed")));
        }
    }

    private static void applyProductUpdates(Product product, ProductRequest request) {
        updateTextFields(product, request);
        updateStockFields(product, request);
        updatePriceFields(product, request);
    }

    private static void updateTextFields(Product product, ProductRequest request) {
        if (request.name != null) product.name = request.name;
        if (request.category != null) product.category = request.category;
        if (request.barcode != null) product.barcode = request.barcode;
        if (request.unit != null) product.unit = request.unit;
        if (request.description != null) product.description = request.description;
        if (request.brand != null) product.brand = request.brand;
        if (request.supplier != null) product.supplier = request.supplier;
        if (request.spec != null) product.spec = request.spec;
        if (request.productCode != null) product.productCode = request.productCode;
    }

    private static void updateStockFields(Product product, ProductRequest request) {
        if (request.quantity != null) product.quantity = request.quantity;
        if (request.minStock != null) product.minStock = request.minStock;
    }

    private static void updatePriceFields(Product product, ProductRequest request) {
        if (request.price != null) product.price = request.price;
        if (request.cost != null) product.cost = request.cost;
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
                   .json(Map.of("success", false, "message",
                       com.cashier.api.ApiMessages.text(ctx, "api.product.not_found")));
                return;
            }
            
            productDAO.delete(id);
            
            logger.info("删除商品: {} ({})", product.name, product.id);
            ctx.json(Map.of("success", true, "message",
                com.cashier.api.ApiMessages.text(ctx, "api.product.deleted")));
        } catch (Exception e) {
            logger.error("删除商品失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message",
                   com.cashier.api.ApiMessages.text(ctx, "api.product.delete_failed")));
        }
    }
    
    /**
     * 库存预警列表
     * GET /api/products/low-stock
     */
    public static void lowStock(Context ctx) {
        try {
            ApiPagination.PageRequest page = ApiPagination.from(ctx);
            PageResult<Product> products = productDAO.findLowStock(page.page(), page.pageSize());
            ctx.json(ApiPagination.success(products));
        } catch (Exception e) {
            logger.error("获取低库存商品失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message",
                   com.cashier.api.ApiMessages.text(ctx, "api.product.low_stock_failed")));
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
