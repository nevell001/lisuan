package com.cashier.api.controller;

import com.cashier.api.controller.HoldOrderApiController.HoldItemRequest;
import com.cashier.api.controller.PromotionApiController.AppliedPromotionView;
import com.cashier.api.controller.PromotionApiController.PromotionRequest;
import com.cashier.api.controller.PromotionApiController.PromotionView;
import com.cashier.api.controller.PromotionApiController.QuoteRequest;
import com.cashier.api.controller.PromotionApiController.QuoteView;
import com.cashier.api.support.TestContext;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.PromotionDAORefactored;
import com.cashier.model.Member;
import com.cashier.model.Product;
import com.cashier.model.Promotion;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("促销 REST API")
class PromotionApiControllerTest extends DatabaseTestBase {

    private static final PromotionDAORefactored DAO = DAOFactory.getInstance().getPromotionDAO();
    private static final AtomicInteger SEQ = new AtomicInteger();

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("创建促销：对外代码落库为中文类型，日期归一到整天，编号由服务端生成")
    void createMapsTypeAndNormalizesDates() throws Exception {
        PromotionRequest request = validRequest("满100减10", "reduction");
        request.threshold = new BigDecimal("100");
        request.discount = new BigDecimal("10");

        TestContext ctx = post("/api/promotions", request);
        PromotionApiController.create(ctx.context);

        assertEquals(HttpStatus.CREATED, ctx.status);
        PromotionView view = (PromotionView) body(ctx).get("data");
        assertEquals("REDUCTION", view.type);
        assertNotNull(view.typeName);
        assertEquals(0, new BigDecimal("100").compareTo(view.threshold));
        assertEquals("2026-01-02", view.startDate);
        assertEquals("2026-02-03", view.endDate);
        assertEquals(-1, view.maxUsage);
        assertTrue(view.promotionCode.startsWith("P"));

        Promotion saved = DAO.findById(view.id);
        assertEquals("满减", saved.type);
        assertEquals(java.time.LocalTime.MIN, saved.startDate.toLocalTime());
        assertEquals(23, saved.endDate.getHour());
        assertEquals(59, saved.endDate.getMinute());
        assertEquals(-1, saved.maxUsage);
    }

    @Test
    @DisplayName("客户端回传的 promotionCode/usageCount 不采纳")
    void createIgnoresServerOwnedFields() throws Exception {
        PromotionRequest request = validRequest("满50减5", "REDUCTION");
        request.threshold = new BigDecimal("50");
        request.discount = new BigDecimal("5");
        request.promotionCode = "CLIENT-CODE";
        request.usageCount = 99;

        TestContext ctx = post("/api/promotions", request);
        PromotionApiController.create(ctx.context);

        assertEquals(HttpStatus.CREATED, ctx.status);
        Promotion saved = DAO.findById(((PromotionView) body(ctx).get("data")).id);
        assertEquals("P", saved.promotionCode.substring(0, 1));
        assertNotEquals("CLIENT-CODE", saved.promotionCode);
        assertEquals(0, saved.usageCount);
    }

