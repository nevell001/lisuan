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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductApiControllerTest extends DatabaseTestBase {

    private Product insertProduct(String name) throws Exception {
        Product product = new Product();
        product.productCode = "P-" + name;
        product.name = name;
        product.price = BigDecimal.valueOf(9.9);
        product.quantity = 20;
        product.category = "测试分类";
        product.unit = "个";
        assertTrue(DAOFactory.getInstance().getProductDAO().insert(product));
        return product;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("商品列表：无条件返回全部")
    void listProductsWithoutFilters() throws Exception {
        insertProduct("列表商品A");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/products");
        ProductApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
        assertNotNull(response(ctx).get("data"));
    }

    @Test
    @DisplayName("商品列表：关键词搜索")
    void listProductsByKeyword() throws Exception {
        insertProduct("搜索目标商品");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/products")
            .withQueryParam("keyword", "搜索目标");
        ProductApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("商品列表：按分类过滤")
    void listProductsByCategory() throws Exception {
        insertProduct("分类商品");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/products")
            .withQueryParam("category", "测试分类");
        ProductApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("商品详情：返回已保存商品")
    void getProductReturnsData() throws Exception {
        Product saved = insertProduct("详情商品");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/products/1")
            .withPathParam("id", String.valueOf(saved.id));
        ProductApiController.get(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        Product data = (Product) response(ctx).get("data");
        assertEquals(saved.id, data.id);
    }

    @Test
    @DisplayName("商品详情：不存在返回 404")
    void getMissingProductReturns404() {
        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/products/999999")
            .withPathParam("id", "999999");
        ProductApiController.get(ctx.context);

        assertEquals(HttpStatus.NOT_FOUND, ctx.status);
        assertFalse((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("创建商品返回 201")
    void createProductReturnsCreated() {
        ProductApiController.ProductRequest request = new ProductApiController.ProductRequest();
        request.productCode = "P-NEW-001";
        request.name = "新建商品";
        request.price = BigDecimal.valueOf(5);

        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/products").withBody(request);
        ProductApiController.create(ctx.context);

        assertEquals(HttpStatus.CREATED, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("更新商品并持久化")
    void updateProductAppliesChanges() throws Exception {
        Product saved = insertProduct("待更新商品");
        ProductApiController.ProductRequest request = new ProductApiController.ProductRequest();
        request.name = "更新后的商品";

        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/products/1")
            .withPathParam("id", String.valueOf(saved.id))
            .withBody(request);
        ProductApiController.update(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
        assertEquals("更新后的商品", DAOFactory.getInstance().getProductDAO().findById(saved.id).name);
    }

    @Test
    @DisplayName("更新不存在的商品返回 404")
    void updateMissingProductReturns404() {
        ProductApiController.ProductRequest request = new ProductApiController.ProductRequest();
        request.name = "x";

        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/products/999999")
            .withPathParam("id", "999999")
            .withBody(request);
        ProductApiController.update(ctx.context);

        assertEquals(HttpStatus.NOT_FOUND, ctx.status);
    }

    @Test
    @DisplayName("删除商品后记录不存在")
    void deleteProductRemovesIt() throws Exception {
        Product saved = insertProduct("待删除商品");

        TestContext ctx = new TestContext().withRequest(HandlerType.DELETE, "/api/products/1")
            .withPathParam("id", String.valueOf(saved.id));
        ProductApiController.delete(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
        assertNull(DAOFactory.getInstance().getProductDAO().findById(saved.id));
    }

    @Test
    @DisplayName("删除不存在的商品返回 404")
    void deleteMissingProductReturns404() {
        TestContext ctx = new TestContext().withRequest(HandlerType.DELETE, "/api/products/999999")
            .withPathParam("id", "999999");
        ProductApiController.delete(ctx.context);

        assertEquals(HttpStatus.NOT_FOUND, ctx.status);
    }

    @Test
    @DisplayName("低库存列表返回成功")
    void lowStockReturnsSummary() throws Exception {
        insertProduct("低库存商品");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/products/low-stock");
        ProductApiController.lowStock(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("空请求体创建商品返回 400")
    void createWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/products");
        ProductApiController.create(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
        assertFalse((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("创建商品：负价格/负库存返回 400，且不落库")
    void createRejectsNegativeValues() throws Exception {
        ProductApiController.ProductRequest negativePrice = new ProductApiController.ProductRequest();
        negativePrice.productCode = "P-NEG-1";
        negativePrice.name = "负价格商品";
        negativePrice.price = new BigDecimal("-0.01");
        TestContext priceCtx = new TestContext().withRequest(HandlerType.POST, "/api/products")
            .withBody(negativePrice);
        ProductApiController.create(priceCtx.context);
        assertEquals(HttpStatus.BAD_REQUEST, priceCtx.status);
        assertNull(DAOFactory.getInstance().getProductDAO().findByProductCode("P-NEG-1"));

        ProductApiController.ProductRequest negativeStock = new ProductApiController.ProductRequest();
        negativeStock.productCode = "P-NEG-2";
        negativeStock.name = "负库存商品";
        negativeStock.quantity = -5;
        TestContext stockCtx = new TestContext().withRequest(HandlerType.POST, "/api/products")
            .withBody(negativeStock);
        ProductApiController.create(stockCtx.context);
        assertEquals(HttpStatus.BAD_REQUEST, stockCtx.status);
        assertNull(DAOFactory.getInstance().getProductDAO().findByProductCode("P-NEG-2"));
    }

    @Test
    @DisplayName("创建商品：重名/重编号返回 409 而不是 500")
    void createDuplicateReturnsConflict() throws Exception {
        insertProduct("重名目标商品");

        ProductApiController.ProductRequest duplicateName = new ProductApiController.ProductRequest();
        duplicateName.productCode = "P-UNIQUE-1";
        duplicateName.name = "重名目标商品";
        TestContext nameCtx = new TestContext().withRequest(HandlerType.POST, "/api/products")
            .withBody(duplicateName);
        ProductApiController.create(nameCtx.context);
        assertEquals(HttpStatus.CONFLICT, nameCtx.status);

        ProductApiController.ProductRequest duplicateCode = new ProductApiController.ProductRequest();
        duplicateCode.productCode = "P-重名目标商品";
        duplicateCode.name = "另一个商品";
        TestContext codeCtx = new TestContext().withRequest(HandlerType.POST, "/api/products")
            .withBody(duplicateCode);
        ProductApiController.create(codeCtx.context);
        assertEquals(HttpStatus.CONFLICT, codeCtx.status);
    }

    @Test
    @DisplayName("更新商品：改成已存在的名称返回 409，且原记录不变")
    void updateToDuplicateNameReturnsConflict() throws Exception {
        insertProduct("已占用名称");
        Product target = insertProduct("待改名商品");

        ProductApiController.ProductRequest request = new ProductApiController.ProductRequest();
        request.name = "已占用名称";
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/products/" + target.id)
            .withPathParam("id", String.valueOf(target.id))
            .withBody(request);
        ProductApiController.update(ctx.context);

        assertEquals(HttpStatus.CONFLICT, ctx.status);
        assertEquals("待改名商品", DAOFactory.getInstance().getProductDAO().findById(target.id).name);
    }

    @Test
    @DisplayName("更新商品：负成本返回 400")
    void updateRejectsNegativeCost() throws Exception {
        Product target = insertProduct("负成本商品");

        ProductApiController.ProductRequest request = new ProductApiController.ProductRequest();
        request.cost = new BigDecimal("-1");
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/products/" + target.id)
            .withPathParam("id", String.valueOf(target.id))
            .withBody(request);
        ProductApiController.update(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }
}
