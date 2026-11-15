package com.cashier.api.controller;

import com.cashier.api.controller.HoldOrderApiController.CreateHoldRequest;
import com.cashier.api.controller.HoldOrderApiController.HoldItemRequest;
import com.cashier.api.controller.HoldOrderApiController.HoldOrderView;
import com.cashier.api.controller.HoldOrderApiController.ResumeView;
import com.cashier.api.support.TestContext;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.HoldOrderDAORefactored;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.HoldOrder;
import com.cashier.model.Product;
import com.cashier.model.Promotion;
import com.cashier.model.User;
import com.cashier.util.DatabaseTestBase;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("挂单 REST API")
class HoldOrderApiControllerTest extends DatabaseTestBase {

    private static final ProductDAORefactored PRODUCT_DAO = DAOFactory.getInstance().getProductDAO();
    private static final HoldOrderDAORefactored HOLD_DAO = DAOFactory.getInstance().getHoldOrderDAO();
    private static final AtomicInteger SEQ = new AtomicInteger();

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    private static User user(int id, String role) {
        User user = new User();
        user.id = id;
        user.username = role + id;
        user.role = role;
        return user;
    }

    @Test
    @DisplayName("创建挂单时金额由服务端按会员/最优促销计算，客户端不参与")
    void createComputesServerSideTotals() throws Exception {
        Product product = insertProduct("10.00", 10);
        insertPromotion("打折", BigDecimal.ZERO, new BigDecimal("0.9"));

        CreateHoldRequest request = request(product.id, 2);

        TestContext ctx = post("/api/hold-orders", request, user(1, "cashier"));
        HoldOrderApiController.create(ctx.context);

        assertEquals(HttpStatus.CREATED, ctx.status);
        assertTrue((Boolean) body(ctx).get("success"));
        HoldOrderView view = (HoldOrderView) body(ctx).get("data");
        assertEquals(0, new BigDecimal("20.00").compareTo(view.totalAmount));
        assertEquals(0, new BigDecimal("2.00").compareTo(view.discountAmount), "9 折应优惠 2 元");
        assertEquals(0, new BigDecimal("18.00").compareTo(view.finalAmount));
        assertEquals("PENDING", view.status);
        assertNotNull(view.statusName);
        assertEquals(1, view.itemCount);
        assertEquals(1, view.items.size());
        assertEquals(0, new BigDecimal("18.00").compareTo(HOLD_DAO.findById(view.id).finalAmount),
            "落库金额必须与响应一致");
    }

    @Test
    @DisplayName("创建挂单的请求校验逐项返回 400")
    void createValidatesRequest() throws Exception {
        Product product = insertProduct("10.00", 1);

        assertBadRequest(post("/api/hold-orders", null, user(1, "cashier")));
        assertBadRequest(post("/api/hold-orders", new CreateHoldRequest(), user(1, "cashier")));

        CreateHoldRequest zeroQuantity = request(product.id, 0);
        assertBadRequest(post("/api/hold-orders", zeroQuantity, user(1, "cashier")));

        CreateHoldRequest duplicate = request(product.id, 1);
        duplicate.items = List.of(item(product.id, 1), item(product.id, 1));
        assertBadRequest(post("/api/hold-orders", duplicate, user(1, "cashier")));

        assertBadRequest(post("/api/hold-orders", request(99999999, 1), user(1, "cashier")));
        assertBadRequest(post("/api/hold-orders", request(product.id, 2), user(1, "cashier")));

        CreateHoldRequest missingMember = request(product.id, 1);
        missingMember.memberId = 99999999;
        assertBadRequest(post("/api/hold-orders", missingMember, user(1, "cashier")));

        CreateHoldRequest longNotes = request(product.id, 1);
        longNotes.notes = "x".repeat(501);
        assertBadRequest(post("/api/hold-orders", longNotes, user(1, "cashier")));

        CreateHoldRequest tooMany = request(product.id, 1);
        tooMany.items = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            tooMany.items.add(item(product.id, 1));
        }
        assertBadRequest(post("/api/hold-orders", tooMany, user(1, "cashier")));