    @Test
    @DisplayName("创建促销的请求校验逐项返回 400")
    void createValidatesRequest() throws Exception {
        assertBadRequest(null);

        PromotionRequest blankName = validRequest("  ", "REDUCTION");
        blankName.threshold = BigDecimal.TEN;
        blankName.discount = BigDecimal.ONE;
        assertBadRequest(blankName);

        PromotionRequest badType = validRequest("测试促销", "BOGUS");
        badType.threshold = BigDecimal.TEN;
        badType.discount = BigDecimal.ONE;
        assertBadRequest(badType);

        PromotionRequest missingThreshold = validRequest("测试促销", "REDUCTION");
        missingThreshold.discount = BigDecimal.ONE;
        assertBadRequest(missingThreshold);

        PromotionRequest negativeThreshold = validRequest("测试促销", "REDUCTION");
        negativeThreshold.threshold = new BigDecimal("-1");
        negativeThreshold.discount = BigDecimal.ONE;
        assertBadRequest(negativeThreshold);

        PromotionRequest badDiscount = validRequest("测试促销", "REDUCTION");
        badDiscount.threshold = BigDecimal.TEN;
        badDiscount.discount = BigDecimal.ZERO;
        assertBadRequest(badDiscount);

        PromotionRequest overThreshold = validRequest("测试促销", "REDUCTION");
        overThreshold.threshold = new BigDecimal("5");
        overThreshold.discount = new BigDecimal("10");
        assertBadRequest(overThreshold);

        PromotionRequest badRate = validRequest("测试促销", "DISCOUNT");
        badRate.threshold = BigDecimal.ZERO;
        badRate.discount = BigDecimal.ONE;
        assertBadRequest(badRate);

        PromotionRequest badQuantity = validRequest("测试促销", "COUPON");
        badQuantity.discount = BigDecimal.ONE;
        badQuantity.maxUsage = 0;
        assertBadRequest(badQuantity);

        PromotionRequest missingDate = validRequest("测试促销", "COUPON");
        missingDate.discount = BigDecimal.ONE;
        missingDate.startDate = null;
        assertBadRequest(missingDate);

        PromotionRequest badDate = validRequest("测试促销", "COUPON");
        badDate.discount = BigDecimal.ONE;
        badDate.startDate = "2026/01/01";
        assertBadRequest(badDate);

        PromotionRequest reversed = validRequest("测试促销", "COUPON");
        reversed.discount = BigDecimal.ONE;
        reversed.startDate = "2026-02-01";
        reversed.endDate = "2026-01-01";
        assertBadRequest(reversed);

        assertEquals(0, DAO.findPage(1, 100, null, null, false).getTotal(), "校验失败不应落库");
    }

    @Test
    @DisplayName("更新只改传入字段，编号与使用次数保持不变")
    void updateAppliesPartialChanges() throws Exception {
        Promotion promotion = insert("DELETE", "原促销", "满减", new BigDecimal("100"),
            new BigDecimal("10"), true, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));

        PromotionRequest request = new PromotionRequest();
        request.name = "改名后的促销";
        request.enabled = false;
        // 客户端把 GET 结果整包回传：只读字段必须被忽略而不是当成未知字段拒绝
        request.id = promotion.id;
        request.promotionCode = "CLIENT-CODE";
        request.usageCount = 99;
        request.typeName = "满减";
        request.active = true;

        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/promotions/" + promotion.id)
            .withPathParam("id", String.valueOf(promotion.id)).withBody(request);
        PromotionApiController.update(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        Promotion saved = DAO.findById(promotion.id);
        assertEquals("改名后的促销", saved.name);
        assertFalse(saved.enabled);
        assertEquals(promotion.promotionCode, saved.promotionCode);
        assertEquals(promotion.usageCount, saved.usageCount);
        assertEquals("满减", saved.type);
    }

    @Test
    @DisplayName("列表按类型/启用状态过滤，非法查询参数返回 400")
    void listFiltersAndValidatesQueryParams() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insert("L-R", "满减促销", "满减", BigDecimal.TEN, BigDecimal.ONE, true,
            now.minusDays(1), now.plusDays(1));
        insert("L-D", "打折促销", "打折", BigDecimal.ZERO, new BigDecimal("0.9"), true,
            now.minusDays(1), now.plusDays(1));
        insert("L-OFF", "停用促销", "优惠券", BigDecimal.ZERO, new BigDecimal("5"), false,
            now.minusDays(1), now.plusDays(1));

        TestContext byType = new TestContext().withRequest(HandlerType.GET, "/api/promotions")
            .withQueryParam("type", "discount");
        PromotionApiController.list(byType.context);
        assertEquals(HttpStatus.OK, byType.status);
        assertEquals(1, ((List<?>) body(byType).get("data")).size());
        assertEquals("DISCOUNT", ((PromotionView) ((List<?>) body(byType).get("data")).get(0)).type);

        TestContext enabled = new TestContext().withRequest(HandlerType.GET, "/api/promotions")
            .withQueryParam("enabled", "false");
        PromotionApiController.list(enabled.context);
        assertEquals(1, ((List<?>) body(enabled).get("data")).size());

