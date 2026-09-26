package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import com.cashier.dao.DAOFactory;
import com.cashier.model.Product;
import com.cashier.util.DatabaseTestBase;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryApiControllerTest extends DatabaseTestBase {

    private Product insertProduct(String name, int quantity, int minStock) throws Exception {
        Product product = new Product();
        product.productCode = "INV-" + name;
        product.name = name;
        product.price = BigDecimal.valueOf(5);
        product.quantity = quantity;
        product.minStock = minStock;
        product.category = "库存分类";
        product.unit = "件";
        assertTrue(DAOFactory.getInstance().getProductDAO().insert(product));
        return product;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("库存列表返回分页数据")
    void listInventory() throws Exception {
        insertProduct("库存商品", 20, 5);

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/inventory");
        InventoryApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("库存预警只返回低库存商品")
    void alertsReturnsLowStock() throws Exception {
        insertProduct("预警商品", 3, 10);
        insertProduct("正常商品", 50, 10);

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/inventory/alerts");
        InventoryApiController.alerts(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("alert"));
    }

    @Test
    @DisplayName("更新库存数量")
    void updateStockSetsQuantity() throws Exception {
        Product saved = insertProduct("改库存商品", 10, 5);
        InventoryApiController.StockRequest request = new InventoryApiController.StockRequest();
        request.quantity = 66;

        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/inventory/1")
            .withPathParam("id", String.valueOf(saved.id))
            .withBody(request);
        InventoryApiController.updateStock(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
        assertEquals(66, DAOFactory.getInstance().getProductDAO().findById(saved.id).quantity);
    }

    @Test
    @DisplayName("更新库存：不得把库存调成负数（400）")
    void updateStockRejectsNegativeQuantity() throws Exception {
        Product saved = insertProduct("负库存商品", 10, 5);

        InventoryApiController.StockRequest negative = new InventoryApiController.StockRequest();
        negative.adjustment = -999;
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/inventory/1")
            .withPathParam("id", String.valueOf(saved.id))
            .withBody(negative);
        InventoryApiController.updateStock(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status, "库存不得被调成负数");
        assertEquals(10, DAOFactory.getInstance().getProductDAO().findById(saved.id).quantity);
    }

    @Test
    @DisplayName("更新不存在的库存返回 404")
    void updateStockMissingReturns404() {
        InventoryApiController.StockRequest request = new InventoryApiController.StockRequest();
        request.quantity = 1;

        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/inventory/999999")
            .withPathParam("id", "999999")
            .withBody(request);
        InventoryApiController.updateStock(ctx.context);

        assertEquals(HttpStatus.NOT_FOUND, ctx.status);
    }

    @Test
    @DisplayName("库存盘点汇总返回各项数量")
    void checkReturnsSummary() throws Exception {
        insertProduct("盘点商品", 3, 10);

        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/inventory/check");
        InventoryApiController.check(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
        assertTrue((Long) response(ctx).get("totalProducts") > 0);
    }

    @Test
    @DisplayName("空请求体更新库存返回 400")
    void updateStockWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/inventory/1")
            .withPathParam("id", "1");
        InventoryApiController.updateStock(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }
}