        assertEquals(0, HOLD_DAO.count(null, null), "校验失败不应留下挂单");
    }

    @Test
    @DisplayName("收银员列表只看自己的，管理员看全部；status=all 不过滤")
    void listRespectsOwnershipAndStatusFilter() throws Exception {
        insertHold(1, 0, "[]");
        insertHold(1, 1, "[]");
        insertHold(2, 0, "[]");

        TestContext cashierMine = new TestContext().withAttribute("currentUser", user(1, "cashier"))
            .withRequest(HandlerType.GET, "/api/hold-orders").withQueryParam("status", "all");
        HoldOrderApiController.list(cashierMine.context);
        assertEquals(HttpStatus.OK, cashierMine.status);
        assertEquals(2, ((List<?>) body(cashierMine).get("data")).size());
        assertEquals(2, ((Number) body(cashierMine).get("total")).intValue());

        TestContext cashierPending = new TestContext().withAttribute("currentUser", user(1, "cashier"))
            .withRequest(HandlerType.GET, "/api/hold-orders");
        HoldOrderApiController.list(cashierPending.context);
        assertEquals(1, ((List<?>) body(cashierPending).get("data")).size(),
            "默认只看待取单");

        TestContext cashierAllWithSpaces = new TestContext().withAttribute("currentUser", user(1, "cashier"))
            .withRequest(HandlerType.GET, "/api/hold-orders").withQueryParam("status", " all ");
        HoldOrderApiController.list(cashierAllWithSpaces.context);
        assertEquals(HttpStatus.OK, cashierAllWithSpaces.status,
            "status= all  应等价于 all，不能空响应");
        assertEquals(2, ((List<?>) body(cashierAllWithSpaces).get("data")).size());

        TestContext admin = new TestContext().withAttribute("currentUser", user(9, "admin"))
            .withRequest(HandlerType.GET, "/api/hold-orders").withQueryParam("status", "all");
        HoldOrderApiController.list(admin.context);
        assertEquals(3, ((List<?>) body(admin).get("data")).size());

        TestContext invalid = new TestContext().withAttribute("currentUser", user(1, "cashier"))
            .withRequest(HandlerType.GET, "/api/hold-orders").withQueryParam("status", "9");
        HoldOrderApiController.list(invalid.context);
        assertEquals(HttpStatus.BAD_REQUEST, invalid.status);
    }

    @Test
    @DisplayName("详情按归属放行：本人/管理员可看，其他收银员 403")
    void getEnforcesOwnership() throws Exception {
        HoldOrder order = insertHold(2, 0, "[]");

        TestContext otherCashier = get(order.id, user(1, "cashier"));
        HoldOrderApiController.get(otherCashier.context);
        assertEquals(HttpStatus.FORBIDDEN, otherCashier.status);

        TestContext admin = get(order.id, user(9, "admin"));
        HoldOrderApiController.get(admin.context);
        assertEquals(HttpStatus.OK, admin.status);
        assertEquals(order.orderNumber,
            ((HoldOrderView) body(admin).get("data")).orderNumber);

        TestContext missing = get(99999999, user(1, "cashier"));
        HoldOrderApiController.get(missing.context);
        assertEquals(HttpStatus.NOT_FOUND, missing.status);

        TestContext invalid = new TestContext().withAttribute("currentUser", user(1, "cashier"))
            .withRequest(HandlerType.GET, "/api/hold-orders/abc").withPathParam("id", "abc");
        HoldOrderApiController.get(invalid.context);
        assertEquals(HttpStatus.BAD_REQUEST, invalid.status);
    }

    @Test
    @DisplayName("取单成功返回可恢复明细，重复取单 409")
    void resumeReturnsItemsAndRejectsDoubleResume() throws Exception {
        Product product = insertProduct("10.00", 10);
        HoldOrder order = insertHold(1, 0, "[{\"productId\":" + product.id + ",\"quantity\":2}]");

        TestContext first = post("/api/hold-orders/" + order.id + "/resume", null, user(1, "cashier"));
        first = first.withPathParam("id", String.valueOf(order.id));
        HoldOrderApiController.resume(first.context);

        assertEquals(HttpStatus.OK, first.status);
        ResumeView view = (ResumeView) body(first).get("data");
        assertEquals("RESUMED", view.status);
        assertEquals(1, view.items.size());
        assertEquals(2, view.items.get(0).quantity);
        assertEquals(0, new BigDecimal("20.00").compareTo(view.items.get(0).subtotal));
        assertTrue(view.missingProductIds.isEmpty());
        assertEquals(1, HOLD_DAO.findById(order.id).status);

        TestContext second = post("/api/hold-orders/" + order.id + "/resume", null, user(1, "cashier"));
        second = second.withPathParam("id", String.valueOf(order.id));
        HoldOrderApiController.resume(second.context);
        assertEquals(HttpStatus.CONFLICT, second.status);
        assertEquals("RESUMED", body(second).get("currentStatus"));
    }

    @Test
    @DisplayName("取单时已删除的商品只报告缺失，不阻塞其余明细")
    void resumeReportsMissingProducts() throws Exception {
        Product product = insertProduct("10.00", 10);
        HoldOrder order = insertHold(1, 0,
            "[{\"productId\":" + product.id + ",\"quantity\":1},{\"productId\":99999999,\"quantity\":2}]");

        TestContext ctx = post("/api/hold-orders/" + order.id + "/resume", null, user(1, "cashier"));
        ctx = ctx.withPathParam("id", String.valueOf(order.id));
        HoldOrderApiController.resume(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        ResumeView view = (ResumeView) body(ctx).get("data");
        assertEquals(1, view.items.size());
        assertEquals(List.of(99999999), view.missingProductIds);
    }

    @Test
    @DisplayName("取消挂单成功，重复取消 409；他人挂单 403")
    void cancelEnforcesStateAndOwnership() throws Exception {
        HoldOrder order = insertHold(1, 0, "[]");

        TestContext other = post("/api/hold-orders/" + order.id + "/cancel", null, user(2, "cashier"))
            .withPathParam("id", String.valueOf(order.id));
        HoldOrderApiController.cancel(other.context);
        assertEquals(HttpStatus.FORBIDDEN, other.status);

        TestContext ctx = post("/api/hold-orders/" + order.id + "/cancel", null, user(1, "cashier"))
            .withPathParam("id", String.valueOf(order.id));
        HoldOrderApiController.cancel(ctx.context);
        assertEquals(HttpStatus.OK, ctx.status);
        assertEquals("CANCELLED", ((HoldOrderView) body(ctx).get("data")).status);
        assertEquals(2, HOLD_DAO.findById(order.id).status);

        TestContext again = post("/api/hold-orders/" + order.id + "/cancel", null, user(1, "cashier"))
            .withPathParam("id", String.valueOf(order.id));
        HoldOrderApiController.cancel(again.context);
        assertEquals(HttpStatus.CONFLICT, again.status);
    }

    private static TestContext post(String path, Object requestBody, User user) {
        TestContext ctx = new TestContext().withAttribute("currentUser", user)
            .withRequest(HandlerType.POST, path);
        if (requestBody != null) {
            ctx.withBody(requestBody);
        }
        return ctx;
    }

    private static TestContext get(int id, User user) {
        return new TestContext().withAttribute("currentUser", user)
            .withRequest(HandlerType.GET, "/api/hold-orders/" + id)
            .withPathParam("id", String.valueOf(id));
    }

    private static void assertBadRequest(TestContext ctx) {
        HoldOrderApiController.create(ctx.context);
        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    private static CreateHoldRequest request(int productId, int quantity) {
        CreateHoldRequest request = new CreateHoldRequest();
        request.items = List.of(item(productId, quantity));
        request.notes = "测试挂单";
        return request;
    }

    private static HoldItemRequest item(int productId, int quantity) {
        HoldItemRequest item = new HoldItemRequest();
        item.productId = productId;
        item.quantity = quantity;
        return item;
    }

    private static Product insertProduct(String price, int quantity) throws Exception {
        int seq = SEQ.incrementAndGet();
        Product product = new Product(0, "HOLD-API-" + seq, "挂单接口商品" + seq,
            Double.parseDouble(price), quantity, "测试分类", "HOLD-API-BAR-" + seq, "件",
            "描述", "品牌", "供应商", "规格", 0, 5.0);
        assertTrue(PRODUCT_DAO.insert(product));
        assertNotNull(product.id);
        return product;
    }

    private static HoldOrder insertHold(int userId, int status, String itemsJson) throws Exception {
        HoldOrder order = new HoldOrder();
        order.orderNumber = HoldOrder.generateOrderNumber();
        order.userId = userId;
        order.totalAmount = new BigDecimal("20.00");
        order.finalAmount = new BigDecimal("20.00");
        order.discountAmount = BigDecimal.ZERO;
        order.itemCount = 1;
        order.itemsJson = itemsJson;
        order.notes = "";
        order.status = status;
        HOLD_DAO.insert(order);
        assertFalse(order.id == null || order.id <= 0);
        return order;
    }

    private static void insertPromotion(String legacyType, BigDecimal threshold, BigDecimal discount)
            throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Promotion promotion = new Promotion(0, "P-" + System.nanoTime(), "测试促销", legacyType,
            threshold, discount, "", true, now.minusDays(1), now.plusDays(1), 0, -1);
        assertTrue(DAOFactory.getInstance().getPromotionDAO().insert(promotion));
    }
}