        TestContext all = new TestContext().withRequest(HandlerType.GET, "/api/promotions");
        PromotionApiController.list(all.context);
        assertEquals(3, ((Number) body(all).get("total")).intValue());

        TestContext badType = new TestContext().withRequest(HandlerType.GET, "/api/promotions")
            .withQueryParam("type", "BOGUS");
        PromotionApiController.list(badType.context);
        assertEquals(HttpStatus.BAD_REQUEST, badType.status);

        TestContext badBoolean = new TestContext().withRequest(HandlerType.GET, "/api/promotions")
            .withQueryParam("enabled", "maybe");
        PromotionApiController.list(badBoolean.context);
        assertEquals(HttpStatus.BAD_REQUEST, badBoolean.status);
    }

    @Test
    @DisplayName("active 只返回启用且在有效期内的促销")
    void activeReturnsOnlyUsablePromotions() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insert("A-OK", "有效促销", "满减", BigDecimal.ZERO, BigDecimal.ONE, true,
            now.minusDays(1), now.plusDays(1));
        insert("A-OFF", "停用促销", "满减", BigDecimal.ZERO, BigDecimal.ONE, false,
            now.minusDays(1), now.plusDays(1));
        insert("A-OLD", "过期促销", "满减", BigDecimal.ZERO, BigDecimal.ONE, true,
            now.minusDays(10), now.minusDays(1));

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/promotions/active");
        PromotionApiController.active(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        List<?> data = (List<?>) body(ctx).get("data");
        assertEquals(1, data.size());
        assertEquals("A-OK", ((PromotionView) data.get(0)).promotionCode);
        assertTrue(((PromotionView) data.get(0)).active);
    }

    @Test
    @DisplayName("详情/删除的 404 与非法 ID 的 400")
    void getAndDeleteHandleMissingRecords() throws Exception {
        TestContext missing = new TestContext().withRequest(HandlerType.GET, "/api/promotions/99999999")
            .withPathParam("id", "99999999");
        PromotionApiController.get(missing.context);
        assertEquals(HttpStatus.NOT_FOUND, missing.status);

        TestContext invalid = new TestContext().withRequest(HandlerType.GET, "/api/promotions/abc")
            .withPathParam("id", "abc");
        PromotionApiController.get(invalid.context);
        assertEquals(HttpStatus.BAD_REQUEST, invalid.status);

        Promotion promotion = insert("D-1", "待删除促销", "满减", BigDecimal.ZERO, BigDecimal.ONE, true,
            LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));

        TestContext deleted = delete(promotion.id);
        PromotionApiController.delete(deleted.context);
        assertEquals(HttpStatus.OK, deleted.status);
        assertNull(DAO.findById(promotion.id));

        TestContext again = delete(promotion.id);
        PromotionApiController.delete(again.context);
        assertEquals(HttpStatus.NOT_FOUND, again.status);
    }

    @Test
    @DisplayName("试算按会员折扣 + 最优促销计价，不写库也不占用促销次数")
    void quoteAppliesMemberAndBestPromotion() throws Exception {
        Product product = insertProduct("50.00", 10);
        Promotion promotion = insert("Q-R", "试算满减", "满减", new BigDecimal("100"),
            new BigDecimal("10"), true, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        Member member = insertMember("13900001111", new BigDecimal("9.5"));

        QuoteRequest request = new QuoteRequest();
        request.items = List.of(item(product.id, 2));
        request.memberId = member.id;

        TestContext ctx = post("/api/promotions/quote", request);
        PromotionApiController.quote(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        QuoteView view = (QuoteView) body(ctx).get("data");
        assertAmountEquals("100.00", view.totalAmount);
        assertAmountEquals("9.5", view.memberDiscountRate);
        assertAmountEquals("5.00", view.memberDiscountAmount);
        assertAmountEquals("10.00", view.promotionDiscount);
        assertAmountEquals("85.00", view.finalAmount);
        assertEquals(1, view.itemCount);
        assertEquals("REDUCTION", view.appliedPromotion.type);
        assertNotNull(view.appliedPromotion.typeName);

        Promotion saved = DAO.findById(promotion.id);
        assertEquals(0, saved.usageCount, "试算不得占用促销次数");
    }

    @Test
    @DisplayName("试算不校验库存；商品/会员参数错误时返回 400")
    void quoteValidatesRequestWithoutStockCheck() throws Exception {
        Product scarce = insertProduct("10.00", 1);

        QuoteRequest beyondStock = new QuoteRequest();
        beyondStock.items = List.of(item(scarce.id, 2));
        TestContext stockCtx = post("/api/promotions/quote", beyondStock);
        PromotionApiController.quote(stockCtx.context);
        assertEquals(HttpStatus.OK, stockCtx.status, "试算只算钱，不因库存不足失败");

        assertQuoteBadRequest(null);
        assertQuoteBadRequest(new QuoteRequest());

        QuoteRequest duplicate = new QuoteRequest();
        duplicate.items = List.of(item(scarce.id, 1), item(scarce.id, 1));
        assertQuoteBadRequest(duplicate);

        QuoteRequest zeroQuantity = new QuoteRequest();
        zeroQuantity.items = List.of(item(scarce.id, 0));
        assertQuoteBadRequest(zeroQuantity);

        QuoteRequest missingProduct = new QuoteRequest();
        missingProduct.items = List.of(item(99999999, 1));
        assertQuoteBadRequest(missingProduct);

        QuoteRequest missingMember = new QuoteRequest();
        missingMember.items = List.of(item(scarce.id, 1));
        missingMember.memberId = 99999999;
        assertQuoteBadRequest(missingMember);
    }

    private static void assertQuoteBadRequest(QuoteRequest request) {
        TestContext ctx = post("/api/promotions/quote", request);
        PromotionApiController.quote(ctx.context);
        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    private static void assertAmountEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private static HoldItemRequest item(int productId, int quantity) {
        HoldItemRequest item = new HoldItemRequest();
        item.productId = productId;
        item.quantity = quantity;
        return item;
    }

    private static Product insertProduct(String price, int quantity) throws Exception {
        int seq = SEQ.incrementAndGet();
        Product product = new Product(0, "QUOTE-API-" + seq, "试算接口商品" + seq,
            Double.parseDouble(price), quantity, "测试分类", "QUOTE-API-BAR-" + seq, "件",
            "描述", "品牌", "供应商", "规格", 0, 5.0);
        assertTrue(DAOFactory.getInstance().getProductDAO().insert(product));
        assertNotNull(product.id);
        return product;
    }

    private static Member insertMember(String phone, BigDecimal discount) throws Exception {
        Member member = new Member();
        member.phone = phone;
        member.name = "试算会员";
        member.balance = BigDecimal.ZERO;
        member.points = BigDecimal.ZERO;
        member.level = "银卡";
        member.discount = discount;
        member.discountRate = discount;
        assertTrue(DAOFactory.getInstance().getMemberDAO().insert(member));
        return DAOFactory.getInstance().getMemberDAO().findByPhone(phone);
    }

    private static PromotionRequest validRequest(String name, String type) {
        PromotionRequest request = new PromotionRequest();
        request.name = name;
        request.type = type;
        request.startDate = "2026-01-02";
        request.endDate = "2026-02-03";
        request.enabled = true;
        return request;
    }

    private static TestContext post(String path, Object requestBody) {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, path);
        if (requestBody != null) {
            ctx.withBody(requestBody);
        }
        return ctx;
    }

    private static TestContext delete(int id) {
        return new TestContext().withRequest(HandlerType.DELETE, "/api/promotions/" + id)
            .withPathParam("id", String.valueOf(id));
    }

    private static void assertBadRequest(PromotionRequest request) {
        TestContext ctx = post("/api/promotions", request);
        PromotionApiController.create(ctx.context);
        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    private static Promotion insert(String code, String name, String type, BigDecimal threshold,
                                    BigDecimal discount, boolean enabled,
                                    LocalDateTime start, LocalDateTime end) throws Exception {
        Promotion promotion = new Promotion(0, code, name, type, threshold, discount, "",
            enabled, start, end, 0, -1);
        assertTrue(DAO.insert(promotion));
        return promotion;
    }
}
